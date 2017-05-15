package com.github.onsdigital.zebedee.reader.api.endpoint;

import com.github.davidcarboni.restolino.framework.Api;
import com.github.onsdigital.zebedee.content.base.Content;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.reader.DataGenerator;
import com.github.onsdigital.zebedee.reader.Resource;
import com.github.onsdigital.zebedee.reader.api.ReadRequestHandler;
import com.github.onsdigital.zebedee.reader.api.ReadRequestHandlerFactory;
import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import java.io.IOException;
import java.util.List;

import static com.github.onsdigital.zebedee.reader.util.ReaderRequestUtils.extractFilter;
import static com.github.onsdigital.zebedee.reader.util.ReaderRequestUtils.getRequestedLanguage;
import static com.github.onsdigital.zebedee.reader.util.ReaderResponseResponseUtils.sendResponse;

/**
 * Created by thomasridd on 07/10/15.
 */
@Api
public class Generator {

    private static final DataGenerator dataGenerator = new DataGenerator();
    private static final String UTF_8 = "UTF-8";
    private static final String FORMAT_PARAM = "format";

    private static final List<String> SUPPORTED_FORMATS = new ImmutableList.Builder<String>()
            .add("csv")
            .add("xls")
            .add("xlsx").build();

    private static final String UNSUPPORTED_FORMAT_MSG = "The requested format is not supported.";
    private static final String UNDEFINED_FORMAT_MSG = "Please specify a format. Supported formats are " +
            SUPPORTED_FORMATS.toString();
    private static final String GENERIC_BAD_REQUEST_MSG = "Invalid file request";

    // Wrap in a lambda function so it can be replaced with a mock in testing.
    private ReadRequestHandlerFactory readRequestHandlerFactory = (contentLanguage) -> new ReadRequestHandler((contentLanguage));

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
        String format = request.getParameter(FORMAT_PARAM);
        if (StringUtils.isEmpty(format)) {
            throw new BadRequestException(UNDEFINED_FORMAT_MSG);
        }

        if (!SUPPORTED_FORMATS.contains(format.toLowerCase())) {
            throw new BadRequestException(UNSUPPORTED_FORMAT_MSG);
        }

        ReadRequestHandler readRequestHandler = readRequestHandlerFactory.get(getRequestedLanguage(request));
        Content content = readRequestHandler.findContent(request, extractFilter(request));

        if (content != null) {
            try (Resource resource = dataGenerator.generateData(content, format)) {
                sendResponse(resource, response, UTF_8);
            }
        }
    }

    public void setReadRequestHandlerFactory(ReadRequestHandlerFactory readRequestHandlerFactory) {
        this.readRequestHandlerFactory = readRequestHandlerFactory;
    }
}
