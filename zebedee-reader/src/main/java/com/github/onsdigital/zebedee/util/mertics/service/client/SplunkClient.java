package com.github.onsdigital.zebedee.util.mertics.service.client;

import com.github.onsdigital.zebedee.util.mertics.model.SplunkRequestMessage;
import com.splunk.Args;
import com.splunk.ResponseMessage;
import com.splunk.Service;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;

import java.io.IOException;
import java.io.StringWriter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;

import static com.github.onsdigital.zebedee.logging.ZebedeeReaderLogBuilder.logError;
import static com.github.onsdigital.zebedee.logging.ZebedeeReaderLogBuilder.logWarn;

/**
 * Client encapsulates sending HTTP collection events to Splunk.
 */
public class SplunkClient {

    protected static ExecutorService pool = Executors.newSingleThreadExecutor();
    private static final String UNEXPECTED_RESPONSE_CODE = "Unexpected HTTP response code";

    private Service splunkService = null;
    private Args serviceArgs = null;
    private Consumer<ResponseMessage> errorResponseHandler = (responseMessage -> {
        logWarn(UNEXPECTED_RESPONSE_CODE)
                .expected(200)
                .actual(responseMessage.getStatus())
                .addMessage(parseResponse(responseMessage))
                .log();
    });

    /**
     * Constructs a new SplunkClient Object.
     *
     * @param serviceArgs the parameters required to connect to the Splunk instance.
     */
    public SplunkClient(Args serviceArgs) {
        this.serviceArgs = serviceArgs;
        this.splunkService = Service.connect(serviceArgs);
    }

    /**
     * Send a HTTP Collectiom Event to the Splunk instance.
     *
     * @param uri                  the URI to send the request to.
     * @param splunkRequestMessage the {@link SplunkRequestMessage} to send.
     */
    public Future send(final String uri, final SplunkRequestMessage splunkRequestMessage) {
        return pool.submit(() -> {
            ResponseMessage responseMessage = splunkService.send(uri, splunkRequestMessage);
            if (responseMessage == null || HttpStatus.SC_OK != responseMessage.getStatus()) {
                errorResponseHandler.accept(responseMessage);
            }
        });
    }

    private String parseResponse(ResponseMessage responseMessage) {
        StringWriter message = new StringWriter();
        try {
            IOUtils.copy(responseMessage.getContent(), message);
        } catch (IOException ex) {
            logError(ex).log();
        }
        return message.toString();
    }

    Args getServiceArgs() {
        return serviceArgs;
    }

    void setSplunkService(Service splunkService) {
        this.splunkService = splunkService;
    }

    /**
     * Sets {@link Consumer} accepting {@link ResponseMessage} to handle any Splunk response that does not have a
     * 200 status code.
     */
    void setErrorResponseHandler(Consumer<ResponseMessage> errorResponseHandler) {
        this.errorResponseHandler = errorResponseHandler;
    }
}
