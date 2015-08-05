package com.github.onsdigital.zebedee.json;

import java.util.ArrayList;

public class Events extends ArrayList<Event> {

    public boolean hasEventForType(EventType type) {
        for (Event event : this) {
            if (event.type.equals(type)) {
                return true;
            }
        }
        return false;
    }

    /**
     * return the most recent event for the given {@link EventType}
     *
     * @param type The event type to get the most recent event for.
     * @return The most recent event if there is one, else null.
     */
    public Event mostRecentEventForType(EventType type) {
        Event mostRecentEvent = null;
        for (Event event : this) {
            if (event.type.equals(type) &&
                    (mostRecentEvent == null || mostRecentEvent.date.before(event.date))) {
                mostRecentEvent = event;
            }
        }

        return mostRecentEvent;
    }

    /**
     * return the most recent event
     *
     * @return The most recent event if there is one, else null.
     */
    public Event mostRecentEvent() {
        Event mostRecentEvent = null;
        for (Event event : this) {
            if (mostRecentEvent == null || mostRecentEvent.date.before(event.date)) {
                mostRecentEvent = event;
            }
        }

        return mostRecentEvent;
    }

    /**
     *
     */
    public boolean mostRecentEventWasOfType(Event type) {
        if (this.size() == 0) {
            return false;
        }
        if (this.get(this.size() - 1).toString().equals(type)) {
            return true;
        }
        return false;
    }
}
