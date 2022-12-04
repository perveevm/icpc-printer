package ru.perveevm.printer.server.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.perveevm.printer.server.exceptions.PrinterException;
import ru.perveevm.printer.server.services.PrintService;

@RestController
@RequestMapping("/api")
public class PrintController {
    private final PrintService printService;

    public PrintController(final PrintService printService) {
        this.printService = printService;
    }

    @PostMapping("print")
    public ResponseEntity<?> print(final MultiValueMap<String, String> parameters) {
        if (parameters == null) {
            return ResponseEntity.badRequest().build();
        }

        String login = parameters.getFirst("login");
        String password = parameters.getFirst("password");
        String source = parameters.getFirst("source");
        if (login == null || password == null || source == null) {
            return ResponseEntity.badRequest().build();
        }

        try {
            printService.print(login, password, source);
            return ResponseEntity.ok().build();
        } catch (PrinterException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
