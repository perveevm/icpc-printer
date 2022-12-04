package ru.perveevm.printer.server.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.perveevm.printer.server.exceptions.PrinterException;
import ru.perveevm.printer.server.model.PrintRequest;
import ru.perveevm.printer.server.services.PrintService;

@RestController
@RequestMapping("/api")
public class PrintController {
    private final PrintService printService;

    public PrintController(final PrintService printService) {
        this.printService = printService;
    }

    @PostMapping("print")
    public ResponseEntity<?> print(@RequestBody final PrintRequest request) {
        try {
            printService.print(request.login(), request.password(), request.source());
            return ResponseEntity.ok().build();
        } catch (PrinterException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
