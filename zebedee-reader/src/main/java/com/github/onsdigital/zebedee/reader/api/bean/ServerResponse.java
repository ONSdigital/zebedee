package com.github.onsdigital.zebedee.reader.api.bean;

import java.util.Map;

/**
 * Created by bren on 31/07/15.
 */
public class ServerResponse {

    private String message;
    private Map<String, String> data;

    public ServerResponse(String message) {
        this.message = message;
    }

    public ServerResponse(String message, Map<String, String> data) {
        this.message = message;
        this.data = data;
    }
}
