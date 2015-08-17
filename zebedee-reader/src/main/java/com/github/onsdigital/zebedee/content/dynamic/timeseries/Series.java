package com.github.onsdigital.zebedee.content.dynamic.timeseries;

import com.github.onsdigital.zebedee.content.base.Content;
import com.github.onsdigital.zebedee.content.page.base.PageDescription;

import java.net.URI;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Created by bren on 03/08/15.
 */
public class Series extends Content {

    private URI uri;
    private PageDescription description;
    private Set<Point> series = new LinkedHashSet<>();

    public void add(Point point) {
        series.add(point);
    }

    public Set<Point> getSeries() {
        return series;
    }

    public void setSeries(Set<Point> series) {
        this.series = series;
    }

    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    public PageDescription getDescription() {
        return description;
    }

    public void setDescription(PageDescription description) {
        this.description = description;
    }
}
