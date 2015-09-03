package com.github.onsdigital.zebedee.content.page.statistics.dataset;

import com.github.onsdigital.zebedee.content.page.base.PageType;

/**
 * Created by bren on 03/09/15.
 */
public class TimeSeriesDataset extends Dataset {

    @Override
    public PageType getType() {
        return PageType.timeseries_dataset;
    }
}
