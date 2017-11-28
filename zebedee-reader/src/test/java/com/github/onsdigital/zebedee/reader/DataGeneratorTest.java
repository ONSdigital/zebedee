package com.github.onsdigital.zebedee.reader;

import au.com.bytecode.opencsv.CSVWriter;
import com.github.onsdigital.zebedee.content.base.Content;
import com.github.onsdigital.zebedee.content.page.statistics.document.bulletin.Bulletin;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static com.github.onsdigital.zebedee.reader.DataGenerator.CDID_COL;
import static com.github.onsdigital.zebedee.reader.DataGenerator.NEXT_RELEASE_COL;
import static com.github.onsdigital.zebedee.reader.DataGenerator.NOTES_COL;
import static com.github.onsdigital.zebedee.reader.DataGenerator.PRE_UNIT_COL;
import static com.github.onsdigital.zebedee.reader.DataGenerator.RELEASE_DATE_COL;
import static com.github.onsdigital.zebedee.reader.DataGenerator.SHEET_NAME;
import static com.github.onsdigital.zebedee.reader.DataGenerator.SOURCE_DATASET_COL;
import static com.github.onsdigital.zebedee.reader.DataGenerator.TITLE_COL;
import static com.github.onsdigital.zebedee.reader.DataGenerator.UNIT_COL;
import static com.github.onsdigital.zebedee.reader.DataGenerator.XLS_EXT;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test cases verifying the behaviour of the the {@link DataGenerator}.
 */
public class DataGeneratorTest {

    @Mock
    private Workbook xlsWorkbookMock;

    @Mock
    private Workbook xlsxWorkbookMock;

    @Mock
    private CSVWriter csvWriterMock;

    @Mock
    private Sheet sheetMock;

    @Mock
    private Row rowMock;

    @Mock
    private Cell cellMock;

    @Mock
    private CellStyle styleMock;

    @Mock
    private DataFormat dataFormatMock;

    @Rule
    public TemporaryFolder temporaryFolder;

    private DataGenerator generator;
    private List<List<String>> testDataGrid;

    private int xlsSupplierInvocations = 0;
    private int xlsxSupplierInvocations = 0;
    private int csvWriterFactoryInvocations = 0;

    @Before
    public void setup() throws Exception {
        MockitoAnnotations.initMocks(this);

        temporaryFolder = new TemporaryFolder();
        temporaryFolder.create();

        generator = new DataGenerator();

        generator.setXLSWorkbookSupplier(() -> {
            xlsSupplierInvoked();
            return xlsWorkbookMock;
        });

        generator.setXLSXWorkbookSupplier(() -> {
            xlsSupplierInvoked();
            return xlsxWorkbookMock;
        });

        generator.setCsvWriterFactory((w, sep) -> {
            csvWriteFactoryInvoked();
            return csvWriterMock;
        });

        // Create the mandatory rows & data
        testDataGrid = new ArrayList<>();
        testDataGrid.add(Arrays.asList(new String[]{TITLE_COL, TITLE_COL}));
        testDataGrid.add(Arrays.asList(new String[]{CDID_COL, CDID_COL}));
        testDataGrid.add(Arrays.asList(new String[]{SOURCE_DATASET_COL, SOURCE_DATASET_COL}));
        testDataGrid.add(Arrays.asList(new String[]{PRE_UNIT_COL, PRE_UNIT_COL}));
        testDataGrid.add(Arrays.asList(new String[]{UNIT_COL, UNIT_COL}));
        testDataGrid.add(Arrays.asList(new String[]{RELEASE_DATE_COL, RELEASE_DATE_COL}));
        testDataGrid.add(Arrays.asList(new String[]{NEXT_RELEASE_COL, NEXT_RELEASE_COL}));
        testDataGrid.add(Arrays.asList(new String[]{NOTES_COL, NOTES_COL}));
    }

    @After
    public void tearDown() {
        temporaryFolder.delete();
    }

    private void setUpMockBehaviours() {
        when(xlsWorkbookMock.createSheet(SHEET_NAME))
                .thenReturn(sheetMock);
        when(sheetMock.createRow(anyInt()))
                .thenReturn(rowMock);
        when(rowMock.createCell(anyInt()))
                .thenReturn(cellMock);
        when(xlsWorkbookMock.createCellStyle())
                .thenReturn(styleMock);
        when(xlsWorkbookMock.createDataFormat())
                .thenReturn(dataFormatMock);
    }

    private void xlsSupplierInvoked() {
        xlsSupplierInvocations++;
    }

    private void xlsxSupplierInvoked() {
        xlsxSupplierInvocations++;
    }

    private void csvWriteFactoryInvoked() {
        csvWriterFactoryInvocations++;
    }

    @Test
    public void shouldCreateXLSWithNumericalCellsCorrectlyFormatted() throws Exception {
        testDataGrid.add(Arrays.asList(new String[]{"1990", "12.34"}));
        String fileName = UUID.randomUUID().toString() + XLS_EXT;
        temporaryFolder.getRoot().toPath().resolve(fileName);

        setUpMockBehaviours();

        generator.generateResourceFromDataGrid(testDataGrid, "test.xls");

        assertThat(xlsSupplierInvocations, equalTo(1));
        assertThat(xlsxSupplierInvocations, equalTo(0));
        assertThat(csvWriterFactoryInvocations, equalTo(0));

        verify(sheetMock, times(9)).createRow(anyInt());
        verify(xlsWorkbookMock, times(1)).createCellStyle();
        verify(dataFormatMock, times(1)).getFormat("0.00");

        // 8 meta rows containing a label and a value & 1 data row containing a label and a number == 17 string cells
        // & 1 numeric cell.
        verify(cellMock, times(17)).setCellType(Cell.CELL_TYPE_STRING);
        verify(cellMock, times(1)).setCellType(Cell.CELL_TYPE_NUMERIC);
        verify(xlsWorkbookMock, times(1)).write(any(OutputStream.class));
    }

