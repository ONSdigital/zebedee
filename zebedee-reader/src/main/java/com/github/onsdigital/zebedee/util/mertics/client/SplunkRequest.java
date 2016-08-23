package com.github.onsdigital.zebedee.util.mertics.client;

import com.splunk.RequestMessage;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * Created by dave on 8/17/16.
 */
public class SplunkRequest extends RequestMessage {

    public SplunkRequest(String method, String content) {
        super(method);
        super.setContent(content);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public boolean equals(Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj);
    }
}
