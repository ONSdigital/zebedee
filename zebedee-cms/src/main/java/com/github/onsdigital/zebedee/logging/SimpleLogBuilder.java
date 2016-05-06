package com.github.onsdigital.zebedee.logging;

import ch.qos.logback.classic.Level;
import com.github.onsdigital.logging.builder.LogMessageBuilder;

/**
 * Created by dave on 5/6/16.
 */
public class SimpleLogBuilder extends LogMessageBuilder {


    public static void logMessage(String eventDescription) {
        new SimpleLogBuilder(eventDescription, Level.DEBUG).log();
    }

    public static void logMessage(String eventDescription, Level level) {
        new SimpleLogBuilder(eventDescription, level).log();
    }

    public static void logError(String eventDescription) {
        new SimpleLogBuilder(eventDescription, Level.ERROR).log();
    }

    public static void logError(Exception ex) {
        new SimpleLogBuilder(ex.getMessage(), Level.ERROR).log();
    }

    private SimpleLogBuilder(String eventDescription, Level level) {
        super(eventDescription, level);
    }

    @Override
    public String getLoggerName() {
        return "com.github.onsdigital.logging";
    }
}
