package com.github.onsdigital.zebedee.content.page.release;

import java.util.Date;

/**
 * Created by bren on 11/08/15.
 */
public class ReleaseDateChange {
    private Date previousDate;
    private String markdown;

    public Date getPreviousDate() {
        return previousDate;
    }

    public void setPreviousDate(Date previousDate) {
        this.previousDate = previousDate;
    }

    public String getMarkdown() {
        return markdown;
    }

    public void setMarkdown(String markdown) {
        this.markdown = markdown;
    }
}
