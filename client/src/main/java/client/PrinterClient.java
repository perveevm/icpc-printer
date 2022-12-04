package client;

import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Font;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.*;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.net.URIBuilder;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.printing.PDFPageable;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.xhtmlrenderer.layout.SharedContext;
import org.xhtmlrenderer.pdf.ITextRenderer;

import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

public class PrinterClient {
    private final String url;
    private final String token;

    private static final PrintService service = PrintServiceLookup.lookupDefaultPrintService();

    private static final CloseableHttpClient client = HttpClients.createDefault();

    public PrinterClient(final String url, final String token) {
        this.url = url;
        this.token = token;
    }

    public void checkQueueAndPrint() {
        System.out.println("Checking queue...");

        String fileName = UUID.randomUUID() + ".pdf";
        String newFileName = UUID.randomUUID() + ".pdf";
        String html = downloadHtml();

        if (html == null || html.isEmpty()) {
            System.out.println("Nothing to print, skipping...");
            return;
        }

        Document document = Jsoup.parse(html);
        document.outputSettings().syntax(Document.OutputSettings.Syntax.xml);

        try (OutputStream stream = new FileOutputStream(fileName)) {
            ITextRenderer renderer = new ITextRenderer();
            SharedContext context = renderer.getSharedContext();
            context.setPrint(true);
            context.setInteractive(false);
            renderer.getFontResolver().addFont("ARIALUNI.TTF", BaseFont.IDENTITY_H, BaseFont.NOT_EMBEDDED);
            renderer.setDocumentFromString(document.html());
            renderer.layout();
            renderer.createPDF(stream);
        } catch (IOException e) {
            System.out.println("Cannot save file, printing HTML to console:");
            System.out.println(html);
            return;
        }

        try {
            manipulatePdf(fileName, newFileName);
            Files.delete(Path.of(fileName));
        } catch (IOException | DocumentException e) {
            System.out.println("Error happened while manipulating pdf " + fileName + ": " + e.getMessage());
            return;
        }

        try {
            printPdf(newFileName);
        } catch (IOException | PrinterException e) {
            System.out.println("Error happened while printing pdf " + newFileName + ": " + e.getMessage());
        }
    }

    private void printPdf(final String fileName) throws IOException, PrinterException {
        PDDocument document = PDDocument.load(Path.of(fileName).toFile());
        PrinterJob job = PrinterJob.getPrinterJob();
        job.setPageable(new PDFPageable(document));
        job.setPrintService(service);
        job.print();
    }

    private void manipulatePdf(final String fileName, final String newFileName) throws IOException, DocumentException {
        PdfReader reader = new PdfReader(fileName);
        int n = reader.getNumberOfPages();

        PdfStamper stamper = new PdfStamper(reader, new FileOutputStream(newFileName));
        int i = 0;

        while (i < n) {
            ++i;
            PdfContentByte pageContent = stamper.getOverContent(i);
            BaseFont bf = BaseFont.createFont("ARIALUNI.TTF", "Identity-H", true);
            Font font = new Font(bf, 10.0F);
            ColumnText.showTextAligned(pageContent, 2,
                    new Phrase(String.format("Страница %s из %s", i, n), font), 559.0F, 760.0F, 0.0F);
        }

        stamper.close();
        reader.close();
    }

    private String downloadHtml() {
        HttpGet request = new HttpGet(url + "/api/client");
        try {
            URI uri = new URIBuilder(request.getUri()).addParameter("token", token).build();
            request.setUri(uri);
        } catch (URISyntaxException e) {
            System.out.println("Error: URL is incorrect");
            return null;
        }

        try {
            return client.execute(request, response -> {
                if (response.getCode() != 200) {
                    System.out.println("Response code is not 200, skipping...");
                    return null;
                }

                HttpEntity entity = response.getEntity();
                if (entity == null) {
                    return null;
                }

                return EntityUtils.toString(entity);
            });
        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
            return null;
        }
    }
}
