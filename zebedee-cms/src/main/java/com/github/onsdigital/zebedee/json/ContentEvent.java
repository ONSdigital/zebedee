package com.github.onsdigital.zebedee.json;

import java.util.Date;

public class ContentEvent {
    public Date date;
    public ContentEventType type;
    public String email;

    public ContentEvent(Date date, ContentEventType type, String email) {
        this.date = date;
        this.type = type;
        this.email = email;
    }
}
