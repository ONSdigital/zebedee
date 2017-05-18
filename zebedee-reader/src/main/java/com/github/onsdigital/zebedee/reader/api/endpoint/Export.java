package com.github.onsdigital.zebedee.reader.api.endpoint;

import com.github.davidcarboni.restolino.framework.Api;
import com.github.onsdigital.zebedee.content.page.statistics.data.timeseries.TimeSeries;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.reader.DataGenerator;
import com.github.onsdigital.zebedee.reader.api.ReadRequestHandler;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.POST;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.github.onsdigital.zebedee.reader.util.ReaderRequestUtils.getRequestedLanguage;
import static com.github.onsdigital.zebedee.reader.util.ReaderResponseResponseUtils.sendResponse;

/**
 * Created by bren on 27/11/15.
 * <p>
 * Generates excel or csv for a list of time series using a uri list passed in as http request
 */
@Api
public class Export {

    private static final DataGenerator dataGenerator = new DataGenerator();
    private static final String UTF_8 = "UTF-8";

    @POST
    public void post(HttpServletRequest request, HttpServletResponse response) throws ZebedeeException, IOException {
        String format = request.getParameter("format");
        if (StringUtils.isEmpty(format)) {
            throw new BadRequestException("Please specify format (csv, xls or xlsx)");
        }

        String[] uris = request.getParameterValues("uri");
        if (ArrayUtils.isEmpty(uris)) {
            throw new BadRequestException("Please specify at least one uri to export data");
        }

        // Try to get a content page
        ReadRequestHandler readRequestHandler = new ReadRequestHandler((getRequestedLanguage(request)));

        List<TimeSeries> timeSeriesList = new ArrayList<>();
        for (int i = 0; i < uris.length; i++) {
            String uri = uris[i];
            timeSeriesList.add((TimeSeries) readRequestHandler.find(request, null, uri));
        }
        try (com.github.onsdigital.zebedee.reader.Resource resource = dataGenerator.generateData(timeSeriesList,
                format)) {
            sendResponse(resource, response, UTF_8);
        }
    }
}
