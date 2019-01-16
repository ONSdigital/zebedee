package com.github.onsdigital.zebedee.model.approval;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ApprovalEvent {

    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");

    private String event;
    private String time;

    public ApprovalEvent(ApprovalEventType event, Date comletedAt) {
        this.event = event.getDescription();
        this.time = DATE_FORMAT.format(comletedAt);
    }

    public String getTime() {
        return time;
    }

    public String getEvent() {
        return event;
    }
}
