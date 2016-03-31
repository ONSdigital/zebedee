package com.github.onsdigital.zebedee.data.processing;

import com.github.onsdigital.zebedee.content.page.statistics.data.timeseries.TimeSeries;
import com.github.onsdigital.zebedee.content.page.statistics.data.timeseries.TimeSeriesValue;
import com.github.onsdigital.zebedee.data.json.TimeSerieses;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.FastDateFormat;

import java.util.*;

/**
 * Created by thomasridd on 1/23/16.
 */
public class DataGrid {
    // The date format including the BST timezone. Dates are stored at UTC and must be formated to take BST into account.
    private static FastDateFormat formatter = FastDateFormat.getInstance("dd-MM-yyyy", TimeZone.getTimeZone("Europe/London"));

    public List<DataGridRow> metadata;
    public List<DataGridRow> rows;


    public DataGrid(TimeSerieses timeSerieses) {
        metadata = getMetadataRows(timeSerieses);
        rows = getTimeSeriesRows(timeSerieses);
    }

    /**
     * Get a table of the metadata rows
     *
     * @param timeSerieses the data as a list of timeseries pages
     * @return
     */
    List<DataGridRow> getMetadataRows(TimeSerieses timeSerieses) {
        DataGridRow title = new DataGridRow("Title");
        DataGridRow cdid = new DataGridRow("CDID");
        DataGridRow nationalStatistic = new DataGridRow("National Statistic");
        DataGridRow preunit = new DataGridRow("PreUnit");
        DataGridRow unit = new DataGridRow("Unit");
        DataGridRow releaseDate = new DataGridRow("Release Date");
        DataGridRow nextRelease = new DataGridRow("Next release");
        DataGridRow importantNotes = new DataGridRow("Important Notes");

        for (TimeSeries timeSeries : timeSerieses) {
            title.cells.add(timeSeries.getDescription().getTitle());
            cdid.cells.add(timeSeries.getDescription().getCdid());
            nationalStatistic.cells.add(timeSeries.getDescription().isNationalStatistic() ? "Y" : "N");
            preunit.add(timeSeries.getDescription().getPreUnit());
            unit.add(timeSeries.getDescription().getUnit());

            if (timeSeries.getDescription().getReleaseDate() == null) {
                releaseDate.add("");
            } else {
                releaseDate.add(formatter.format(timeSeries.getDescription().getReleaseDate()));
            }

            nextRelease.add(timeSeries.getDescription().getNextRelease());
            importantNotes.add(StringUtils.join(timeSeries.getNotes()));
        }

        List<DataGridRow> results = new ArrayList<>();
        results.add(title);
        results.add(cdid);
//        results.add(nationalStatistic); - removed until we have sufficient data to back it up
        results.add(preunit);
        results.add(unit);
        results.add(releaseDate);
        results.add(nextRelease);
        results.add(importantNotes);

        return results;
    }

    /**
     * Get a table of the timeseries data rows
     *
     * @param timeSerieses the data as a list of timeseries pages
     * @return
     */
    List<DataGridRow> getTimeSeriesRows(TimeSerieses timeSerieses) {
        // Get the rows that we need to record
        DataTimeRange range = new DataTimeRange(timeSerieses);

        // Get an empty grid (with appropriate rows)
        Map<String, DataGridRow> timeMap = emptyRowsWithRange(range, timeSerieses.size());

        // Fill in the grid
        for (int i = 0; i < timeSerieses.size(); i++)
            fillTimeSeriesValuesInMap(timeMap, timeSerieses.get(i), i);

        // Convert to an ordered list of rows
        return timeMapToList(range, timeMap);
    }

    /**
     * Get a map of time period to a DataGridRow with n blank cells
     *
     * @param range           the DataTimeRange we are referring to
     * @param timeSeriesCount the number of columns required per DataGridRow
     * @return a template map to fill in grid rows
     */
    Map<String, DataGridRow> emptyRowsWithRange(DataTimeRange range, int timeSeriesCount) {
        Map<String, DataGridRow> rows = new HashMap<>();
        if(range.years != null) {
            for (String year : range.years)
                rows.put(year, new DataGridRow(year, timeSeriesCount));
        }
        if(range.quarters != null) {
            for (String quarter : range.quarters)
                rows.put(quarter, new DataGridRow(quarter, timeSeriesCount));
        }
        if(range.months != null) {
            for (String month : range.months)
                rows.put(month, new DataGridRow(month, timeSeriesCount));
        }
        return rows;
    }

    /**
     * Fill in a template map of DataGridRow with values from a timeseries
     *
     * @param map        the template to fill
     * @param timeSeries the series to take values from
     * @param column     the column of each DataGridRow to fill
     */
    void fillTimeSeriesValuesInMap(Map<String, DataGridRow> map, TimeSeries timeSeries, int column) {

        for (TimeSeriesValue value : timeSeries.years)
            map.get(value.date).cells.set(column, value.value);
        for (TimeSeriesValue value : timeSeries.months)
            map.get(value.date).cells.set(column, value.value);
        for (TimeSeriesValue value : timeSeries.quarters)
            map.get(value.date).cells.set(column, value.value);
    }

    /**
     * Convert a map of DataGridRow to a list using time periods from a DataTimeRange as an order
     *
     * @param range   a DataTimeRange used to order the DataGridRows by [years, quarters, months] then date
     * @param timeMap the input map
     * @return
     */
    List<DataGridRow> timeMapToList(DataTimeRange range, Map<String, DataGridRow> timeMap) {
        List<DataGridRow> list = new ArrayList<>();
        for (String year : range.years)
            list.add(timeMap.get(year));
        for (String quarter : range.quarters)
            list.add(timeMap.get(quarter));
        for (String month : range.months)
            list.add(timeMap.get(month));
        return list;
    }

}