package com.github.onsdigital.zebedee.json;

import java.util.Date;

public class Event {
    public Date date;
    public EventType type;
    public String email;

    public Event(Date date, EventType type, String email) {
        this.date = date;
        this.type = type;
        this.email = email;
    }
}
