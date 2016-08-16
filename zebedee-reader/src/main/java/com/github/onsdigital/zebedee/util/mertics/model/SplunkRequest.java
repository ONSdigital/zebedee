package com.github.onsdigital.zebedee.util.mertics.model;

import com.google.gson.Gson;

/**
 * Created by dave on 8/11/16.
 */
public class SplunkRequest {

    private SplunkEvent event;

    public SplunkRequest(SplunkEvent event) {
        this.event = event;
    }

    public SplunkEvent getEvent() {
        return event;
    }

    public String toJson() {
        return new Gson().toJson(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SplunkRequest that = (SplunkRequest) o;

        return event != null ? event.equals(that.event) : that.event == null;
    }

    @Override
    public int hashCode() {
        return event != null ? event.hashCode() : 0;
    }
}
