package com.github.onsdigital.zebedee.data.processing.xls;

import com.github.onsdigital.zebedee.data.processing.DataGridRow;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;

import java.util.regex.Pattern;

import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logError;
import static java.util.Objects.requireNonNull;

/**
 * Class provides functionality for writing
 * {@link com.github.onsdigital.zebedee.content.page.statistics.data.timeseries.TimeSeriesValue}'s to a {@link Cell}
 * if the value is numeric the value will be written as either a Integer or Decimal (depending on the value). If the
 * value is not numberic then it will be written as a String.
 */
public class TimeSeriesCellWriter {

    private static final String DATA_GRID_REQUIRED_MSG = "dataGridRow is required.";
    private static final String ROW_REQUIRED_MSG = "row is required.";
    private static TimeSeriesCellWriter instance = new TimeSeriesCellWriter();

    /**
     * This regex will accept numerical string values that contain 1 or no leading minus symbol followed by 1
     * or more digits i.e. positive/negative whole numbers. If accepted {@link Integer#parseInt(String)} will handle
     * any edge cases with stricter validation.
     */
    protected static final String INTEGER_REGEX = "^-?\\d+$";
    protected static final Pattern INT_PATTERN = Pattern.compile(INTEGER_REGEX);

    /**
     * Regex that will accept string values of positive/negative decimal numbers. If accepted
     * {@link Double#parseDouble(String)} will handle any edge cases with stricter validation.
     */
    protected static final String DECIMAL_REGEX = "^-?\\d+\\.\\d+$";
    protected static final Pattern DECIMAL_PATTERN = Pattern.compile(DECIMAL_REGEX);

    private CellWriter numericCellWriter = new NumericCellWriter();
    private CellWriter stringCellWriter = new StringCellWriter();


    /**
     * @return singleton instance of {@link TimeSeriesCellWriter}.
     */
    public static TimeSeriesCellWriter getInstance() {
        if (instance == null) {
            instance = new TimeSeriesCellWriter();
        }
        return instance;
    }

    private TimeSeriesCellWriter() {
        // singleton pattern use get instance.
    }

    public void writeCells(DataGridRow dataGridRow, Row row) throws NumberFormatException {
        mandatoryCheck(dataGridRow, DATA_GRID_REQUIRED_MSG);
        mandatoryCheck(row, ROW_REQUIRED_MSG);

        writeString(dataGridRow.getLabel(), row);
        dataGridRow
                .getCells()
                .stream()
                .forEach(dataGridRowCell -> write(dataGridRowCell, row));
    }

    public void write(String input, Row row) throws NumberFormatException {
        int cellType = getCellType(input);
        getCellWriter(cellType).writeCell(input, row.createCell(nextColumnIndex(row)));
    }

    public void writeString(String input, Row row) throws NumberFormatException {
        int cellType = Cell.CELL_TYPE_STRING;
        getCellWriter(cellType).writeCell(input, row.createCell(nextColumnIndex(row)));
    }

    /**
     * Columns are indexed from 0, getting the current number of cells is equivalent to last index + 1.
     */
    private int nextColumnIndex(Row r) {
        return r.getPhysicalNumberOfCells();
    }

    /**
     * Get the approprite {@link CellWriter} impl for the requested {@link Cell} type.
     * @param cellType see @{@link Cell#getCellType()}.
     * @return
     * {@link NumericCellWriter} If cellType == {@link Cell#CELL_TYPE_NUMERIC} otherwise {@link StringCellWriter}.
     */
    CellWriter getCellWriter(int cellType) {
        return cellType == Cell.CELL_TYPE_NUMERIC ? numericCellWriter : stringCellWriter;
    }

    /**
     * Determined the {@link Cell#getCellType()} for the supplied value.
     */
    int getCellType(String input) {
        if (StringUtils.isEmpty(input)) {
            return Cell.CELL_TYPE_STRING;
        }
        if (INT_PATTERN.matcher(input).matches() || DECIMAL_PATTERN.matcher(input).matches()) {
            return  Cell.CELL_TYPE_NUMERIC;
        }
        return Cell.CELL_TYPE_STRING;
    }

    private <T> void mandatoryCheck(T t, String message) {
        try {
            requireNonNull(t, message);
        } catch (NullPointerException ex) {
            logError(ex, message)
                    .addParameter("field", t.getClass().getSimpleName())
                    .throwUnchecked(ex);
        }
    }
}
