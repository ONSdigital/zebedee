package com.github.onsdigital.zebedee.model.collection.audit;

import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.util.ArrayList;
import java.util.List;

/**
 * Response object of the {@link com.github.onsdigital.zebedee.api.CollectionHistory} API endpoint. A {@link List} of
 * {@link CollectionHistoryEvent} items that represent the event history of a specified collection. The items are ordered
 * from oldest to latest.
 */
public class CollectionHistory extends ArrayList<CollectionHistoryEvent> {

    /**
     * @param events
     * @throws ZebedeeException
     */
    public CollectionHistory(List<com.github.onsdigital.zebedee.persistence.model.CollectionHistoryEvent> events)
            throws ZebedeeException {
        super();
        for (com.github.onsdigital.zebedee.persistence.model.CollectionHistoryEvent event : events) {
            this.add(new CollectionHistoryEvent(event));
        }
    }

    /**
     * No args constructor.
     */
    public CollectionHistory() {
        super();
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.JSON_STYLE);
    }
}
