package com.github.onsdigital.zebedee.data.processing;

import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.content.dynamic.timeseries.Series;
import com.github.onsdigital.zebedee.content.page.base.Page;
import com.github.onsdigital.zebedee.content.page.base.PageDescription;
import com.github.onsdigital.zebedee.content.page.statistics.data.timeseries.TimeSeries;
import com.github.onsdigital.zebedee.content.page.statistics.data.timeseries.TimeSeriesValue;
import com.github.onsdigital.zebedee.content.page.statistics.dataset.DatasetLandingPage;
import com.github.onsdigital.zebedee.content.util.ContentUtil;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.model.CollectionContentReader;
import com.github.onsdigital.zebedee.model.CollectionContentWriter;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.util.Times;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.sql.Time;
import java.util.Date;
import java.util.Set;

/**
 * Created by thomasridd on 1/16/16.
 */
public class DataProcessor {
    public DataProcessor() {

    }

    /**
     * Take a timeseries as produced by Brian from an upload and combine it with current content
     *
     * @param publishedContentReader
     * @param reviewedContentReader
     * @param reviewedContentWriter
     * @param details
     * @param timeSeries
     * @return
     */
    public TimeSeries processTimeseries(CollectionContentReader publishedContentReader, CollectionContentReader reviewedContentReader, CollectionContentWriter reviewedContentWriter, DataPublicationDetails details, TimeSeries timeSeries) throws ZebedeeException, IOException {

        // Get the uri to publish this timeseries to
        String seriesUri = publishPathForTimeseries(timeSeries, details);

        // Start building our processed version
        TimeSeries processed = initialTimeseries(timeSeries, publishedContentReader, details);

        // T

        // Check if updates have been made to the data
        if(differencesExist(processed, seriesUri, publishedContentReader)) {

        }

        return null;
    }

    /**
     * Get the publish path for a timeseries
     *
     * (TODO: This is currently based on datasetUri and cdid only but will update when code goes unique)
     *
     * @param series
     * @param details
     * @return
     */
    String publishPathForTimeseries(TimeSeries series, DataPublicationDetails details) {
        return details.getTimeseriesFolder() + series.getCdid().toLowerCase() + "/data.json";
    }

    /**
     * Get the starting point for our timeseries by loading a
     *
     * @param series
     * @param publishedContentReader
     * @param details
     * @return
     * @throws ZebedeeException
     * @throws IOException
     */
    TimeSeries initialTimeseries(TimeSeries series, CollectionContentReader publishedContentReader, DataPublicationDetails details) throws ZebedeeException, IOException {

        // Try to get an existing timeseries
        TimeSeries existing = (TimeSeries) publishedContentReader.getContent(publishPathForTimeseries(series, details));

        // If it doesn't exist create a new empty one using the description
        if (existing == null) {
            TimeSeries initial = new TimeSeries();
            initial.setDescription(series.getDescription());
            return initial;
        }
        return existing;
    }

    /**
     * Takes a part-built TimeSeries page and populates the {@link com.github.onsdigital.zebedee.content.page.statistics.data.timeseries.TimeSeriesValue} list using data gathered from a csdb file
     *
     * @param page   a part-built time series page
     * @param series a time series returned by Brian by parsing a csdb file
     * @return
     */
    TimeSeries populatePageFromTimeSeries(TimeSeries page, TimeSeries series, DatasetLandingPage landingPage) {

        // Time series is a bit of an inelegant beast in that it splits data storage by time period
        // We deal with this by
        populatePageFromSetOfValues(page, page.years, series.years, landingPage);
        populatePageFromSetOfValues(page, page.quarters, series.quarters, landingPage);
        populatePageFromSetOfValues(page, page.months, series.months, landingPage);

        if (page.getDescription() == null || series.getDescription() == null) {
            System.out.println("Problem");
        }
        page.getDescription().setSeasonalAdjustment(series.getDescription().getSeasonalAdjustment());
        page.getDescription().setCdid(series.getDescription().getCdid());

        // Copy across the title if it is currently blank (equates to equalling Cdid)
        if (page.getDescription().getTitle() == null || page.getDescription().getTitle().equalsIgnoreCase("")) {
            page.getDescription().setTitle(series.getDescription().getTitle());
        } else if (page.getDescription().getTitle().equalsIgnoreCase(page.getCdid())) {
            page.getDescription().setTitle(series.getDescription().getTitle());
        }

        page.getDescription().setDate(series.getDescription().getDate());
        page.getDescription().setNumber(series.getDescription().getNumber());

        return page;
    }

