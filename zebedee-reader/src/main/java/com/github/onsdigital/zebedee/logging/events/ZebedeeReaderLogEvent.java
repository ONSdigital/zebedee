package com.github.onsdigital.zebedee.logging.events;

/**
 * Created by dave on 5/4/16.
 */
public enum ZebedeeReaderLogEvent {

    /**
     * Any Exception thrown in Zebedee.
     */
    EXCEPTION("Zebedee exception"),

    /**
     * For any general debug message.
     */
    DEBUG_MESSAGE("Info"),

    /**
     * For ElasticSearch related logging.
     */
    ELASTIC_SEARCH_DEBUG("ElasticSearch logging");

    private final String description;

    ZebedeeReaderLogEvent(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
