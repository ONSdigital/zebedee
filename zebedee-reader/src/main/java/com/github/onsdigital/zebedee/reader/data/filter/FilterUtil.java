package com.github.onsdigital.zebedee.reader.data.filter;

import com.github.onsdigital.zebedee.content.base.Content;
import com.github.onsdigital.zebedee.content.page.base.Page;
import com.github.onsdigital.zebedee.content.page.statistics.data.timeseries.TimeSeries;
import com.github.onsdigital.zebedee.content.page.statistics.data.timeseries.TimeSeriesValue;
import com.github.onsdigital.zebedee.content.dynamic.TimeSeriesValueSet;
import com.github.onsdigital.zebedee.content.dynamic.ContentNodeDetails;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;

import java.util.Set;

/**
 * Created by bren on 03/08/15.
 */
public class FilterUtil {


    /**
     * Filters page data with given ContentFilter
     *
     * If content filter is null , no filter operation will be done and page will be returned as it is
     *
     * @param content
     * @param filter
     * @return
     */
    //TODO: Might be a good idea seperating PageDescription objects into a proper object model if serialise/deserialise pitfalls are not a problem anymore. Check
    public static Content filterPageData(Content content, DataFilter filter) throws BadRequestException, NotFoundException {
        if (filter == null) {
            return content;
        }
        if (content instanceof Page == false) {
            throw new IllegalArgumentException("Filer can only be applied to full page contents");
        }

        Page page = (Page) content;

        switch (filter) {
            case TITLE:
                ContentNodeDetails titleWrapper = new ContentNodeDetails();
                titleWrapper.setTitle(page.getDescription().getTitle());
                titleWrapper.setEdition(page.getDescription().getEdition());
                return titleWrapper;
            case DESCRIPTION:
                return page.getDescription();
            case SERIES:
                return filterTimseriesData(page);
            default:
                throw new IllegalArgumentException("Filter not available");
        }
    }

    private static Content filterTimseriesData(Page page) throws BadRequestException, NotFoundException {
        if (page instanceof TimeSeries == false) {
            throw new BadRequestException("Requested content is not a time series, can not apply series filter");
        }

        Set<TimeSeriesValue> series = null;

        TimeSeries timeSeries = (TimeSeries) page;
        if (timeSeries.years.size() > 0) {
            series = timeSeries.years;
        } else if (timeSeries.quarters.size() > 0) {
            series = timeSeries.quarters;
        } else if (timeSeries.months.size() > 0) {
            series = timeSeries.months;
        }

        if (series == null) {
            throw new NotFoundException("Time series does not contain any series data");
        }

        TimeSeriesValueSet set = new TimeSeriesValueSet();
        set.setSeries(series);
        return set;
    }


}




