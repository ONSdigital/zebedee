package com.github.onsdigital.zebedee.content.page.statistics.document.figure;

import com.github.onsdigital.zebedee.content.page.base.Page;

import java.util.List;

/**
 * Base class for figures (charts / tables etc)
 */
public abstract class FigureBase extends Page {
    private List<AssociatedFile> files;
    private String title;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<AssociatedFile> getFiles() {
        return files;
    }

    public void setFiles(List<AssociatedFile> files) {
        this.files = files;
    }
}
