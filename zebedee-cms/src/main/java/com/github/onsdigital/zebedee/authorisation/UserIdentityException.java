package com.github.onsdigital.zebedee.authorisation;

import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class UserIdentityException extends ZebedeeException {

    public UserIdentityException(String message, int responseCode) {
        super(message, responseCode);
    }

    public int getResponseCode() {
        return super.statusCode;
    }


    @Override
    public boolean equals(Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }
}
