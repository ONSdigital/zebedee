package com.github.onsdigital.zebedee.search.fastText.exceptions;

public class FastTextServerError extends Exception {

    public FastTextServerError(String response, int code, String context) {
        super(String.format("Received error response from search service: [status=%d, response=%s, context=%s]",
                code, response, context));
    }

}
