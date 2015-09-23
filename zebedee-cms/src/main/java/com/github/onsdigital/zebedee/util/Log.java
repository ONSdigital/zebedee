package com.github.onsdigital.zebedee.util;

import org.apache.commons.lang.exception.ExceptionUtils;

import java.util.Date;

/**
 * Helper methods to make logging code more concise and consistent.
 */
public class Log {

    /**
     * Prints the given message to System.out prefixed with a timestamp.
     *
     * @param message
     */
    public static void print(String message) {
        System.out.println(timeStamp(message));
    }

    /**
     * Format and print the given message prefixed with a timestamp.
     *
     * @param message
     * @param args
     */
    public static void print(String message, Object... args) {
        print(String.format(message, args));
    }

    /**
     * Print an exception related message, and print the root cause stack trace.
     *
     * @param exception
     * @param message
     * @param args
     */
    public static void print(Exception exception, String message, Object... args) {
        print(String.format(message, args));
        ExceptionUtils.printRootCauseStackTrace(exception);
    }

    /**
     * Prefix the given string with a timestamp.
     *
     * @param message
     * @return
     */
    public static String timeStamp(String message) {
        return String.format("%s %s",
                DateConverter.toString(new Date()),
                message);
    }

    public static void main(String[] args) {
        print("Hi %s", "there");
    }
}
