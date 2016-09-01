package com.github.onsdigital.zebedee.data.processing.xls;

import com.github.onsdigital.zebedee.data.processing.DataGridRow;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.util.ReflectionTestUtils.setField;

public class TimeSeriesCellWriterTest {

    private static final String LABEL = "Title";
    private static final String DECIMAL_VALUE = "0.0";
    private static final String INT_VALUE = "0";
    private static final int TOTAL_ENTRIES = 5;

    @Mock
    private Row rowMock;
    @Mock
    private Cell cellMock;
    @Mock
    private CellWriter stringCellWriterMock;
    @Mock
    private CellWriter numericCellWriterMock;

    private Answer<Integer> numberOfCells = new Answer<Integer>() {
        int cellCount = -1;

        @Override
        public Integer answer(InvocationOnMock invocationOnMock) throws Throwable {
            cellCount++;
            return cellCount;
        }
    };

    private TimeSeriesCellWriter writer;
    private DataGridRow dataGridRowDecimals;
    private DataGridRow dataGridRowIntegers;
    private DataGridRow dataGridRowNonNumeric;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        writer = TimeSeriesCellWriter.getInstance();

        dataGridRowDecimals = new DataGridRow("1", LABEL);
        dataGridRowDecimals.getCells().add(DECIMAL_VALUE);
        dataGridRowDecimals.getCells().add(DECIMAL_VALUE);
        dataGridRowDecimals.getCells().add(DECIMAL_VALUE);
        dataGridRowDecimals.getCells().add(DECIMAL_VALUE);
        dataGridRowDecimals.getCells().add(DECIMAL_VALUE);

        dataGridRowIntegers = new DataGridRow("1", LABEL);
        dataGridRowIntegers.getCells().add(INT_VALUE);
        dataGridRowIntegers.getCells().add(INT_VALUE);
        dataGridRowIntegers.getCells().add(INT_VALUE);
        dataGridRowIntegers.getCells().add(INT_VALUE);
        dataGridRowIntegers.getCells().add(INT_VALUE);

