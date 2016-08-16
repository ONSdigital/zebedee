package com.github.onsdigital.zebedee.util.mertics.model;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

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

    @Override
    public boolean equals(Object o) {
        return EqualsBuilder.reflectionEquals(this, o);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }
}
