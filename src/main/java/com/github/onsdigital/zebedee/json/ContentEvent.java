package com.github.onsdigital.zebedee.json;

import java.util.Date;

public class ContentEvent {
    public Date Date;
    public ContentEventType Type;
    public String Email;

    public ContentEvent(Date date, ContentEventType type, String email) {
        Date = date;
        Type = type;
        Email = email;
    }
}
