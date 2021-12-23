package com.github.onsdigital.zebedee.content.page.home;

import java.net.URI;

public class EmergencyBanner {

    private EmergencyBannerType type;
    private String title;
    private String description;
    private String linkText;
    private URI uri;

    public EmergencyBanner(EmergencyBannerType type, String title, String description, String linkText, URI uri) {
        this.type = type;
        this.title = title;
        this.description = description;
        this.linkText = linkText;
        this.uri = uri;
    }

    public EmergencyBannerType getType() {
        return type;
    }

    public void setType(EmergencyBannerType type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLinkText() {
        return linkText;
    }

    public void setLinkText(String linkText) {
        this.linkText = linkText;
    }

    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }
}
