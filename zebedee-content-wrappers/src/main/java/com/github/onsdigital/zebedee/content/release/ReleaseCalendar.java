package com.github.onsdigital.zebedee.content.release;

import com.github.onsdigital.zebedee.content.base.Content;
import com.github.onsdigital.zebedee.content.base.ContentDescription;
import com.github.onsdigital.zebedee.content.base.ContentType;
import com.github.onsdigital.zebedee.content.util.ContentConstants;

/**
 * Created by bren on 04/06/15.
 */
public class ReleaseCalendar extends Content {

    public ReleaseCalendar() {
        ContentDescription description = new ContentDescription();
        description.setTitle(ContentConstants.RELEASE_CALENDAR_TITLE);
    }

    @Override
    public ContentType getType() {
        return ContentType.release_calendar;
    }
}
