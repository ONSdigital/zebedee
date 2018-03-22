package com.github.onsdigital.zebedee.content.page.statistics.document.figure;

import com.github.onsdigital.zebedee.content.base.Content;

import java.net.URI;

/**
 * Common properties of figures (charts and tables)
 */
public class FigureSection extends Content {
    private String title;
    private String filename;
    private String version;
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

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
}
