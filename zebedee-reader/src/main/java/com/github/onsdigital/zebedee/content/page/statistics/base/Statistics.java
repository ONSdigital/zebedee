package com.github.onsdigital.zebedee.content.page.statistics.base;

import com.github.onsdigital.zebedee.content.page.base.Page;
import com.github.onsdigital.zebedee.content.page.statistics.dataset.Version;
import com.github.onsdigital.zebedee.content.partial.Alert;
import com.github.onsdigital.zebedee.content.partial.Link;

import java.util.List;

/**
 * Created by bren on 04/06/15.
 * <p>
 * <p>
 * Represents statistics pages that gets released periodically. Bulletin, Article, Timeseries, Dataset, etc.
 */
public abstract class Statistics extends Page {

    private List<Alert> alerts;
    private List<Link> relatedMethodology;
    private List<Link> relatedMethodologyArticle;
    private List<Version> versions;

    public List<Link> getRelatedMethodology() {
        return relatedMethodology;
    }

    public void setRelatedMethodology(List<Link> relatedMethodology) {
        this.relatedMethodology = relatedMethodology;
    }

    public List<Alert> getAlerts() {
        return alerts;
    }

    public void setAlerts(List<Alert> alerts) {
        this.alerts = alerts;
    }

    public List<Version> getVersions() {
        return versions;
    }

    public void setVersions(List<Version> versions) {
        this.versions = versions;
    }

    public List<Link> getRelatedMethodologyArticle() {
        return relatedMethodologyArticle;
    }

    public void setRelatedMethodologyArticle(List<Link> relatedMethodologyArticle) {
        this.relatedMethodologyArticle = relatedMethodologyArticle;
    }

}
