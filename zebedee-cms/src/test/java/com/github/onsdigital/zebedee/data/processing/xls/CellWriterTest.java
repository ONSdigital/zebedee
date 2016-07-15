package com.github.onsdigital.zebedee.data.processing.xls;

import org.apache.poi.ss.usermodel.Cell;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.Collection;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Tests verify the behaviour of the different {@link CellWriter} implementations with valid and invalid input.
 */
@RunWith(Parameterized.class)
public class CellWriterTest {

    @Mock
    private Cell cellMock;

    private CellWriter numericCellWriter;
    private CellWriter stringCellWriter;
    private String doubleStr;
    private Double doubleVal;

    @Parameterized.Parameters
    public static Collection<String> data() {
        return Arrays.asList(new String[]{"99.9576", "-99.9576", "-99", "99", "01", ".01"});
    }

    public CellWriterTest(String doubleStr) {
        this.doubleStr = doubleStr;
        this.doubleVal = Double.parseDouble(doubleStr);
    }

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        numericCellWriter = new NumericCellWriter();
        stringCellWriter = new StringCellWriter();
    }

    @Test
    public void shouldWriteValueAndSetCellTypeAsNumeric() {
        numericCellWriter.writeCell(doubleStr, cellMock);

        verify(cellMock, times(1)).setCellType(Cell.CELL_TYPE_NUMERIC);
        verify(cellMock, times(1)).setCellValue(doubleVal);
    }

    @Test
    public void shouldWriteValueAndSetCellTypeAsString() {
        stringCellWriter.writeCell(doubleStr, cellMock);

        verify(cellMock, times(1)).setCellType(Cell.CELL_TYPE_STRING);
        verify(cellMock, times(1)).setCellValue(doubleStr);
    }


    @Test(expected = NumberFormatException.class)
    public void shouldThrowNumberFormatExForNonNumericStringValue() {
        try {
            numericCellWriter.writeCell(doubleStr + "xyz", cellMock);
        } catch (NumberFormatException e) {
            verify(cellMock, times(1)).setCellType(Cell.CELL_TYPE_NUMERIC);
            verify(cellMock, never()).setCellValue(anyString());
            throw e;
        }
    }
}
