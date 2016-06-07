package com.github.onsdigital.zebedee.model.collection.audit;

import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.persistence.CollectionEventType;
import com.github.onsdigital.zebedee.util.PropertiesUtil;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.Date;
import java.util.Map;

/**
 * POJO representation of a collection history event used by {@link com.github.onsdigital.zebedee.api.CollectionHistory}
 * API endpoint.
 */
public class CollectionEventDetails {

    private String type;
    private String description;
    private Date date;
    private String user;
    private Map<String, String> metaData;

    public CollectionEventDetails(CollectionEventType eventType, Date eventDate, String user, Map<String, String> metaData)
            throws ZebedeeException {
        this.type = eventType.name();
        this.description = PropertiesUtil.getProperty(eventType.getDescriptionKey());
        this.date = eventDate;
        this.user = user;
        this.metaData = (metaData == null || metaData.isEmpty()) ? null : metaData;
    }

    public String getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }

    public Date getDate() {
        return date;
    }

    public String getUser() {
        return user;
    }

    public Map<String, String> getMetaData() {
        return metaData;
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
