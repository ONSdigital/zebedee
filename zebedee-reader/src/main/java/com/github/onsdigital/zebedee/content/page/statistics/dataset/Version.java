package com.github.onsdigital.zebedee.content.page.statistics.dataset;

import java.net.URI;
import java.util.Date;

/**
 * Created by bren on 19/10/15.
 */
public class Version {
    private URI uri;
    private Date updateDate;
    private String correctionNotice;
    private String label;

    public String getLabel() { return label; }

    public void setLabel(String label) { this.label = label; }

    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    public Date getUpdateDate() {
        return updateDate;
    }

    public void setUpdateDate(Date updateDate) {
        this.updateDate = updateDate;
    }

    public String getCorrectionNotice() {
        return correctionNotice;
    }

    public void setCorrectionNotice(String correctionNotice) {
        this.correctionNotice = correctionNotice;
    }
}
