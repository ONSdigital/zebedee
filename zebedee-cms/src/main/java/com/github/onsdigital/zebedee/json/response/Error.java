package com.github.onsdigital.zebedee.json.response;

import com.github.onsdigital.zebedee.content.util.ContentUtil;
import com.github.onsdigital.zebedee.json.JSONable;

/**
 * Created by dave on 07/03/2018.
 */
public class Error implements JSONable {

    private String message;

    public Error(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toJSON() {
        return ContentUtil.serialise(this);
    }
}
