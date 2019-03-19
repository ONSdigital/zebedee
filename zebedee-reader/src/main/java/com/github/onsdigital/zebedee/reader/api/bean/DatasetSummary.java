package com.github.onsdigital.zebedee.reader.api.bean;

import com.github.onsdigital.zebedee.content.page.statistics.dataset.DatasetLandingPage;
import dp.api.dataset.model.Dataset;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class DatasetSummary {

    private String title;
    private String summary;
    private String uri;

    public DatasetSummary() {
    }

    public DatasetSummary(DatasetLandingPage dlp) {
        this.title = dlp.getDescription().getTitle();
        this.summary = dlp.getDescription().getSummary();
        this.uri = dlp.getUri().toString();
    }

    public DatasetSummary(Dataset dataset) {
        this.title = dataset.getTitle();
        this.summary = dataset.getDescription();
        this.uri = dataset.getLinks().getSelf().getHref();
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        DatasetSummary summary1 = (DatasetSummary) o;

        return new EqualsBuilder()
                .append(getTitle(), summary1.getTitle())
                .append(getSummary(), summary1.getSummary())
                .append(getUri(), summary1.getUri())
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(getTitle())
                .append(getSummary())
                .append(getUri())
                .toHashCode();
    }
}
