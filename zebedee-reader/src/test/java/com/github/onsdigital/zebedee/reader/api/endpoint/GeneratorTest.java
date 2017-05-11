package com.github.onsdigital.zebedee.reader.api.endpoint;

import com.github.onsdigital.zebedee.content.base.Content;
import com.github.onsdigital.zebedee.content.page.statistics.data.timeseries.TimeSeries;
import com.github.onsdigital.zebedee.content.page.statistics.document.figure.chart.Chart;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.reader.DataGenerator;
import com.github.onsdigital.zebedee.reader.Resource;
import com.github.onsdigital.zebedee.reader.api.ReadRequestHandler;
import com.github.onsdigital.zebedee.reader.configuration.ReaderConfiguration;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by thomasridd on 07/10/15.
 */
public class GeneratorTest {

    @Mock
    private HttpServletRequest requestMock;

    @Mock
    private HttpServletResponse responseMock;

    private int readRequestHandlerFactoryInvocationCount = 0;

    private final String uri = "/economy/environmentalaccounts/bulletins/ukenvironmentalaccounts/2014-07-02/1fff043a";
    HttpServletRequest request = mock(HttpServletRequest.class);
    DataGenerator generator;
    private ReadRequestHandler readRequestHandler;

    @Before
    public void initialize() {
        MockitoAnnotations.initMocks(this);
        ReaderConfiguration.init("target/test-classes/test-content/");
        readRequestHandler = new ReadRequestHandler();
        generator = new DataGenerator();
    }

    @Test
    public void generateChart_givenChartJson_returnsFileForCSV() throws ZebedeeException, IOException {
        Chart chart = createChart();
        try (Resource generated = generator.generateData(chart, "csv")) {
            assertNotNull(generated);
            assertNotNull(generated.getName());
        }
    }

    private Chart createChart() {
        Chart chart = new Chart();
        chart.setData(new ArrayList<Map<String, String>>());
        chart.setTitle("Chart title");
        chart.setSubtitle("chart subtitle");
        chart.setNotes("chart notes");
        chart.setUnit("chart unit");
        chart.setHeaders(new ArrayList<String>());
        return chart;
    }

    @Test
    public void generateChart_givenChartJson_returnsFileForXLS() throws ZebedeeException, IOException {
        Chart chart = createChart();
        try (Resource generated = generator.generateData(chart, "xls")) {
            assertNotNull(generated);
            assertNotNull(generated.getName());
        }
    }

    @Test
    public void generateChart_givenChartJson_returnsFileForXLSX() throws ZebedeeException, IOException {
        Chart chart = createChart();
        try (Resource generated = generator.generateData(chart, "xlsx")) {
            assertNotNull(generated);
            assertNotNull(generated.getName());
        }
    }

    @Test
    public void generateData_givenTimeSeriesJson_returnsFileForCSV() throws ZebedeeException, IOException {
        // Given
        // sample chart json as a resource
        when(request.getParameter("uri")).thenReturn("/employmentandlabourmarket/peopleinwork/earningsandworkinghours/timeseries/a2f8");
        Content content = readRequestHandler.findContent(request, null);

        // When
        // we generate a csv
        try (Resource generated = generator.generateData((TimeSeries) content, "csv")) {

            // Then
            // we should have a non null file with data in it
            assertNotNull(generated);
            assertNotNull(generated.getName());
        }
    }

    @Test
    public void generateData_givenTimeSeriesJson_returnsFileForXLSX() throws ZebedeeException, IOException {
        // Given
        // sample chart json as a resource
        when(request.getParameter("uri")).thenReturn("/employmentandlabourmarket/peopleinwork/earningsandworkinghours/timeseries/a2f8");
        Content content = readRequestHandler.findContent(request, null);

        // When
        // we generate a csv
        try (Resource generated = generator.generateData((TimeSeries) content, "xlsx")) {

            // Then
            // we should have a non null file with data in it
            assertNotNull(generated);
            assertNotNull(generated.getName());
        }
    }

    @Test
    public void generateData_givenTimeSeriesJson_returnsFileForXLS() throws ZebedeeException, IOException {
        // Given
        // sample chart json as a resource
        when(request.getParameter("uri")).thenReturn("/employmentandlabourmarket/peopleinwork/earningsandworkinghours/timeseries/a2f8");
        Content content = readRequestHandler.findContent(request, null);

        // When
        // we generate a csv
        try (Resource generated = generator.generateData((TimeSeries) content, "xls")) {

            // Then
            // we should have a non null file with data in it
            assertNotNull(generated);
            assertNotNull(generated.getName());
        }
    }

    @Test(expected = BadRequestException.class)
    public void shouldThrowBadRequestExeForUnsupportedFormats() throws Exception {
        Generator api = new Generator();

        api.setReadRequestHandlerFactory((language) -> {
            incrementInvocationCount();
            return null;
        });

        when(requestMock.getParameter("format"))
                .thenReturn("pdf");
        try {
            api.get(requestMock, responseMock);
        } catch (Exception e) {
            assertThat(this.readRequestHandlerFactoryInvocationCount, equalTo(0));
            throw e;
        }
    }

    private void incrementInvocationCount() {
        this.readRequestHandlerFactoryInvocationCount++;
    }
}