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

    public String getTitle() {
        return title;
    }

    public ContentDetailDescription setTitle(String title) {
        this.title = title;
        return this;
    }

    public String getEdition() {
        return edition;
    }

    public ContentDetailDescription setEdition(String edition) {
        this.edition = edition;
        return this;
    }

    public String getLanguage() {
        return language;
    }

    public ContentDetailDescription setLanguage(String language) {
        this.language = language;
        return this;
    }
}
