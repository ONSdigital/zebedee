package com.github.onsdigital.zebedee.data.processing;

import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.xssf.usermodel.XSSFCell;

/**
 * Object to hold the expected value and type of a {@link XSSFCell} - used by {@link DataFileGeneratorTest}.
 */
public class XSSFCellExpectation<T> {

    private  T value;
    private  int cellType;

    protected XSSFCellExpectation(T value) {
        this.value = value;
        if (Integer.class.equals(value.getClass()) || Double.class.equals(value.getClass())) {
            this.cellType = CellType.NUMERIC.getCode();
        } else {
            this.cellType = CellType.STRING.getCode();
        }
    }

    public int getCellType() {
        return cellType;
    }

    public T getValue() {
        return value;
    }
}
