package com.github.onsdigital.zebedee.data.processing;

import com.github.onsdigital.zebedee.content.page.base.PageDescription;
import com.github.onsdigital.zebedee.content.page.statistics.data.timeseries.TimeSeries;
import com.github.onsdigital.zebedee.content.page.statistics.data.timeseries.TimeSeriesValue;
import com.github.onsdigital.zebedee.data.json.TimeSerieses;
import com.github.onsdigital.zebedee.data.processing.xls.TimeSeriesCellWriter;
import com.github.onsdigital.zebedee.model.ContentWriter;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import static java.text.MessageFormat.format;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

/**
 * Test verifies {@link DataFileGenerator#writeXLS(DataGrid, ContentWriter, String)} generates the correct output
 * from a {@link DataGrid} object. Test verifies:
 * <ul>
 *     <li>The file contains the expected number of worksheets.</li>
 *     <li>The file contains the expected number of rows & cells.</li>
 *     <li>Each cell contains the expected value and that the cell is of the expected type (see
 *     {@link Cell#getCellType()}).</li>
 * </ul>
 */
@RunWith(Parameterized.class)
public class DataFileGeneratorTest {

    static final String XLS_PATH = "xls";
    static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd-MM-yy");
    static String TITLE = "CPI INDEX 07.1 : PURCHASE OF VEHICLES 2015=100";
    static String CDID = "D7CO";
    static String PRE_UNIT = "";
    static String UNIT = "Index, base year = 100";
    static String RELEASE_DATE = "14-06-2016";
    static String NEXT_RELEASE = "19 July  2016";
    static int YEAR = 1990;

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();
    @Mock
    private ContentWriter contentWriterMock;
    @Mock
    private OutputStream outputStreamMock;
    @Mock
    private TimeSeriesCellWriter intWriterMock;
    @Mock
    private TimeSeriesCellWriter decimalWriterMock;
    @Mock
    private TimeSeriesCellWriter stringWriterMock;
    @Mock
    private Cell cellMock;

    private TimeSeries timeSeries;
    private TimeSerieses timeSeriesList;
    private DataGrid dataGrid;
    private DataFileGenerator dataFileGenerator;
    private XLSExpectations xlsExpectations;
    private File xlsTargetFile;
    private String fileName;

    /**
     * Build 2 sets of TimeSeries data - one with decimal values the other with integer values.
     */
    @Parameterized.Parameters
    public static Collection<Object[]> data() throws ParseException {
        Random random = new Random();
        PageDescription desc = new PageDescription();
        desc.setTitle(TITLE);
        desc.setCdid(CDID);
        desc.setPreUnit(PRE_UNIT);
        desc.setUnit(UNIT);
        desc.setReleaseDate(DATE_FORMAT.parse(RELEASE_DATE));
        desc.setNextRelease(NEXT_RELEASE);

        TimeSeries decimalTS = new TimeSeries();
        TimeSeries integerTS = new TimeSeries();

        decimalTS.setDescription(desc);
        integerTS.setDescription(desc);

        for (int index = 0; index < 10; index++) {
            TimeSeriesValue decimalTSV = new TimeSeriesValue();
            TimeSeriesValue integerTSV = new TimeSeriesValue();

            decimalTSV.date = String.valueOf(++YEAR);
            integerTSV.date = decimalTSV.date;

            StringBuilder dp = new StringBuilder();
            dp.append(random.nextInt(50));

            integerTSV.value = dp.toString();
            decimalTSV.value = dp.append(".")
                    .append(random.nextInt(100))
                    .toString();

            decimalTS.years.add(decimalTSV);
            integerTS.years.add(integerTSV);
        }
        // 2 cases one with decimal value and one with integer values.
        return Arrays.asList(new Object[][]{{decimalTS, "test-decimals.xls"}, {integerTS, "test-integers.xls"}});
    }

    public DataFileGeneratorTest(TimeSeries parameter, String fileName) throws Exception {
        // Set up the test data using the parameterized test values.
        this.timeSeries = parameter;
        this.fileName = fileName;
        this.timeSeriesList = new TimeSerieses();
        this.timeSeriesList.add(timeSeries);
        this.dataGrid = new DataGrid(timeSeriesList);
        this.dataFileGenerator = new DataFileGenerator(null);
        //reset xslCellWriter.. some how as its static it gets substituted for an invalid one.
        DataFileGenerator.xlsCellWriter  = TimeSeriesCellWriter.getInstance();
        this.xlsExpectations = XLSExpectations.get(timeSeries);
    }

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        when(contentWriterMock.getOutputStream(XLS_PATH))
                .thenReturn(outputStreamMock);

        this.xlsTargetFile = new File(temporaryFolder.getRoot().getPath() + XLS_PATH);
    }

    @Test
    public void shouldCreateExpectedXlsFromTimeSeries() throws Exception {
        // Call the test target with the TS to create the XLS file.
        dataFileGenerator.writeXLS(dataGrid, new ContentWriter(xlsTargetFile.toPath()), fileName);

        // Load the created XLS file into Java.
        XSSFWorkbook workbook = new XSSFWorkbook(new FileInputStream(xlsTargetFile.toPath().resolve(fileName)
                .toString()));

        // Verify the rows and cells match what is expected.
        assertThat("Expected 1 workbook sheet.", workbook.getNumberOfSheets(), equalTo(1));
        XSSFSheet xlsSheet = workbook.getSheetAt(0);

        assertThat("Incorrect number of rows.", xlsSheet.getPhysicalNumberOfRows(), equalTo(xlsExpectations.numberOfRows()));

        for (int rowIndex = 0; rowIndex < xlsSheet.getPhysicalNumberOfRows(); rowIndex++) {
            // Get the row and the corresponding row expectation.
            List<XSSFCellExpectation> rowExpectations = xlsExpectations.data(rowIndex);
            XSSFRow spreadsheetRow = xlsSheet.getRow(rowIndex);

            assertThat("Row contains incorrect number of cells", rowExpectations.size(),
                    equalTo(spreadsheetRow.getPhysicalNumberOfCells()));

            for (int cellIndex = 0; cellIndex < spreadsheetRow.getPhysicalNumberOfCells(); cellIndex++) {
                // Get the cell and the corresponding cell expectation.
                XSSFCell cell = spreadsheetRow.getCell(cellIndex);
                XSSFCellExpectation cellExpectation = rowExpectations.get(cellIndex);

                assertThat(format("Cell is type incorrect row: {0} cell: {1}", rowIndex, cellIndex),
                        cell.getCellType(), equalTo(cellExpectation.getCellType()));

                Object cellValue = cellExpectation.getCellType() == XSSFCell.CELL_TYPE_NUMERIC
                        ? cell.getNumericCellValue() : cell.getStringCellValue();

                assertThat(format("Cell is value incorrect: row {0} cell: {1}", rowIndex, cellIndex),
                        cellExpectation.getValue(), equalTo(cellValue));
            }
        }
    }

}
