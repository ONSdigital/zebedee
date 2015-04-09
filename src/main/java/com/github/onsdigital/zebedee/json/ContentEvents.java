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
}
