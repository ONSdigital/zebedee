package com.github.onsdigital.zebedee.reader.api.endpoint;

import com.github.davidcarboni.restolino.framework.Api;
import com.github.onsdigital.zebedee.content.base.Content;
import com.github.onsdigital.zebedee.content.dynamic.timeseries.Series;
import com.github.onsdigital.zebedee.content.page.statistics.data.timeseries.TimeSeries;
import com.github.onsdigital.zebedee.content.page.statistics.document.figure.chart.Chart;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.reader.DataGenerator;
import com.github.onsdigital.zebedee.reader.api.ReadRequestHandler;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import java.io.IOException;

import static com.github.onsdigital.zebedee.reader.util.ReaderRequestUtils.extractFilter;
import static com.github.onsdigital.zebedee.reader.util.ReaderRequestUtils.getRequestedLanguage;
import static com.github.onsdigital.zebedee.reader.util.ReaderResponseResponseUtils.sendResponse;

/**
 * Created by thomasridd on 07/10/15.
 */
@Api
public class Generator {

    private final String UTF_8 = "UTF-8";

    /**
     * Generates on the fly resources for data in csv or xls format
     * <p>
     * Data conversion is solid through the DataGenerator class
     * <p>
     * File type detection requires hardening
     *
     * @param request  - requires parameters uri and format. Format should be csv or xls.
     *                 Uri should be a folder for timeseries or a file for resource objects
     * @param response
     * @throws IOException
     * @throws ZebedeeException
     */
    @GET
    public void get(HttpServletRequest request, HttpServletResponse response) throws IOException, ZebedeeException {
        // Check format string
        String format = request.getParameter("format");
        if (StringUtils.isEmpty(format)) {
            throw new BadRequestException("Please specify format (csv, xls or xlsx)");
        }


        // Try to get a content page
        ReadRequestHandler readRequestHandler = new ReadRequestHandler((getRequestedLanguage(request)));
        Content content = readRequestHandler.findContent(request, extractFilter(request));

        if (content != null) {
            if (content instanceof Chart) {
                // If page is a chart write the chart spreadsheet requested to response
                sendResponse(new DataGenerator().generateData((Chart) content, format), response, UTF_8);
            } else if(content instanceof TimeSeries){
                // If page then write the timeseries spreadsheet requested to response
                sendResponse(new DataGenerator().generateData((TimeSeries) content, format), response,UTF_8);
            } else if (content instanceof Series) {
                sendResponse(new DataGenerator().generateData((Series) content, format), response,UTF_8);
            } else {
                throw new BadRequestException("Invalid file request");
            }
        }
    }
}
