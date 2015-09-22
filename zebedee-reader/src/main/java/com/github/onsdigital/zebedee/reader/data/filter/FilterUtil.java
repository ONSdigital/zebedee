package com.github.onsdigital.zebedee.reader.data.filter;

import com.github.onsdigital.zebedee.content.base.Content;
import com.github.onsdigital.zebedee.content.dynamic.DescriptionWrapper;
import com.github.onsdigital.zebedee.content.dynamic.timeseries.Point;
import com.github.onsdigital.zebedee.content.page.base.Page;
import com.github.onsdigital.zebedee.content.page.base.PageDescription;
import com.github.onsdigital.zebedee.content.page.statistics.data.timeseries.TimeSeries;
import com.github.onsdigital.zebedee.content.page.statistics.data.timeseries.TimeSeriesValue;
import com.github.onsdigital.zebedee.content.dynamic.timeseries.Series;
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
     * <p>
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
                titleWrapper.setUri(page.getUri());
                return titleWrapper;
            case DESCRIPTION:
                return new DescriptionWrapper(page.getUri(), page.getDescription());
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

        Set<TimeSeriesValue> set = null;

        TimeSeries timeSeries = (TimeSeries) page;
        if (timeSeries.months.size() > 0) {
            set = timeSeries.months;
        } else if (timeSeries.quarters.size() > 0) {
            set = timeSeries.quarters;
        } else if (timeSeries.years.size() > 0) {
            set = timeSeries.years;
        }

        if (set == null) {
            throw new NotFoundException("Time series does not contain any series data");
        }

        Series series = new Series();
        series.setUri(page.getUri());
        series.setDescription(new PageDescription());// only setting title and cdid of description
        series.getDescription().setCdid(page.getDescription().getCdid());
        series.getDescription().setTitle(page.getDescription().getTitle());
        for (TimeSeriesValue timeSeriesValue : set) {
            series.add(new Point(timeSeriesValue.date, timeSeriesValue.value));
        }
        return series;
    }


}




