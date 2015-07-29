package com.github.onsdigital.zebedee.content.statistics.document.figure;

import java.net.URI;

/**
 * Common properties of figures (charts and tables)
 */
public class FigureSection {
    private String title;
    private String filename;
    private URI uri;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }
}
