package com.github.onsdigital.zebedee.model.collection.audit;

import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.persistence.model.CollectionHistoryEventMetaData;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by dave on 6/2/16.
 */
public class CollectionHistoryEvent {

    private String collectionId;
    private String collectionName;
    private CollectionEvent eventDetails;
    private String pageURI;
    private String fileURI;
    private String exceptionText;

    public CollectionHistoryEvent(com.github.onsdigital.zebedee.persistence.model.CollectionHistoryEvent event) throws ZebedeeException {
        this.collectionId = event.getCollectionId();
        this.collectionName = event.getCollectionName();
        this.pageURI = event.getPageURI();
        this.fileURI = event.getFileURI();
        this.exceptionText = event.getExceptionText();
        Map<String, String> metaData = null;

        if (event.getCollectionHistoryEventMetaData() != null && !event.getCollectionHistoryEventMetaData().isEmpty()) {
            metaData = event.getCollectionHistoryEventMetaData()
                    .stream()
                    .collect(Collectors.toMap(
                            CollectionHistoryEventMetaData::getKey, CollectionHistoryEventMetaData::getValue)
                    );
        }
        this.eventDetails = new CollectionEvent(event.getEventType(), event.getEventDate(), event.getUser(), metaData);
    }

    public String getCollectionId() {
        return collectionId;
    }

    public CollectionEvent getEventDetails() {
        return eventDetails;
    }

    public String getCollectionName() {
        return collectionName;
    }

    public String getPageURI() {
        return pageURI;
    }

    public String getFileURI() {
        return fileURI;
    }

    public String getExceptionText() {
        return exceptionText;
    }
}
