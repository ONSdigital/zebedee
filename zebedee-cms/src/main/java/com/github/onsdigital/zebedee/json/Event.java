package com.github.onsdigital.zebedee.json;

import java.util.Date;

/**
 * Generic event class to store events triggered by a user and classified by the event type.
 */
public class Event {
    public Date date;
    public EventType type;
    public String email;
    public String note;

    /**
     * Constructor without specifying a note.
     *
     * @param date
     * @param type
     * @param email
     */
    public Event(Date date, EventType type, String email) {
        this.date = date;
        this.type = type;
        this.email = email;
    }

    /**
     * Constructor taking all parameters.
     *
     * @param date
     * @param type
     * @param email
     * @param note
     */
    public Event(Date date, EventType type, String email, String note) {
        this.date = date;
        this.type = type;
        this.email = email;
        this.note = note;
    }
}
