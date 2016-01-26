package com.github.onsdigital.zebedee.content.page.statistics.data.timeseries;

import org.apache.commons.lang3.StringUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class TimeSeriesValue implements Comparable<TimeSeriesValue> {

	// Display values:

	public String date;
	public String value;
	public String label;

	// Values split out into explicit components:

	public String year;
	public String month;
	public String quarter;

	/**
	 * This field is here so that Rob can see which datasets have contributed
	 * values. Please don't rely on it unless and until it has been designed
	 * into the app with a genuine purpose.
	 */
	public String sourceDataset;

	public Date updateDate;

	/**
	 * We don't want to serialise this, but it's useful to keep a cached copy
	 * because the regex and Calendar work is expensive, particularly given the
	 * amount of data we need to handle.
	 */
	private transient Date toDate;

	/**
	 * Calls {@link #toDate()} and returns {@link Date#hashCode()}.
	 */
	@Override
	public int hashCode() {
		return toDate().hashCode();
	}

	/**
	 * Calls {@link #toDate()} and uses {@link Date#equals(Object)}.
	 */
	@Override
	public boolean equals(Object obj) {
		return obj != null && this.getClass().isAssignableFrom(obj.getClass()) && toDate().equals(((TimeSeriesValue) obj).toDate());
	}

	/**
	 * Calls {@link #toDate()} and returns {@link Date#compareTo(Date)}.
	 */
	@Override
	public int compareTo(TimeSeriesValue o) {
		return toDate().compareTo(o.toDate());
	}

	public Date toDate() {
		if (toDate == null) {
			toDate = toDate(date);
		}
		return toDate;
	}

	public static Date toDate(String date) {
		Date result;

		try {
			String standardised = StringUtils.lowerCase(StringUtils.trim(date));
			if (TimeSeries.year.matcher(standardised).matches()) {
				result = new SimpleDateFormat("yyyy").parse(standardised);
			} else if (TimeSeries.month.matcher(standardised).matches()) {
				result = new SimpleDateFormat("yyyy MMM").parse(standardised);
			} else if (TimeSeries.monthNumVal.matcher(standardised).matches()) {
				result = new SimpleDateFormat("yyyy MM").parse(standardised);
			} else if (TimeSeries.quarter.matcher(standardised).matches()) {
				Date parsed = new SimpleDateFormat("yyyy").parse(standardised);
				Calendar calendar = Calendar.getInstance(Locale.UK);
				calendar.setTime(parsed);
				if (standardised.endsWith("1")) {
					calendar.set(Calendar.MONTH, Calendar.JANUARY);
				} else if (standardised.endsWith("2")) {
					calendar.set(Calendar.MONTH, Calendar.APRIL);
				} else if (standardised.endsWith("3")) {
					calendar.set(Calendar.MONTH, Calendar.JULY);
				} else if (standardised.endsWith("4")) {
					calendar.set(Calendar.MONTH, Calendar.OCTOBER);
				} else {
					throw new RuntimeException("Didn't detect quarter in " + standardised);
				}
				result = calendar.getTime();
			} else if (TimeSeries.yearInterval.matcher(standardised).matches()) {
				result = new SimpleDateFormat("yyyy").parse(standardised.substring("yyyy-".length()));
			} else if (TimeSeries.yearPair.matcher(standardised).matches()) {
				result = new SimpleDateFormat("yy").parse(standardised.substring("yyyy/".length()));
			} else if (TimeSeries.yearEnd.matcher(standardised).matches()) {
				result = new SimpleDateFormat("MMM yy").parse(standardised.substring("YE ".length()));
			} else {
				throw new ParseException("Unknown format: '" + date + "'", 0);
			}
		} catch (ParseException e) {
			throw new RuntimeException("Error parsing date: '" + date + "'", e);
		}

		return result;
	}

	@Override
	public String toString() {
		return date + ":" + value;
	}

}
