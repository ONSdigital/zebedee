package com.github.onsdigital.zebedee.data.processing;

import com.github.onsdigital.zebedee.content.page.statistics.data.timeseries.TimeSeries;
import com.github.onsdigital.zebedee.content.page.statistics.data.timeseries.TimeSeriesValue;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

/**
 * Created by dave on 7/11/16.
 */
public class XLSExpectations {

    static int NO_META_ROWS = 7;
    static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd-MM-yyyy");

    private static final int TITLE_INDEX = 0;
    private static final int CDID_INDEX = 1;
    private static final int PREUNIT_INDEX = 2;
    private static final int UNIT_INDEX = 3;
    private static final int RELEASEDATE_INDEX = 4;
    private static final int NEXTRELEASE_INDEX = 5;
    private static final int IMPORTANTNOTES_INDEX = 6;

    enum CellValueType {
        DOUBLE,
        INTEGER,
        STRING
    }

    private List<List<XSSFCellExpectation>> cells;

    public static XLSExpectations get(TimeSeries ts) {
        return new XLSExpectations(ts, CellValueType.DOUBLE);
    }

    private XLSExpectations(TimeSeries timeSeries, CellValueType cellValueType) {
        this.cells = new ArrayList<>();
        addMeta("Title", timeSeries.getDescription().getTitle(), TITLE_INDEX);
        addMeta("CDID", timeSeries.getDescription().getCdid(), CDID_INDEX);
        addMeta("PreUnit", timeSeries.getDescription().getPreUnit(), PREUNIT_INDEX);
        addMeta("Unit", timeSeries.getDescription().getUnit(), UNIT_INDEX);
        addMeta("Release Date", DATE_FORMAT.format(timeSeries.getDescription().getReleaseDate()), RELEASEDATE_INDEX);
        addMeta("Next release", timeSeries.getDescription().getNextRelease(), NEXTRELEASE_INDEX);
        addMeta("Important Notes", "", IMPORTANTNOTES_INDEX);

        List<TreeSet<TimeSeriesValue>> treeSets = new ArrayList<>();
        treeSets.add(timeSeries.years);
        treeSets.add(timeSeries.months);
        treeSets.add(timeSeries.quarters);

        for (TreeSet<TimeSeriesValue> treeSet : treeSets) {
            for (TimeSeriesValue timeSeriesValue : treeSet) {
                List<XSSFCellExpectation> dataRow = new ArrayList<>();
                dataRow.add(new XSSFCellExpectation(timeSeriesValue.date));

                switch (cellValueType) {
                    case DOUBLE:
                        dataRow.add(new XSSFCellExpectation(Double.valueOf(timeSeriesValue.value)));
                        break;
                    case INTEGER:
                        dataRow.add(new XSSFCellExpectation(Integer.parseInt(timeSeriesValue.value)));
                        break;
                    default:
                        dataRow.add(new XSSFCellExpectation(timeSeriesValue.value));
                        break;
                }
                this.addDataRow(dataRow);
            }
        }
    }

    private void addMeta(String name, String value, int index) {
        List<XSSFCellExpectation> row = new ArrayList<>();
        row.add(new XSSFCellExpectation(name));
        row.add(new XSSFCellExpectation(value));
        this.cells.add(index, row);
    }

    public void addDataRow(List<XSSFCellExpectation> row) {
        this.cells.add(row);
    }

    public List<List<XSSFCellExpectation>> dataRows() {
        return cells;
    }

    public List<XSSFCellExpectation> data(int rowIndex) {
        return this.cells.get(rowIndex);
    }

    public int numberOfRows() {
        return this.cells.size();
    }
}
