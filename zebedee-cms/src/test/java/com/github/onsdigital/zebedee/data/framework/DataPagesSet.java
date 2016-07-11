package com.github.onsdigital.zebedee.data.framework;

import com.github.onsdigital.zebedee.content.page.statistics.data.timeseries.TimeSeries;
import com.github.onsdigital.zebedee.content.page.statistics.dataset.DatasetLandingPage;
import com.github.onsdigital.zebedee.content.page.statistics.dataset.TimeSeriesDataset;
import com.github.onsdigital.zebedee.data.json.TimeSerieses;
import com.github.onsdigital.zebedee.data.processing.DataPublicationDetails;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.reader.ContentReader;

import java.io.IOException;
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

    public DataPublicationDetails getDetails(ContentReader publishedReader, ContentReader reviewed) throws ZebedeeException, IOException {
        return new DataPublicationDetails(publishedReader, reviewed, timeSeriesDataset.getUri().toString());
    }

    public TimeSerieses getTimeSerieses() {
        TimeSerieses timeSerieses = new TimeSerieses();
        for (TimeSeries timeSeries: timeSeriesList)
            timeSerieses.add(timeSeries);
        return timeSerieses;
    }
}
