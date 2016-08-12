package com.github.onsdigital.zebedee.util.mertics.model;

public abstract class SplunkEvent {

    public enum StatsType {
        REQUEST_TIME,
        REQUEST_ERROR,
        PING_TIME
    }

    protected StatsType statsType;

    protected static final ThreadLocal<RequestMetrics> requestTimeEventsThreadLocal = new ThreadLocal<>();

    public static void storeRequestMetrics(RequestMetrics event) {
        requestTimeEventsThreadLocal.set(event);
    }

    public static RequestMetrics getRequestMetrics() {
        return requestTimeEventsThreadLocal.get();
    }

    public StatsType getStatsType() {
        return statsType;
    }

    public void setStatsType(StatsType statsType) {
        this.statsType = statsType;
    }
}
