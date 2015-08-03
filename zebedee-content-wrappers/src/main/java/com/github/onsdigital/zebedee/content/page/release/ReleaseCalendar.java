package com.github.onsdigital.zebedee.content.page.release;

import com.github.onsdigital.zebedee.content.page.base.Page;
import com.github.onsdigital.zebedee.content.page.base.PageDescription;
import com.github.onsdigital.zebedee.content.page.base.PageType;
import com.github.onsdigital.zebedee.content.util.ContentConstants;

/**
 * Created by bren on 04/06/15.
 */
public class ReleaseCalendar extends Page {

    public ReleaseCalendar() {
        PageDescription description = new PageDescription();
        description.setTitle(ContentConstants.RELEASE_CALENDAR_TITLE);
    }

    @Override
    public PageType getType() {
        return PageType.release_calendar;
    }
}
