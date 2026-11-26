package com.github.onsdigital.zebedee.data.processing.xls;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;

/**
 * {@link CellWriter} for writing decimal strings to a {@link Cell} with {@link CellType#NUMERIC} as the cell
 * type.
 */
public class NumericCellWriter implements CellWriter {

    @Override
    public void writeCell(String input, Cell cell) throws NumberFormatException {
        cell.setCellType(CellType.NUMERIC);
        cell.setCellValue(Double.parseDouble(input));
    }
}
