package com.github.onsdigital.zebedee.authorisation;

import com.github.onsdigital.zebedee.content.util.ContentUtil;

/**
 * Created by dave on 07/03/2018.
 */
public class IdentityError implements JSONable {

    private String message;

    public IdentityError(String message) {
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
