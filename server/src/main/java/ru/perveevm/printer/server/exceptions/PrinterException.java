package ru.perveevm.printer.server.exceptions;

public class PrinterException extends RuntimeException {
    public PrinterException(final String message) {
        super("Error happened while working with printer: " + message);
    }

    public PrinterException(final String message, final Throwable cause) {
        super("Error happened while working with printer: " + message, cause);
    }
}
