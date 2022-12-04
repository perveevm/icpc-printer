package ru.perveevm.printer.server.model;

public record PrintRequest(String login, String password, String source) {
}
