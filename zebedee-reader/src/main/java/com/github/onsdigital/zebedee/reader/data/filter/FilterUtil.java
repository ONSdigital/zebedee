package com.github.onsdigital.zebedee.reader.data.filter;

import com.github.onsdigital.zebedee.content.base.Content;
import com.github.onsdigital.zebedee.content.dynamic.ContentNodeDetails;
import com.github.onsdigital.zebedee.content.dynamic.DescriptionWrapper;
import com.github.onsdigital.zebedee.content.dynamic.timeseries.Point;
import com.github.onsdigital.zebedee.content.dynamic.timeseries.Series;
import com.github.onsdigital.zebedee.content.page.base.Page;
import com.github.onsdigital.zebedee.content.page.statistics.data.timeseries.TimeSeries;
import com.github.onsdigital.zebedee.content.page.statistics.data.timeseries.TimeSeriesValue;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import org.apache.commons.lang3.StringUtils;

import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static com.github.onsdigital.zebedee.logging.ZebedeeReaderLogBuilder.logDebug;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

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

        switch (filter.getType()) {
            case TITLE:
                ContentNodeDetails titleWrapper = new ContentNodeDetails();
                titleWrapper.setTitle(page.getDescription().getTitle());
                titleWrapper.setEdition(page.getDescription().getEdition());
                titleWrapper.setUri(page.getUri());
                return titleWrapper;
            case DESCRIPTION:
                return new DescriptionWrapper(page.getUri(), page.getDescription());
            case SERIES:
                return filterTimseriesData(page, filter.getParameters());
            default:
                throw new IllegalArgumentException("Filter not available");
        }
    }


    private static Content filterTimseriesData(Page page, Map<String, String[]> parameters) throws BadRequestException, NotFoundException {
        if (page instanceof TimeSeries == false) {
            throw new BadRequestException("Requested content is not a time series, can not apply series filter");
        }

        SeriesFilterRequest filterRequest = new SeriesFilterRequest(parameters);

        TimeSeries timeSeries = (TimeSeries) page;
        String frequency = getValue(parameters, "frequency");
        frequency = frequency == null ? "" : StringUtils.lowerCase(frequency);

        Set<TimeSeriesValue> set = null;

        switch (frequency) {
            case "years":
                set = timeSeries.years;
                break;
            case "months":
                set = timeSeries.months;
                break;
            case "quarters":
                set = timeSeries.quarters;
                break;
            default:
                if (timeSeries.months.size() > 0) {
                    set = timeSeries.months;
                } else if (timeSeries.quarters.size() > 0) {
                    set = timeSeries.quarters;
                } else if (timeSeries.years.size() > 0) {
                    set = timeSeries.years;
                }
                break;
        }

        if (set == null) {
            throw new NotFoundException("Time series does not contain any series data");
        }

        set = applyRange(set, toDate(filterRequest.from), toDate(filterRequest.to));

        Series series = new Series();
        series.setUri(page.getUri());
        series.setDescription(page.getDescription());
        for (TimeSeriesValue timeSeriesValue : set) {
            series.add(new Point(isNotEmpty(timeSeriesValue.label) ? timeSeriesValue.label : timeSeriesValue.date, timeSeriesValue.value));
        }
        return series;
    }

    //applies filter, migrated code from the Alpha
    private static Set<TimeSeriesValue> applyRange(Set<TimeSeriesValue> set, Date from, Date to) {
        if (from == null && to == null) {
            return set;
        }

        Set<TimeSeriesValue> result = new TreeSet<>();

        boolean add = false;
        for (TimeSeriesValue timeSeriesValue : set) {
            Date date = timeSeriesValue.toDate();
            // Start adding if no from date has been specified:
            if ((!add && from == null) || date.equals(from) || date.after(from)) {
                logDebug("applying date range").addParameter("from", date.toString()).log();
                add = true;
            }
            if (add) {
                result.add(timeSeriesValue);
            }
            if (date.equals(to)) {
                logDebug("applying date range").addParameter("to", date.toString()).log();
                break;
            }
        }

        return result;
    }


    private static String getValue(Map<String, String[]> parameters, String paramName) {
        if (parameters == null) {
            return null;
        }
        String[] paramValues = parameters.get(paramName);
        if (paramValues == null || paramValues.length == 0) {
            return null;
        }
        return paramValues[0];
    }

    private static Date toDate(DateVal from) {
        Date result = null;
        if (from != null) {
            result = TimeSeriesValue.toDate(from.toString());
        }
        return result;
    }


    private static class SeriesFilterRequest {

        private DateVal from;
        private DateVal to;

        public SeriesFilterRequest(Map<String, String[]> parameters) {

            /*From*/
            String fromYear = getValue(parameters, "fromYear");
            if (fromYear != null) {
                from = new DateVal();
                from.month = getValue(parameters, "fromMonth");
                from.quarter = getValue(parameters, "fromQuarter");
                from.year = Integer.parseInt(fromYear);
            }

            /*To*/
            String toYear = getValue(parameters, "toYear");
            if (toYear != null) {
                to = new DateVal();
                to.month = getValue(parameters, "toMonth");
                to.quarter = getValue(parameters, "toQuarter");
                to.year = Integer.parseInt(toYear);
            }
        }
    }


    private static class DateVal {
        private int year;
        private String quarter; //Q1,Q2,Q3 or Q4
        private String month; //Jan,Feb .....

        @Override
        public String toString() {
            String date = String.valueOf(year);
            if (StringUtils.isNotBlank(month)) {
                date += " " + month;
            }
            if (StringUtils.isNotBlank(quarter)) {
                date += " " + quarter;
            }
            return date;
        }
    }

}




