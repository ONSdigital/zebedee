package com.github.onsdigital.zebedee.data.processing;

import com.github.onsdigital.zebedee.content.page.statistics.data.timeseries.TimeSeries;
import com.github.onsdigital.zebedee.content.page.statistics.data.timeseries.TimeSeriesValue;
import com.github.onsdigital.zebedee.data.json.TimeSerieses;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 *
 */
public class DataTimeRange {
    public List<String> years = new ArrayList<>();
    public List<String> months = new ArrayList<>();
    public List<String> quarters = new ArrayList<>();

    public DataTimeRange(TimeSerieses timeSerieses) {
        years = findYearRange(timeSerieses);
        months = findMonthRange(timeSerieses);
        quarters = findQuarterRange(timeSerieses);
    }

    /**
     * Get list of years that should appear in a spreadsheet for a given set of time series
     *
     * @param seriesList the timeseries
     * @return a List of
     */
     List<String> findYearRange(TimeSerieses seriesList) {
        TimeSeriesValue min = null;
        TimeSeriesValue max = null;
        for (TimeSeries series : seriesList) {
            for (TimeSeriesValue value : series.years) {
                if (min == null || min.compareTo(value) > 0) {
                    min = value;
                }
                if (max == null || max.compareTo(value) < 0) {
                    max = value;
                }
            }
        }

        if (min == null) {
            return new ArrayList<>();
        }

        Calendar cal = Calendar.getInstance();
        cal.setTime(min.toDate());
        int minYear = cal.get(Calendar.YEAR);
        cal.setTime(max.toDate());
        int maxYear = cal.get(Calendar.YEAR);

        List<String> yearLabels = new ArrayList<>();
        for (int i = minYear; i <= maxYear; i++) {
            yearLabels.add(i + "");
        }

        return yearLabels;
    }

    /**
     * Get list of quarters that should appear in a spreadsheet for a given set of time series
     *
     * @param seriesList the timeseries
     * @return a List of
     */
     List<String> findQuarterRange(TimeSerieses seriesList) {
        TimeSeriesValue min = null;
        TimeSeriesValue max = null;
        for (TimeSeries series : seriesList) {
            for (TimeSeriesValue value : series.quarters) {
                if (min == null || min.compareTo(value) > 0) {
                    min = value;
                }
                if (max == null || max.compareTo(value) < 0) {
                    max = value;
                }
            }
        }

        if (min == null) {
            return new ArrayList<>();
        }

        Calendar cal = Calendar.getInstance();
        cal.setTime(min.toDate());
        int minYear = cal.get(Calendar.YEAR);
        int minQuarter = cal.get(Calendar.MONTH) / 3;

        cal.setTime(max.toDate());
        int maxYear = cal.get(Calendar.YEAR);
        int maxQuarter = cal.get(Calendar.MONTH) / 3;

        String[] quarters = "Q1,Q2,Q3,Q4".split(",");

        List<String> quarterLabels = new ArrayList<>();

        for (int i = minYear; i <= maxYear; i++) {
            for (int q = 0; q < 4; q++) {
                if (i == minYear) {
                    if (q >= minQuarter) {
                        quarterLabels.add(i + " " + quarters[q]);
                    }
                } else if (i == maxYear) {
                    if (q <= maxQuarter) {
                        quarterLabels.add(i + " " + quarters[q]);
                    }
                } else {
                    quarterLabels.add(i + " " + quarters[q]);
                }
            }
        }

        return quarterLabels;
    }

    /**
     * Get list of months that should appear in a spreadsheet for a given set of time series
     *
     * @param seriesList the timeseries
     * @return a List of
     */
     List<String> findMonthRange(TimeSerieses seriesList) {
        TimeSeriesValue min = null;
        TimeSeriesValue max = null;
        for (TimeSeries series : seriesList) {
            for (TimeSeriesValue value : series.months) {
                if (min == null || min.compareTo(value) > 0) {
                    min = value;
                }
                if (max == null || max.compareTo(value) < 0) {
                    max = value;
                }
            }
        }

        if (min == null) {
            return new ArrayList<>();
        }

        Calendar cal = Calendar.getInstance();
        cal.setTime(min.toDate());
        int minYear = cal.get(Calendar.YEAR);
        int minMonth = cal.get(Calendar.MONTH);

        cal.setTime(max.toDate());
        int maxYear = cal.get(Calendar.YEAR);
        int maxMonth = cal.get(Calendar.MONTH);

        String[] months = "JAN,FEB,MAR,APR,MAY,JUN,JUL,AUG,SEP,OCT,NOV,DEC".split(",");

        List<String> monthLabels = new ArrayList<>();

        for (int i = minYear; i <= maxYear; i++) {
            for (int q = 0; q < 12; q++) {
                if (i == minYear) {
                    if (q >= minMonth) {
                        monthLabels.add(i + " " + months[q]);
                    }
                } else if (i == maxYear) {
                    if (q <= maxMonth) {
                        monthLabels.add(i + " " + months[q]);
                    }
                } else {
                    monthLabels.add(i + " " + months[q]);
                }
            }
        }

        return monthLabels;
    }
}
