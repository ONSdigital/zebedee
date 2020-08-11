package com.github.onsdigital.zebedee.content.page.home;

import java.net.URI;

public class HomeContentItem {

    private String title;
    private URI uri;
    private String description;
    private String image;

    public HomeContentItem() {
    }

    public HomeContentItem(String title, URI uri, String description, String image) {
        setTitle(title);
        setUri(uri);
        setDescription(description);
        setImage(image);
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }
}