    /**
     * Add individual points to a set of points (yearly, monthly, quarterly)
     *
     * @param page          the page
     * @param currentValues the current value list
     * @param updateValues  the new value list
     * @param landingPage   the landing page (used to get dataset id)
     */
    void populatePageFromSetOfValues(TimeSeries page, Set<TimeSeriesValue> currentValues, Set<TimeSeriesValue> updateValues, DatasetLandingPage landingPage) {

        // Iterate through values
        for (TimeSeriesValue value : updateValues) {
            // Find the current value of the data point
            TimeSeriesValue current = getCurrentValue(currentValues, value);

            if (current != null) { // A point already exists for this data

                if (!current.value.equalsIgnoreCase(value.value)) { // A point already exists for this data

                    // Update the point
                    current.value = value.value;
                    current.sourceDataset = landingPage.getDescription().getDatasetId();
                    current.updateDate = new Date();
                }
            } else {
                value.sourceDataset = landingPage.getDescription().getDatasetId();
                value.updateDate = new Date();

                page.add(value);
            }
        }
    }








    /**
     * Check if our new page is a new version
     *
     * @param series
     * @param seriesUri
     * @param publishedContentReader
     * @return
     * @throws ZebedeeException
     * @throws IOException
     */
    boolean differencesExist(TimeSeries series, String seriesUri, CollectionContentReader publishedContentReader) throws ZebedeeException, IOException {
        TimeSeries content = (TimeSeries) publishedContentReader.getContent(seriesUri);
        if (content == null)
            return false;

        return differencesExist(series, content);
    }

    /**
     * Check if differences exist between two pages
     *
     * @param page1
     * @param page2
     * @return
     */
    boolean differencesExist(TimeSeries page1, TimeSeries page2) {

        // Time series is a bit of an inelegant beast in that it splits data storage by time period
        // We deal with this by being inelegant ourselves
        if (differencesExist(page1.years, page2.years)) return true;
        if (differencesExist(page1.quarters, page2.quarters)) return true;
        if (differencesExist(page1.months, page2.months)) return true;

        return false;
    }

    /**
     * Check if differences exist between two sets of timeseries points
     *
     * @param currentValues
     * @param updateValues
     * @return
     */
    boolean differencesExist(Set<TimeSeriesValue> currentValues, Set<TimeSeriesValue> updateValues) {

        // Iterate through values
        for (TimeSeriesValue value : updateValues) {
            // Find the current value of the data point

            TimeSeriesValue current = getCurrentValue(currentValues, value);
            if (current == null || !current.value.equalsIgnoreCase(value.value)) {
                return true;
            }
        }

        return false;
    }

    /**
     * If a {@link TimeSeriesValue} for value.time exists in currentValues returns that.
     * Otherwise null
     *
     * @param currentValues a set of {@link TimeSeriesValue}
     * @param value         a {@link TimeSeriesValue}
     * @return a {@link TimeSeriesValue} from currentValues
     */
    static TimeSeriesValue getCurrentValue(Set<TimeSeriesValue> currentValues, TimeSeriesValue value) {
        if (currentValues == null) {
            return null;
        }

        for (TimeSeriesValue current : currentValues) {
            if (current.compareTo(value) == 0) {
                return current;
            }
        }
        return null;
    }

}
