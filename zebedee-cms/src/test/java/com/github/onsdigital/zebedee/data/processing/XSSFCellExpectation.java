package com.github.onsdigital.zebedee.data.processing;

import org.apache.poi.xssf.usermodel.XSSFCell;

/**
 * Object to hold the expected value and type of a {@link XSSFCell} - used by {@link DataFileGeneratorTest}.
 */
public class XSSFCellExpectation<T> {

    protected T value;
    protected int cellType;

    protected XSSFCellExpectation(T value) {
        this.value = value;
        if (Integer.class.equals(value.getClass()) || Double.class.equals(value.getClass())) {
            this.cellType = XSSFCell.CELL_TYPE_NUMERIC;
        } else {
            this.cellType = XSSFCell.CELL_TYPE_STRING;
        }
    }

    public int getCellType() {
        return cellType;
    }

    public T getValue() {
        return value;
    }
}
