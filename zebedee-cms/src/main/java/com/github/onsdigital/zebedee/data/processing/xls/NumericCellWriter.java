package com.github.onsdigital.zebedee.data.processing.xls;

import org.apache.poi.ss.usermodel.Cell;

/**
 * {@link CellWriter} for writing decimal strings to a {@link Cell} with {@link Cell#CELL_TYPE_NUMERIC} as the cell
 * type.
 */
public class NumericCellWriter implements CellWriter {

    @Override
    public void writeCell(String input, Cell cell) throws NumberFormatException {
        cell.setCellType(Cell.CELL_TYPE_NUMERIC);
        cell.setCellValue(Double.parseDouble(input));
    }
}
