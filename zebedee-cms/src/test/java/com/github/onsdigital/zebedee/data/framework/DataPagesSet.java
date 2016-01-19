package com.github.onsdigital.zebedee.data.framework;

import com.github.onsdigital.zebedee.content.page.statistics.data.timeseries.TimeSeries;
import com.github.onsdigital.zebedee.content.page.statistics.dataset.DatasetLandingPage;
import com.github.onsdigital.zebedee.content.page.statistics.dataset.TimeSeriesDataset;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by thomasridd on 1/19/16.
 */
public class DataPagesSet {
    public DatasetLandingPage datasetLandingPage;
    public TimeSeriesDataset timeSeriesDataset;
    public List<TimeSeries> timeSeriesList = new ArrayList<>();
    public String fileUri;
}
