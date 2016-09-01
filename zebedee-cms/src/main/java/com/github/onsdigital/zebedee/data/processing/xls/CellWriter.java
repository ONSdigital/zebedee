package com.github.onsdigital.zebedee.data.processing.xls;

import org.apache.poi.ss.usermodel.Cell;

/**
 * Defines API for a custom {@link Cell} writer to maintain control over how a String value should be written to the
 * cell.
 */
@FunctionalInterface
public interface CellWriter {

    /**
     * Write the input value to the {@link Cell} with the format specified.
     *
     * @param value the value to write.
     * @param cell  the {@link Cell} to write the value to.
     * @throws NumberFormatException problem converting the value to a number.
     */
    void writeCell(String value, Cell cell) throws NumberFormatException;
}
