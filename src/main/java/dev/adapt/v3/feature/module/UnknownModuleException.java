package dev.adapt.v3.feature.module;

public final class UnknownModuleException extends Exception {
    public UnknownModuleException(String text) {
        super("Unknown module: " + text);
    }
}