        dataGridRowNonNumeric = new DataGridRow("1", LABEL);
        dataGridRowNonNumeric.getCells().add("abcd");
        dataGridRowNonNumeric.getCells().add("-7^1.3");
        dataGridRowNonNumeric.getCells().add("0..0");
        dataGridRowNonNumeric.getCells().add("+_)Ykkf");
        dataGridRowNonNumeric.getCells().add("one");
    }

    @Test
    public void shouldUserIntWriterForIntStringValues() throws Exception {
        when(rowMock.getPhysicalNumberOfCells())
                .then(numberOfCells);
        when(rowMock.createCell(anyInt()))
                .thenReturn(cellMock);

        // run test.
        writer.writeCells(dataGridRowDecimals, rowMock);

        verify(rowMock, times(TOTAL_ENTRIES + 1)).getPhysicalNumberOfCells(); // Title row + 5 data rows.
        verify(rowMock, times(1)).createCell(0);
        verify(rowMock, times(1)).createCell(1);
        verify(rowMock, times(1)).createCell(2);
        verify(rowMock, times(1)).createCell(3);
        verify(rowMock, times(1)).createCell(4);
        verify(cellMock, times(5)).setCellType(Cell.CELL_TYPE_NUMERIC);
        verify(cellMock, times(1)).setCellType(Cell.CELL_TYPE_STRING);
        verify(cellMock, times(1)).setCellValue(LABEL);
        verify(cellMock, times(TOTAL_ENTRIES)).setCellValue(0.0);
    }

    @Test
    public void shouldUseDecimalWriterForDecimalStrings() throws Exception {
        setWriterMocks();
        when(rowMock.getPhysicalNumberOfCells())
                .then(numberOfCells);
        when(rowMock.createCell(anyInt()))
                .thenReturn(cellMock);

        // run test.
        writer.writeCells(dataGridRowDecimals, rowMock);

        verify(rowMock, times(TOTAL_ENTRIES + 1)).getPhysicalNumberOfCells(); // Title row + 5 data rows.
        verify(stringCellWriterMock, times(1)).writeCell(dataGridRowDecimals.getLabel(), cellMock);
        verify(numericCellWriterMock, times(TOTAL_ENTRIES)).writeCell(DECIMAL_VALUE, cellMock);
    }

    @Test
    public void shouldUseIntegerWriterForIntegerStrings() throws Exception {
        setWriterMocks();
        when(rowMock.getPhysicalNumberOfCells())
                .then(numberOfCells);
        when(rowMock.createCell(anyInt()))
                .thenReturn(cellMock);

        // run test.
        writer.writeCells(dataGridRowIntegers, rowMock);

        verify(rowMock, times(TOTAL_ENTRIES + 1)).getPhysicalNumberOfCells(); // Title row + 5 data rows.
        verify(stringCellWriterMock, times(1)).writeCell(dataGridRowIntegers.getLabel(), cellMock);
        verify(numericCellWriterMock, times(TOTAL_ENTRIES)).writeCell(INT_VALUE, cellMock);
    }

    @Test
    public void shouldUseStringWriterForNonNumericStrings() throws Exception {
        setWriterMocks();
        when(rowMock.getPhysicalNumberOfCells())
                .then(numberOfCells);
        when(rowMock.createCell(anyInt()))
                .thenReturn(cellMock);

        // run test.
        writer.writeCells(dataGridRowNonNumeric, rowMock);

        verify(rowMock, times(TOTAL_ENTRIES + 1)).getPhysicalNumberOfCells(); // Title row + 5 data rows.
        verify(stringCellWriterMock, times(1)).writeCell(dataGridRowNonNumeric.getLabel(), cellMock);
        verify(stringCellWriterMock, times(6)).writeCell(anyString(), eq(cellMock));
        dataGridRowNonNumeric.getCells().stream()
                .forEach(item -> verify(stringCellWriterMock, times(1)).writeCell(item, cellMock));
        verify(numericCellWriterMock, never()).writeCell(INT_VALUE, cellMock);
    }

    @Test
    public void shouldReturnCorrectCellWriterForCellType() {
        setWriterMocks();
        assertThat("Incorrect CellWriter was returned.", numericCellWriterMock,
                equalTo(writer.getCellWriter(Cell.CELL_TYPE_NUMERIC)));

        assertThat("Incorrect CellWriter was returned.", stringCellWriterMock,
                equalTo(writer.getCellWriter(Cell.CELL_TYPE_STRING)));

        assertThat("Incorrect CellWriter was returned.", stringCellWriterMock,
                equalTo(writer.getCellWriter(Cell.CELL_TYPE_BLANK)));
    }

    @Test
    public void shouldReturnNumericCellType() {
        setWriterMocks();
        assertThat("Incorrect CellWriter was returned.", Cell.CELL_TYPE_NUMERIC,
                equalTo(writer.getCellType("0")));

        assertThat("Incorrect CellWriter was returned.", Cell.CELL_TYPE_NUMERIC,
                equalTo(writer.getCellType("0.001")));

        assertThat("Incorrect CellWriter was returned.", Cell.CELL_TYPE_NUMERIC,
                equalTo(writer.getCellType("-0.001")));

        assertThat("Incorrect CellWriter was returned.", Cell.CELL_TYPE_NUMERIC,
                equalTo(writer.getCellType("-0")));

        assertThat("Incorrect CellWriter was returned.", Cell.CELL_TYPE_NUMERIC,
                equalTo(writer.getCellType("01")));
    }

    @Test
    public void shouldReturnStringCellType() {
        setWriterMocks();
        assertThat("Incorrect CellWriter was returned.", Cell.CELL_TYPE_STRING,
                equalTo(writer.getCellType("Hello")));

        assertThat("Incorrect CellWriter was returned.", Cell.CELL_TYPE_STRING,
                equalTo(writer.getCellType(null)));

        assertThat("Incorrect CellWriter was returned.", Cell.CELL_TYPE_STRING,
                equalTo(writer.getCellType("")));

        assertThat("Incorrect CellWriter was returned.", Cell.CELL_TYPE_STRING,
                equalTo(writer.getCellType("" +
                        ".123")));
    }

    @Test(expected = NullPointerException.class)
    public void shouldThrowNPXForMissingDataGridRow() {
        writer.writeCells(null, rowMock);
    }

    @Test(expected = NullPointerException.class)
    public void shouldThrowNPXForMissingRow() {
        writer.writeCells(dataGridRowDecimals, null);
    }

    private void setWriterMocks() {
        setField(writer, "stringCellWriter", stringCellWriterMock);
        setField(writer, "numericCellWriter", numericCellWriterMock);
    }

    @After
    public void tidyUp() throws Exception {
        setField(writer, "instance", null);
    }
}
