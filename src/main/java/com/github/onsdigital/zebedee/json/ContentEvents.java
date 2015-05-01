package com.github.onsdigital.zebedee.json;

import java.util.ArrayList;

public class ContentEvents extends ArrayList<ContentEvent> {

    public boolean hasEventForType(ContentEventType type) {
        for (ContentEvent contentEvent : this) {
            if (contentEvent.type.equals(type)) {
                return true;
            }
        }
        return false;
    }

    /**
     * return the most recent event for the given {@link ContentEventType}
     *
     * @param type The event type to get the most recent event for.
     * @return The most recent event if there is one, else null.
     */
    public ContentEvent mostRecentEventForType(ContentEventType type) {
        ContentEvent mostRecentEvent = null;
        for (ContentEvent contentEvent : this) {
            if (contentEvent.type.equals(type) &&
                    (mostRecentEvent == null || mostRecentEvent.date.before(contentEvent.date))) {
                mostRecentEvent = contentEvent;
            }
        }

        return mostRecentEvent;
    }

    /**
     *
     */
    public boolean mostRecentEventWasOfType(ContentEvent type) {
        if(this.size() == 0) { return false; }
        if(this.get(this.size() - 1).toString().equals(type)) { return true;}
        return false;
    }
}
