package com.github.onsdigital.zebedee.content.dynamic;

import com.github.onsdigital.zebedee.content.base.Content;

/**
 * Created by bren on 03/08/15.
 *
 * Simple wrapper containing only title. Used for data filtering
 */
public class TitleWrapper extends Content {
    private String title;
    private String edition; //Edition is only available for bulletins and articles

    public TitleWrapper() {

    }
    public TitleWrapper(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getEdition() {
        return edition;
    }

    public void setEdition(String edition) {
        this.edition = edition;
    }
}
