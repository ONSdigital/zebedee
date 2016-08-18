package com.github.onsdigital.zebedee.util.mertics.events;

/**
 * Created by dave on 8/17/16.
 */
public enum MetricsType {

    /**
     * Metrics for the request/response time.
     */
    REQUEST_TIME,

    /**
     * Metrics about errors.
     */
    REQUEST_ERROR,

    /**
     * Metrics for ping times.
     */
    PING_TIME,

    /**
     * Metrics for collections publish tines.
     */
    COLLECTIONS_PUBLISH_TIME
}
