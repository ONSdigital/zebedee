package com.github.onsdigital.zebedee.content.dynamic.timeseries;

import com.github.onsdigital.zebedee.content.base.Content;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by bren on 03/08/15.
 */
public class Series extends Content {

    private Set<Point> series = new HashSet<>();

    public void add(Point point) {
        series.add(point);
    }

    public Set<Point> getSeries() {
        return series;
    }

    public void setSeries(Set<Point> series) {
        this.series = series;
    }
}