    @Test
    public void shouldWriteIntergersAsIntergers() throws Exception {
        testDataGrid.add(Arrays.asList(new String[]{"1990", "12"}));
        String fileName = UUID.randomUUID().toString() + XLS_EXT;
        temporaryFolder.getRoot().toPath().resolve(fileName);

        setUpMockBehaviours();

        generator.generateResourceFromDataGrid(testDataGrid, "test.xls");

        assertThat(xlsSupplierInvocations, equalTo(1));
        assertThat(xlsxSupplierInvocations, equalTo(0));
        assertThat(csvWriterFactoryInvocations, equalTo(0));

        verify(sheetMock, times(9)).createRow(anyInt());
        verify(xlsWorkbookMock, never()).createCellStyle();
        verify(dataFormatMock, never()).getFormat(any());

        // 8 meta rows containing a label and a value & 1 data row containing a label and a number == 17 string cells
        // & 1 numeric cell.
        verify(cellMock, times(17)).setCellType(Cell.CELL_TYPE_STRING);
        verify(cellMock, times(1)).setCellType(Cell.CELL_TYPE_NUMERIC);
        verify(xlsWorkbookMock, times(1)).write(any(OutputStream.class));
    }

    @Test
    public void shouldHandleNullAsEmptyString() throws Exception {
        testDataGrid.add(Arrays.asList(new String[]{"1990", null}));
        String fileName = UUID.randomUUID().toString() + XLS_EXT;
        temporaryFolder.getRoot().toPath().resolve(fileName);

        setUpMockBehaviours();

        generator.generateResourceFromDataGrid(testDataGrid, "test.xls");

        assertThat(xlsSupplierInvocations, equalTo(1));
        assertThat(xlsxSupplierInvocations, equalTo(0));
        assertThat(csvWriterFactoryInvocations, equalTo(0));

        verify(sheetMock, times(9)).createRow(anyInt());
        verify(xlsWorkbookMock, never()).createCellStyle();
        verify(dataFormatMock, never()).getFormat(any());
        verify(cellMock, times(18)).setCellType(Cell.CELL_TYPE_STRING);
        verify(cellMock, never()).setCellType(Cell.CELL_TYPE_NUMERIC);
        verify(xlsWorkbookMock, times(1)).write(any(OutputStream.class));
    }

    @Test
    public void shouldHandleEmptyString() throws Exception {
        testDataGrid.add(Arrays.asList(new String[]{"1990", ""}));
        String fileName = UUID.randomUUID().toString() + XLS_EXT;
        temporaryFolder.getRoot().toPath().resolve(fileName);

        setUpMockBehaviours();

        generator.generateResourceFromDataGrid(testDataGrid, "test.xls");

        assertThat(xlsSupplierInvocations, equalTo(1));
        assertThat(xlsxSupplierInvocations, equalTo(0));
        assertThat(csvWriterFactoryInvocations, equalTo(0));

        verify(sheetMock, times(9)).createRow(anyInt());
        verify(xlsWorkbookMock, never()).createCellStyle();
        verify(dataFormatMock, never()).getFormat(any());
        verify(cellMock, times(18)).setCellType(Cell.CELL_TYPE_STRING);
        verify(cellMock, never()).setCellType(Cell.CELL_TYPE_NUMERIC);
        verify(xlsWorkbookMock, times(1)).write(any(OutputStream.class));
    }

    @Test
    public void shouldWriteNonNumericsAsStrings() throws Exception {
        testDataGrid.add(Arrays.asList(new String[]{"1990", "abcdefg"}));
        String fileName = UUID.randomUUID().toString() + XLS_EXT;
        temporaryFolder.getRoot().toPath().resolve(fileName);

        setUpMockBehaviours();

        generator.generateResourceFromDataGrid(testDataGrid, "test.xls");

        assertThat(xlsSupplierInvocations, equalTo(1));
        assertThat(xlsxSupplierInvocations, equalTo(0));
        assertThat(csvWriterFactoryInvocations, equalTo(0));

        verify(sheetMock, times(9)).createRow(anyInt());
        verify(xlsWorkbookMock, never()).createCellStyle();
        verify(dataFormatMock, never()).getFormat(any());
        verify(cellMock, times(18)).setCellType(Cell.CELL_TYPE_STRING);
        verify(cellMock, never()).setCellType(Cell.CELL_TYPE_NUMERIC);
        verify(xlsWorkbookMock, times(1)).write(any(OutputStream.class));
    }

    @Test (expected = BadRequestException.class)
    public void shouldthrowExceptionForUnsupportedFormat() throws Exception {
        generator.generateData(mock(Content.class), "pdf");
    }

    @Test (expected = BadRequestException.class)
    public void shouldThrowExceptionForUnsupportedContent() throws Exception {
        generator.generateData(mock(Bulletin.class), "xls");
    }

    @Test
    public void shouldGenerateCSVForValidInput() throws Exception {
        testDataGrid.add(Arrays.asList(new String[]{"1990", "12.34"}));

        generator.csvToBytes(testDataGrid);

        assertThat(csvWriterFactoryInvocations, equalTo(1));

        testDataGrid.stream().forEach(row -> {
            verify(csvWriterMock, times(1)).writeNext(row.toArray(new String[row.size()]));
        });
        verify(csvWriterMock, times(1)).flush();
    }
}
