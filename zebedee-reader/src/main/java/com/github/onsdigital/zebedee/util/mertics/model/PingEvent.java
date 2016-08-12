package com.github.onsdigital.zebedee.util.mertics.model;

/**
 * Created by dave on 8/12/16.
 */
public class PingEvent extends SplunkEvent {

    private long ms;

    public PingEvent(long ms) {
        this.ms = ms;
        super.setStatsType(StatsType.PING_TIME);
    }

    public long getMs() {
        return ms;
    }
}
