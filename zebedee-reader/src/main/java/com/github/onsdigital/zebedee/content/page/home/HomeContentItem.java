package com.github.onsdigital.zebedee.content.page.home;

import com.github.onsdigital.zebedee.content.partial.Image;

import java.net.URI;

public class HomeContentItem {

    private String title;
    private URI uri;
    private String description;
    private Image image;

    public HomeContentItem() {
    }

    public HomeContentItem(String Title, URI uri, String description) {
        setTitle(title);
        setUri(uri);
        setDescription(description);
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

    public Image getImage() {
        return image;
    }

    public void setImage(Image image) {
        this.image = image;
    }
    
}