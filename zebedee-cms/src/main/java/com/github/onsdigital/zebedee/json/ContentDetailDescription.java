package com.github.onsdigital.zebedee.json;

/**
 * ContentDetail sub class that holds the title of the page.
 */
public class ContentDetailDescription {
    public String title;
    public String edition;
    public String language;

    public ContentDetailDescription(String title) {
        this.title = title;
    }

    /**
     * Deep clone of object
     *
     * @return
     */
    public ContentDetailDescription clone() {
        ContentDetailDescription contentDetailDescription = new ContentDetailDescription(this.title);
        contentDetailDescription.edition = edition;
        contentDetailDescription.language = language;
        return contentDetailDescription;
    }
}
