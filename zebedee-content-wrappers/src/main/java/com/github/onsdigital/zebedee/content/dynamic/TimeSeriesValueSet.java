package com.github.onsdigital.zebedee.content.dynamic;

import com.github.onsdigital.zebedee.content.base.Content;
import com.github.onsdigital.zebedee.content.page.statistics.data.timeseries.TimeSeriesValue;

import java.util.Set;

/**
 * Created by bren on 03/08/15.
 */
public class TimeSeriesValueSet extends Content {

    private Set<TimeSeriesValue> series;

    public Set<TimeSeriesValue> getSeries() {
        return series;
    }

    public void setSeries(Set<TimeSeriesValue> series) {
        this.series = series;
    }
}
