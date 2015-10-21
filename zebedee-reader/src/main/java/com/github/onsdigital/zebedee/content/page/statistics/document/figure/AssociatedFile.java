package com.github.onsdigital.zebedee.content.page.statistics.document.figure;

/**
 * Reference of a file associated with a chart / table
 */
public class AssociatedFile {
    private String type;
    private String filename;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }
}
