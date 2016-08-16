package com.github.onsdigital.zebedee.util.mertics.model;

import com.github.davidcarboni.restolino.framework.HttpMethod;
import com.splunk.RequestMessage;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 *
 */
public class SplunkRequestMessage extends RequestMessage {

    public SplunkRequestMessage(HttpMethod method, SplunkEvent event) {
        super(method.name());
        setContent(new SplunkRequest(event).toJson());
    }

    @Override
    public Object getContent() {
        return super.getContent();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        SplunkRequestMessage that = (SplunkRequestMessage) obj;

        if (this.getContent() == null && that.getContent() != null) {
            return false;
        }

        if (this.getMethod() == null && that.getMethod() != null) {
            return false;
        }
        return this.getContent().equals(that.getContent()) && this.getMethod().equals(that.getMethod());
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.JSON_STYLE);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }
}
