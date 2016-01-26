package com.github.onsdigital.zebedee.data.processing;

import com.github.onsdigital.zebedee.content.page.statistics.data.timeseries.TimeSeries;
import com.github.onsdigital.zebedee.content.page.statistics.data.timeseries.TimeSeriesValue;

import java.util.Calendar;

public class TimeSeriesLabeller {
    public static final String STYLE_DEFAULT = "month";
    public static final String STYLE_THREE_MONTH = "three month average";

    /**
     * Check all timeseries to see if updates need to made to labels
     *
     * @param timeSeries a timeseries
     * @return true if changes are made that need to be saved
     */
    public boolean applyLabels(TimeSeries timeSeries) {

        boolean updatesMade = false;
        for (TimeSeriesValue timeSeriesValue: timeSeries.years)
            updatesMade = updateSimpleDateLabel(timeSeriesValue) || updatesMade;

        for (TimeSeriesValue timeSeriesValue: timeSeries.quarters)
            updatesMade = updateSimpleDateLabel(timeSeriesValue) || updatesMade;

        for (TimeSeriesValue timeSeriesValue: timeSeries.months)
            updatesMade = updateMonthLabel(timeSeries, timeSeriesValue) || updatesMade;

        return updatesMade;
    }

    /**
     * Set value label for a simple point that doesn't require elaborate treatment
     *
     * @param timeSeriesValue the data point in question
     * @return true if changes are made to the timeseries label
     */
    private boolean updateSimpleDateLabel(TimeSeriesValue timeSeriesValue) {
        if (timeSeriesValue.label == null || !timeSeriesValue.label.equalsIgnoreCase(timeSeriesValue.date)) {
            timeSeriesValue.label = timeSeriesValue.date;
            return true;
        }
        return false;
    }

    /**
     * Set value label for months (which can be of various styles)
     *
     * @param timeSeries the timeseries (which will dictate style)
     * @param timeSeriesValue the data point from the timeseries
     * @return true if changes are made that need to be saved
     */
    private boolean updateMonthLabel(TimeSeries timeSeries, TimeSeriesValue timeSeriesValue) {

        String style = timeSeries.getDescription().getMonthLabelStyle();
        if (style == null || style.equalsIgnoreCase(STYLE_DEFAULT)) {

            return updateSimpleDateLabel(timeSeriesValue);
        } else if (style.equalsIgnoreCase(STYLE_THREE_MONTH)) {

            String label = threeMonthAverageLabel(timeSeriesValue);
            if (!label.equalsIgnoreCase(timeSeriesValue.label)) {
                timeSeriesValue.label = label;
                return true;
            }
            return false;
        } else {
            timeSeries.getDescription().setMonthLabelStyle(STYLE_DEFAULT);
            return updateSimpleDateLabel(timeSeriesValue);
        }
    }

    /**
     * Three month average label (one either side of the current date)
     *
     * @param timeSeriesValue the data point in question
     * @return
     */
    private String threeMonthAverageLabel(TimeSeriesValue timeSeriesValue) {
        String[] options = "DEC-FEB,JAN-MAR,FEB-APR,MAR-MAY,APR-JUN,MAY-JUL,JUN-AUG,JUL-SEP,AUG-OCT,SEP-NOV,OCT-DEC,NOV-JAN".split(",");

        Calendar cal = Calendar.getInstance();
        cal.setTime(timeSeriesValue.toDate());

        int month = cal.get(Calendar.MONTH);
        int year = Integer.parseInt(timeSeriesValue.year);

        return String.format("%d %s", month==0?year-1:year, options[month]);
    }

}
