package com.github.onsdigital.zebedee.reader.api.endpoint;

import com.github.onsdigital.zebedee.content.base.Content;
import com.github.onsdigital.zebedee.content.page.statistics.data.timeseries.TimeSeries;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.reader.*;
import com.github.onsdigital.zebedee.reader.Resource;
import com.github.onsdigital.zebedee.reader.api.ReadRequestHandler;
import com.github.onsdigital.zebedee.reader.configuration.ReaderConfiguration;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;

import java.io.IOException;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by thomasridd on 07/10/15.
 */
public class GeneratorTest {
    private ReadRequestHandler readRequestHandler;
    HttpServletRequest request = mock(HttpServletRequest.class);
    DataGenerator generator;

    @Before
    public void initialize() {
        ReaderConfiguration.init("target/test-content");
        readRequestHandler = new ReadRequestHandler();
        generator = new DataGenerator();
    }

    @Test
    public void generateChart_givenChartJson_returnsFileForCSV() throws ZebedeeException, IOException {
        // Given
        // sample chart json as a resource
        when(request.getParameter("uri")).thenReturn("/economy/environmentalaccounts/bulletins/ukenvironmentalaccounts/2014-07-02/1fff043a.json");
        try(Resource resource = readRequestHandler.findResource(request)) {

            // When
            // we generate a csv
            try( Resource generated = generator.generateData(resource, "csv") ) {
                assertNotNull(generated);
                assertNotNull(generated.getName());
                assertNotEquals(0, generated.getSize());
            };

        }
    }

    @Test
    public void generateChart_givenChartJson_returnsFileForXLS() throws ZebedeeException, IOException {
        // Given
        // sample chart json as a resource
        when(request.getParameter("uri")).thenReturn("/economy/environmentalaccounts/bulletins/ukenvironmentalaccounts/2014-07-02/1fff043a.json");
        try(Resource resource = readRequestHandler.findResource(request)) {

            // When
            // we generate an xls
            try( Resource generated = generator.generateData(resource, "xls") ) {
                assertNotNull(generated);
                assertNotNull(generated.getName());
                assertNotEquals(0, generated.getSize());
            };

        }
    }

    @Test
    public void generateChart_givenChartJson_returnsFileForXLSX() throws ZebedeeException, IOException {
        // Given
        // sample chart json as a resource
        when(request.getParameter("uri")).thenReturn("/economy/environmentalaccounts/bulletins/ukenvironmentalaccounts/2014-07-02/1fff043a.json");
        try(Resource resource = readRequestHandler.findResource(request)) {

            // When
            // we generate an xls
            try( Resource generated = generator.generateData(resource, "xlsx") ) {
                assertNotNull(generated);
                assertNotNull(generated.getName());
                assertNotEquals(0, generated.getSize());
            };

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
            try( Resource generated = generator.generateData((TimeSeries) content, "csv") ) {

                // Then
                // we should have a non null file with data in it
                assertNotNull(generated);
                assertNotNull(generated.getName());
                assertNotEquals(0, generated.getSize());
            };
    }

    @Test
    public void generateData_givenTimeSeriesJson_returnsFileForXLSX() throws ZebedeeException, IOException {
        // Given
        // sample chart json as a resource
        when(request.getParameter("uri")).thenReturn("/employmentandlabourmarket/peopleinwork/earningsandworkinghours/timeseries/a2f8");
        Content content = readRequestHandler.findContent(request, null);

        // When
        // we generate a csv
        try( Resource generated = generator.generateData((TimeSeries) content, "xlsx") ) {

            // Then
            // we should have a non null file with data in it
            assertNotNull(generated);
            assertNotNull(generated.getName());
            assertNotEquals(0, generated.getSize());
        };
    }

    @Test
    public void generateData_givenTimeSeriesJson_returnsFileForXLS() throws ZebedeeException, IOException {
        // Given
        // sample chart json as a resource
        when(request.getParameter("uri")).thenReturn("/employmentandlabourmarket/peopleinwork/earningsandworkinghours/timeseries/a2f8");
        Content content = readRequestHandler.findContent(request, null);

        // When
        // we generate a csv
        try( Resource generated = generator.generateData((TimeSeries) content, "xls") ) {

            // Then
            // we should have a non null file with data in it
            assertNotNull(generated);
            assertNotNull(generated.getName());
            assertNotEquals(0, generated.getSize());
        };
    }
}