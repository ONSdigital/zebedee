package com.github.onsdigital.zebedee.model;

import javax.ws.rs.core.Response;

/**
 * Simple POJO for json responses.
 */
public class SimpleResponse {

    private String message;
    private int statusCode;

    public SimpleResponse(String message, Response.Status statusCode) {
        this.message = message;
        this.statusCode = statusCode.getStatusCode();
    }

    public String getMessage() {
        return message;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
