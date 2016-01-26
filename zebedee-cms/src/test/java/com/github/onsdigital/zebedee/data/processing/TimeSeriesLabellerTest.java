package com.github.onsdigital.zebedee.data.processing;

import com.github.onsdigital.zebedee.content.page.statistics.data.timeseries.TimeSeries;
import com.github.onsdigital.zebedee.content.page.statistics.data.timeseries.TimeSeriesValue;
import com.github.onsdigital.zebedee.data.framework.DataBuilder;
import com.github.onsdigital.zebedee.data.framework.DataPagesGenerator;
import org.junit.Test;

import java.io.IOException;
import java.util.Date;

import static org.junit.Assert.*;

/**
 * Created by thomasridd on 1/26/16.
 */
public class TimeSeriesLabellerTest {
    DataPagesGenerator generator = new DataPagesGenerator();

    @Test
    public void labeller_forNullMonthStyle_relabels() throws IOException {
        // Given
        // a fresh timeseries with blank values for labels
        TimeSeries timeSeries = generator.exampleTimeseries("cdid", "dataset", new Date(), true, true, true, 5, 2015);

        assertEquals(null, timeSeries.getDescription().getMonthLabelStyle());
        assertEquals(null, timeSeries.years.first().label);
        assertEquals(null, timeSeries.quarters.first().label);
        assertEquals(null, timeSeries.months.first().label);

        // When
        // we run label updates
        boolean updates = new TimeSeriesLabeller().applyLabels(timeSeries);

        // Then
        // updates are made
        assertTrue(updates);
        assertEquals(timeSeries.years.first().date, timeSeries.years.first().label);
        assertEquals(timeSeries.quarters.first().date, timeSeries.quarters.first().label);
        assertEquals(timeSeries.months.first().date, timeSeries.months.first().label);
    }

    @Test
    public void labeller_forDefaultMonthStyle_relabels() throws IOException {
        // Given
        // a fresh timeseries with blank values for labels
        TimeSeries timeSeries = generator.exampleTimeseries("cdid", "dataset", new Date(), true, true, true, 5, 2015);
        timeSeries.getDescription().setMonthLabelStyle(TimeSeriesLabeller.STYLE_DEFAULT);

        assertEquals(TimeSeriesLabeller.STYLE_DEFAULT, timeSeries.getDescription().getMonthLabelStyle());
        assertEquals(null, timeSeries.years.first().label);
        assertEquals(null, timeSeries.quarters.first().label);
        assertEquals(null, timeSeries.months.first().label);

        // When
        // we run label updates
        boolean updates = new TimeSeriesLabeller().applyLabels(timeSeries);

        // Then
        // updates are made
        assertTrue(updates);
        assertEquals(timeSeries.years.first().date, timeSeries.years.first().label);
        assertEquals(timeSeries.quarters.first().date, timeSeries.quarters.first().label);
        assertEquals(timeSeries.months.first().date, timeSeries.months.first().label);
    }

    @Test
    public void labeller_forNotFoundMonthStyle_relabels() throws IOException {
        // Given
        // a fresh timeseries with blank values for labels
        TimeSeries timeSeries = generator.exampleTimeseries("cdid", "dataset", new Date(), true, true, true, 5, 2015);
        timeSeries.getDescription().setMonthLabelStyle("Leif Erikson");

        assertEquals("Leif Erikson", timeSeries.getDescription().getMonthLabelStyle());
        assertEquals(null, timeSeries.years.first().label);
        assertEquals(null, timeSeries.quarters.first().label);
        assertEquals(null, timeSeries.months.first().label);

        // When
        // we run label updates
        boolean updates = new TimeSeriesLabeller().applyLabels(timeSeries);

        // Then
        // updates are made
        assertTrue(updates);
        assertEquals(timeSeries.years.first().date, timeSeries.years.first().label);
        assertEquals(timeSeries.quarters.first().date, timeSeries.quarters.first().label);
        assertEquals(timeSeries.months.first().date, timeSeries.months.first().label);
    }

    @Test
    public void labeller_forThreeMonthStyle_relabels() throws IOException {
        // Given
        // a fresh timeseries with month values and a three month labeller
        TimeSeries timeSeries = generator.exampleTimeseries("cdid", "dataset", new Date(), false, false, true, 1, 2015);
        timeSeries.getDescription().setMonthLabelStyle(TimeSeriesLabeller.STYLE_THREE_MONTH);

        assertEquals(TimeSeriesLabeller.STYLE_THREE_MONTH, timeSeries.getDescription().getMonthLabelStyle());
        assertEquals(null, timeSeries.months.first().label);

        // When
        // we run label updates
        boolean updates = new TimeSeriesLabeller().applyLabels(timeSeries);

        // Then
        // labels are updated appropriately (check first, last, one in the middle)
        assertTrue(updates);
        assertEquals("2014 DEC-FEB", timeSeries.months.first().label);
        assertEquals("2015 NOV-JAN", timeSeries.months.last().label);

        timeSeries.months.remove(timeSeries.months.first());
        assertEquals("2015 JAN-MAR", timeSeries.months.first().label);

    }

    @Test
    public void labeller_forThreeMonthStyle_relabelsOverMonthly() throws IOException {
        // Given
        // a fresh timeseries with month values filled in
        TimeSeries timeSeries = generator.exampleTimeseries("cdid", "dataset", new Date(), false, false, true, 1, 2015);
        timeSeries.getDescription().setMonthLabelStyle(TimeSeriesLabeller.STYLE_DEFAULT);
        new TimeSeriesLabeller().applyLabels(timeSeries);

        assertEquals("2015 JAN", timeSeries.months.first().label);
        assertEquals("2015 DEC", timeSeries.months.last().label);

        // When
        // we change to three month style and run label updates
        timeSeries.getDescription().setMonthLabelStyle(TimeSeriesLabeller.STYLE_THREE_MONTH);
        boolean updates = new TimeSeriesLabeller().applyLabels(timeSeries);


        // Then
        // labels are updated appropriately (check first, last, one in the middle)
        assertTrue(updates);
        assertEquals("2014 DEC-FEB", timeSeries.months.first().label);
        assertEquals("2015 NOV-JAN", timeSeries.months.last().label);

        timeSeries.months.remove(timeSeries.months.first());
        assertEquals("2015 JAN-MAR", timeSeries.months.first().label);

    }

}