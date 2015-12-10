package com.github.onsdigital.zebedee.content.page.statistics.data.timeseries;

import com.github.onsdigital.zebedee.content.page.base.PageDescription;
import com.github.onsdigital.zebedee.content.page.base.PageType;
import com.github.onsdigital.zebedee.content.page.statistics.data.base.StatisticalData;
import org.apache.commons.lang3.StringUtils;

import java.text.ParseException;
import java.util.*;
import java.util.regex.Pattern;

public class TimeSeries extends StatisticalData implements Comparable<TimeSeries> {

    // Regexes (what might the plural be?)
    public static Pattern year = Pattern.compile("\\d{4}");
    public static Pattern yearEnd = Pattern.compile("ye \\w{3} \\d{2}");
    public static Pattern yearInterval = Pattern.compile("\\d{4}-\\d{2,4}");
    public static Pattern yearPair = Pattern.compile("\\d{4}/\\d{2}");
    public static Pattern month = Pattern.compile("\\d{4} \\w{3}");
    public static Pattern monthNumVal = Pattern.compile("\\d{4} \\d{2}"); //e.g. 2012 02 for february
    public static Pattern quarter = Pattern.compile("\\d{4} \\w[1-4]");


    /**
     * The nectar, the goodness, the very juice of the fireflower: data.
     *
     * Important: {@link #years}, {@link #quarters} and {@link #months} are explicitly declared as
     * {@link TreeSet} so {@link TimeSeriesValue} abjects are correctly ordered when de-serialised from Json.
     */
    public TreeSet<TimeSeriesValue> years = new TreeSet<>();
    public TreeSet<TimeSeriesValue> quarters = new TreeSet<>();
    public TreeSet<TimeSeriesValue> months = new TreeSet<>();

    /**
     * Enables the raw CSV values to be scaled to match the units defined here.
     * <p>
     * E.g. this might be 1000 to convert million-scale in the CSV to
     * billion-scale to match the scale of the headline figure and unit.
     * <p>
     * It's only needed during parsing, so we don't want it serialised to json:
     */
    private transient Double csvValueScaleFactor;

    /**
     * This field is here so that Rob can see which datasets have contributed
     * values. Please don't rely on it unless and until it has been designed
     * into the app with a genuine purpose.
     */
    public List<String> sourceDatasets = new ArrayList<String>();

    public void add(TimeSeriesValue value) {

        try {

            // Get the date represented by this value:
            Calendar calendar = Calendar.getInstance(Locale.UK);
            calendar.setTime(value.toDate());

            // Populate the year. This is needed whether it's
            // yearly, quarterly or monthly:
            value.year = String.valueOf(calendar.get(Calendar.YEAR));

            // Set any other date components and
            // add it to the correct list:
            String key = StringUtils.lowerCase(StringUtils.trim(value.date));
            if (year.matcher(key).matches() || yearInterval.matcher(key).matches() || yearPair.matcher(key).matches()) {
                years.add(value);
            } else if (yearEnd.matcher(key).matches() || quarter.matcher(key).matches()) {
                // Months are zero based, which actually makes the quarter
                // calculation a bit easier:
                value.quarter = "Q" + ((calendar.get(Calendar.MONTH) / 3) + 1);
                quarters.add(value);
            } else if (month.matcher(key).matches()) {
                value.month = calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.UK);
                months.add(value);
            } else {
                throw new ParseException("Unknown format: '" + value.date + "'", 0);
            }

        } catch (ParseException e) {
            throw new RuntimeException("Error parsing date " + value.date, e);
        }
    }

    public void setCdid(String cdid) {
        if (getDescription() == null) {
            setDescription(new PageDescription());
        }
        getDescription().setCdid(StringUtils.trim(cdid));
    }

    public void setScaleFactor(double multiply) {
        this.csvValueScaleFactor = multiply;
    }

    public double getScaleFactor() {
        if (this.csvValueScaleFactor == null) {
            return 1;
        }
        return this.csvValueScaleFactor.doubleValue();
    }

    @Override
    public String toString() {
        return getCdid();
    }

    @Override
    public int hashCode() {
        if (getDescription() != null && getCdid() != null) {
            return getCdid().toLowerCase().hashCode();
        } else {
            return 0;
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (!TimeSeries.class.isAssignableFrom(obj.getClass())) {
            return false;
        }
        return StringUtils.equalsIgnoreCase(((TimeSeries) obj).getCdid(), getCdid());
    }

    @Override
    public int compareTo(TimeSeries o) {
        return getCdid().compareTo(o.getCdid());
    }

    @Override
    public PageType getType() {
        return PageType.timeseries;
    }

    public String getCdid() {
        PageDescription description = getDescription();
        if (description != null) {
            return description.getCdid();
        }
        return null;
    }

}
