package ru.perveevm.printer.server.services;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.csv.QuoteMode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.perveevm.printer.server.exceptions.PrinterException;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class PrintService {
    private final Path printerDirectory;
    private final Path usersFile;
    private final Path tokensFile;

    private static final String[] USER_HEADERS = {"login", "password", "name"};
    private static final String[] PRINTER_HEADERS = {"token", "name"};

    public PrintService(@Value("${printing-server.directory}") final String printerDirectory,
                        @Value("${printing-server.users}") final String usersFile,
                        @Value("${printing-server.tokens}") final String tokensFile) {
        this.printerDirectory = Path.of(printerDirectory);
        this.usersFile = Path.of(usersFile);
        this.tokensFile = Path.of(tokensFile);

        Path printedDirectory = Path.of(printerDirectory, "printed");
        if (!Files.exists(printedDirectory)) {
            try {
                Files.createDirectory(printedDirectory);
            } catch (IOException e) {
                throw new PrinterException("Cannot create printed directory");
            }
        }
    }

    public void print(final String login, final String password, final String source) {
        Iterable<CSVRecord> records = readCSV(usersFile, USER_HEADERS);

        for (CSVRecord record : records) {
            String recordLogin = record.get(USER_HEADERS[0]);
            String recordPassword = record.get(USER_HEADERS[1]);
            String recordName = record.get(USER_HEADERS[2]);

            if (!recordLogin.equals(login)) {
                continue;
            }
            if (!recordPassword.equals(password)) {
                throw new PrinterException("invalid password");
            }

            Path sourcePath = Path.of(printerDirectory.toString(), login + "-" + UUID.randomUUID());
            try (BufferedWriter writer = Files.newBufferedWriter(sourcePath)) {
                writer.write(String.format("Логин: %s%n", login));
                writer.write(String.format("Участник: %s%n%n", recordName));
                writer.write(source);
            } catch (IOException e) {
                throw new PrinterException("cannot write source file", e);
            }
            return;
        }

        throw new PrinterException("user not found");
    }

    public String getSourceForPrint(final String token) {
        Iterable<CSVRecord> records = readCSV(tokensFile, PRINTER_HEADERS);

        for (CSVRecord record : records) {
            String recordToken = record.get(PRINTER_HEADERS[0]);
            String recordName = record.get(PRINTER_HEADERS[1]);

            if (!recordToken.equals(token)) {
                continue;
            }

            Path sourcePath;
            try (Stream<Path> files = Files.list(printerDirectory)) {
                Optional<Path> sourcePathOptional = files.filter(p -> !p.getFileName().toString().equals("printed"))
                        .findAny();
                if (sourcePathOptional.isEmpty()) {
                    return null;
                }
                sourcePath = sourcePathOptional.get();
            } catch (IOException e) {
                throw new PrinterException("cannot list queue", e);
            }

            String source;
            try (BufferedReader reader = Files.newBufferedReader(sourcePath)) {
                source = reader.lines().collect(Collectors.joining(System.lineSeparator()));
            } catch (IOException e) {
                throw new PrinterException("cannot read source file", e);
            }

            try {
                Files.move(sourcePath,
                        Path.of(printerDirectory.toString(), "printed", sourcePath.getFileName().toString()));
            } catch (IOException e) {
                throw new PrinterException("cannot move file to printed directory");
            }

            return "Принтер: " + recordName + System.lineSeparator() + source;
        }

        throw new PrinterException("invalid token");
    }

    private Iterable<CSVRecord> readCSV(final Path path, final String[] headers) {
        Reader reader;
        try {
            reader = new FileReader(path.toFile());
        } catch (FileNotFoundException e) {
            throw new PrinterException("CSV file not found at path " + path, e);
        }

        try {
            CSVParser parser = new CSVParser(reader, CSVFormat.Builder.create()
                    .setDelimiter(';')
                    .setRecordSeparator(System.lineSeparator())
                    .setEscape('\\')
                    .setQuoteMode(QuoteMode.NONE)
                    .setHeader(headers).build());
            return parser.getRecords();
        } catch (IOException e) {
            throw new PrinterException("cannot read CSV file at path " + path, e);
        }
    }
}
