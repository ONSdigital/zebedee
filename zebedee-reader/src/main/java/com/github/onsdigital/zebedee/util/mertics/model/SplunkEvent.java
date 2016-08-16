package com.github.onsdigital.zebedee.util.mertics.model;

public abstract class SplunkEvent {

    public enum StatsType {
        REQUEST_TIME,
        REQUEST_ERROR,
        PING_TIME
    }

    protected StatsType statsType;

    public StatsType getStatsType() {
        return statsType;
    }

    public void setStatsType(StatsType statsType) {
        this.statsType = statsType;
    }
}
