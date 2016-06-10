package com.github.onsdigital.zebedee.model.collection.audit;

import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.persistence.model.CollectionHistoryEventMetaData;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by dave on 6/2/16.
 */
public class CollectionHistoryEvent {

    private String collectionId;
    private String collectionName;
    private CollectionEventDetails eventDetails;
    private String uri;
    private String exceptionText;

    public CollectionHistoryEvent(com.github.onsdigital.zebedee.persistence.model.CollectionHistoryEvent event) throws ZebedeeException {
        this.collectionId = event.getCollectionId();
        this.collectionName = event.getCollectionName();
        this.uri = event.getUri();
        this.exceptionText = event.getExceptionText();
        Map<String, String> metaData = null;

        if (event.getCollectionHistoryEventMetaData() != null && !event.getCollectionHistoryEventMetaData().isEmpty()) {
            metaData = event.getCollectionHistoryEventMetaData()
                    .stream()
                    .collect(Collectors.toMap(
                            CollectionHistoryEventMetaData::getKey, CollectionHistoryEventMetaData::getValue)
                    );
        }
        this.eventDetails = new CollectionEventDetails(event.getEventType(), event.getEventDate(), event.getUser(), metaData);
    }

    public String getCollectionId() {
        return collectionId;
    }

    public CollectionEventDetails getEventDetails() {
        return eventDetails;
    }

    public String getCollectionName() {
        return collectionName;
    }

    public String getUri() {
        return uri;
    }

    public String getExceptionText() {
        return exceptionText;
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
