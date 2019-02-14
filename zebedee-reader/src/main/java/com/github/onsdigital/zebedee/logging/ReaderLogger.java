package com.github.onsdigital.zebedee.logging;

import com.github.onsdigital.logging.v2.event.Severity;
import com.github.onsdigital.logging.v2.event.SimpleEvent;

public class ReaderLogger {

    static final String NAMESPACE = "zebedee-reader";

    private ReaderLogger() {
    }

    public static SimpleEvent warn() {
        return new SimpleEvent(NAMESPACE, Severity.WARN);
    }

    public static SimpleEvent info() {
        return new SimpleEvent(NAMESPACE, Severity.INFO);
    }

    public static SimpleEvent error() {
        return new SimpleEvent(NAMESPACE, Severity.ERROR);
    }
}
