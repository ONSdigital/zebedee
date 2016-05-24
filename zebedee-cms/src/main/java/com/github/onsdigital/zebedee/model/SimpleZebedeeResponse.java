package com.github.onsdigital.zebedee.model;

import javax.ws.rs.core.Response;

/**
 * Simple POJO for json responses.
 */
public class SimpleZebedeeResponse {

    private String message;
    private int statusCode;

    public SimpleZebedeeResponse(String message, Response.Status statusCode) {
        this.message = message;
        this.statusCode = statusCode.getStatusCode();
    }

    public String getMessage() {
        return message;
    }

    public int getStatusCode() {
        return statusCode;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        SimpleZebedeeResponse response = (SimpleZebedeeResponse) obj;

        if (statusCode != response.statusCode) {
            return false;
        }
        return message != null ? message.equals(response.message) : response.message == null;

    }

    @Override
    public int hashCode() {
        int result = message != null ? message.hashCode() : 0;
        result = 31 * result + statusCode;
        return result;
    }


}
