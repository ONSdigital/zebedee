package com.github.onsdigital.zebedee.content.dynamic;

import com.github.onsdigital.zebedee.content.base.Content;

import java.net.URI;
import java.util.Date;

/**
 * Created by bren on 03/08/15.
 *
 * Simple wrapper containing only title. Used for data filtering
 */
public class ContentNodeDetails extends Content {
    private URI uri;
    private String title;
    private String edition; //Edition is only available for bulletins and articles
    private Date releaseDate;

    public ContentNodeDetails() {

    }

    public ContentNodeDetails(String title, String edition) {
        this.title = title;
        this.edition = edition;
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

    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    public Date getReleaseDate() {
        return releaseDate;
    }

    public void setReleaseDate(Date releaseDate) {
        this.releaseDate = releaseDate;
    }
}
