package ru.perveevm.printer.server.controllers;

import org.apache.commons.text.StringEscapeUtils;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.perveevm.printer.server.exceptions.PrinterException;
import ru.perveevm.printer.server.services.PrintService;

@RestController
@RequestMapping("/api")
public class ClientController {
    private final PrintService printService;

    public ClientController(final PrintService printService) {
        this.printService = printService;
    }

    @GetMapping(value = "client", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<?> getSource(@RequestParam("token") final String token) {
        String source;
        try {
            source = printService.getSourceForPrint(token);
        } catch (PrinterException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }

        if (source == null) {
            return ResponseEntity.noContent().build();
        }

        StringBuilder html = new StringBuilder();
        html.append("""
                <!DOCTYPE html [
                  <!ENTITY % htmlentities SYSTEM "https://www.w3.org/2003/entities/2007/htmlmathml-f.ent">
                  %htmlentities;
                ]>
                <!-- your XHTML text following here -->
                """);
        html.append("<html>\n");
        html.append("""
                <head>
                    <meta http-equiv='Content-Type' content='text/html; charset=UTF-8'/>
                    <style type='text/css'>
                        * { font-family: 'Arial Unicode MS'; }
                    </style>
                </head>
                """);
//        html.append("<pre>");
        html.append(StringEscapeUtils.escapeHtml4(source)
                .replace(System.lineSeparator(), "<br/>")
                .replace("\t", "    ")
                .replace(" ", "&nbsp;"));
//        html.append("</pre>");
        html.append("</html>");
        return ResponseEntity.ok(html);
    }
}
