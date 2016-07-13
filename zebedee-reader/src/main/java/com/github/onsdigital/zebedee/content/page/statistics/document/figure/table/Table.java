package com.github.onsdigital.zebedee.content.page.statistics.document.figure.table;

import com.github.onsdigital.zebedee.content.page.base.PageType;
import com.github.onsdigital.zebedee.content.page.statistics.document.figure.FigureBase;

public class Table extends FigureBase {

    private String html;
    private String filename;
    private Boolean firstLineTitle;
    private String headerRows;
    private TableModifications modifications;

    @Override
    public PageType getType() {
        return PageType.table;
    }

    public String getHtml() {
        return html;
    }

    public void setHtml(String html) {
        this.html = html;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public Boolean getFirstLineTitle() {
        return firstLineTitle;
    }

    public void setFirstLineTitle(Boolean firstLineTitle) {
        this.firstLineTitle = firstLineTitle;
    }

    public String getHeaderRows() {
        return headerRows;
    }

    public void setHeaderRows(String headerRows) {
        this.headerRows = headerRows;
    }

    public TableModifications getModifications() {
        if (this.modifications == null) {
            this.modifications = new TableModifications();
        }
        return modifications;
    }

    public void setModifications(TableModifications modifications) {
        this.modifications = modifications;
    }
}