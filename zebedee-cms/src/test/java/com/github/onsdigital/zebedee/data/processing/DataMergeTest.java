package com.github.onsdigital.zebedee.data.processing;

import com.github.onsdigital.zebedee.content.page.statistics.data.timeseries.TimeSeries;
import com.github.onsdigital.zebedee.content.page.statistics.data.timeseries.TimeSeriesValue;
import com.github.onsdigital.zebedee.data.framework.DataPagesGenerator;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.TreeSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by thomasridd on 1/21/16.
 */
public class DataMergeTest {
    DataPagesGenerator generator;

    @Before
    public void setUp() throws Exception {
        generator = new DataPagesGenerator();
    }

    @Test
    public void mergeValues_overEmptyTimeSeries_shouldFillValues() {
        // Given
        // an empty timeseries and a simple set of updates from 2000-2002
        TimeSeries initial = clearTimeSeries(generator.exampleTimeseries("cdid", "dataset"));
        TimeSeries updates = simplifyTimeSeries(generator.exampleTimeseries("cdid", "dataset"));

        // When
        // we run a merge
        DataMerge dataMerge = new DataMerge();
        TimeSeries merged = dataMerge.merge(initial, updates, "update");

        // Then
        // we expect
        assertEquals(updates.years.size(), merged.years.size());
        assertTimeSeriesPointsMatch(updates.years, merged.years);
        assertTimeSeriesPointsMatch(updates.months, merged.months);
        assertTimeSeriesPointsMatch(updates.quarters, merged.quarters);
    }

    @Test
    public void mergeValues_overExistingTimeSeries_shouldAddNewPoints() {
        // Given
        // a timeseries for 2000-2002 and a simple update for 2003
        TimeSeries initial = simplifyTimeSeries(generator.exampleTimeseries("cdid", "dataset"));
        TimeSeries updates = clearTimeSeries(generator.exampleTimeseries("cdid", "dataset"));
        updates.add(quickTimeSeriesValue("2003", "4"));

        // When
        // we run a merge
        DataMerge dataMerge = new DataMerge();
        TimeSeries merged = dataMerge.merge(initial, updates, "update");

        // Then
        // we expect the merge to include 4 points including 2003
        assertEquals(4, merged.years.size());
        assertEquals(1, dataMerge.insertions);
        assertEquals("4", valueForTime("2003", merged).value);
    }

    @Test
    public void mergeValues_overExistingTimeSeries_shouldOverwriteExistingPoints() throws IOException {
        // Given
        // a timeseries for 2000-2002 and a simple update for 2003
        TimeSeries initial = simplifyTimeSeries(generator.exampleTimeseries("cdid", "dataset"));
        TimeSeries updates = clearTimeSeries(generator.exampleTimeseries("cdid", "dataset"));
        updates.add(quickTimeSeriesValue("2002", "4"));

        // When
        // we run a merge
        DataMerge dataMerge = new DataMerge();
        TimeSeries merged = dataMerge.merge(initial, updates, "update");

        // Then
        // we expect the merge to include 3 points including 2003
        assertEquals(3, merged.years.size());
        assertEquals(3, dataMerge.corrections);
        assertEquals("4", valueForTime("2002", merged).value);
    }

    @Test
    public void mergeValues_overExistingTimeSeries_shouldNotRemoveExistingPoints() throws IOException {
        // Given
        // a timeseries for 2000-2002 and a simple update for 2003
        TimeSeries initial = simplifyTimeSeries(generator.exampleTimeseries("cdid", "dataset"));
        TimeSeries updates = clearTimeSeries(generator.exampleTimeseries("cdid", "dataset"));
        updates.add(quickTimeSeriesValue("2003", "4"));

        // When
        // we run a merge
        DataMerge dataMerge = new DataMerge();
        TimeSeries merged = dataMerge.merge(initial, updates, "update");

        // Then
        // we expect the merge to include 3 points including 2003
        assertEquals(4, merged.years.size());
        assertEquals(1, dataMerge.insertions);
        assertEquals("", valueForTime("2000", merged).value);
        assertEquals("", valueForTime("2001", merged).value);
        assertEquals("", valueForTime("2002", merged).value);
        assertEquals("4", valueForTime("2003", merged).value);
    }

    @Test
    public void mergeValues_overExistingTimeSeries_shouldBlankOutSuppressedValues() throws IOException {
        // Given a timeseries for 2000-2002 and an set of updates that suppresses the 2002 value
        TimeSeries initial = simplifyTimeSeries(generator.exampleTimeseries("cdid", "dataset"));
        TimeSeries updates = simplifyTimeSeries(generator.exampleTimeseries("cdid", "dataset"));

        TimeSeriesValue suppressedEntry = updates.years.last();
        updates.years.remove(suppressedEntry);

        // When a merge is done
        DataMerge dataMerge = new DataMerge();
        TimeSeries merged = dataMerge.merge(initial, updates, "update");

        // Then the merged data should have a blank value for the suppressed 2002 entry
        // counted as a correction.
        assertEquals(3, merged.years.size());
        assertEquals("", valueForTime("2002", merged).value);
        assertEquals(1, dataMerge.corrections);
    }


    private void assertTimeSeriesPointsMatch(TreeSet<TimeSeriesValue> values1, TreeSet<TimeSeriesValue> values2) {
        assertEquals(values1.size(), values2.size());
        for (TimeSeriesValue value1 : values1) {
            boolean found = false;
            for (TimeSeriesValue value2 : values2) {
                if (value1.value.equalsIgnoreCase(value2.value) && value1.toDate().compareTo(value2.toDate()) == 0) {
                    found = true;
                }
            }
            assertTrue(found);
        }
    }

    // Time series test helpers
    private TimeSeries simplifyTimeSeries(TimeSeries series) {

        clearTimeSeries(series);
        series.years.add(quickTimeSeriesValue("2000", "1"));
        series.years.add(quickTimeSeriesValue("2001", "2"));
        series.years.add(quickTimeSeriesValue("2002", "3"));

        return series;
    }

    private TimeSeries clearTimeSeries(TimeSeries series) {
        series.years = new TreeSet<>();
        series.months = new TreeSet<>();
        series.quarters = new TreeSet<>();
        return series;
    }

    /**
     * Get a simple timeseries value for a year with a value
     *
     * @param year  any year as a string
     * @param value any value
     * @return
     */
    private TimeSeriesValue quickTimeSeriesValue(String year, String value) {
        TimeSeriesValue timeseriesValue = new TimeSeriesValue();
        timeseriesValue.year = year;
        timeseriesValue.value = value;
        timeseriesValue.date = year;
        return timeseriesValue;
    }

    /**
     * Get a value for a given year in a timeseries
     *
     * @param year   any year as a string
     * @param series a series
     * @return
     */
    private TimeSeriesValue valueForTime(String year, TimeSeries series) {
        for (TimeSeriesValue value : series.years) {
            if (value.year.equals(year)) {
                return value;
            }
        }
        return null;
    }
}