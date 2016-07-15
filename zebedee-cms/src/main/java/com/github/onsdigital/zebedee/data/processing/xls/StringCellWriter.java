package com.github.onsdigital.zebedee.data.processing.xls;

import org.apache.poi.ss.usermodel.Cell;

/**
 * Created by dave on 7/13/16.
 */
public class StringCellWriter implements CellWriter {

    @Override
    public void writeCell(String value, Cell cell) throws NumberFormatException {
        cell.setCellType(Cell.CELL_TYPE_STRING);
        cell.setCellValue(value);
    }
}
