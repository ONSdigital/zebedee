package com.github.onsdigital.zebedee.util.mertics.service.client;

import com.github.onsdigital.zebedee.util.mertics.model.SplunkRequestMessage;
import com.splunk.Args;
import com.splunk.ResponseMessage;
import com.splunk.Service;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.StringWriter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.github.onsdigital.zebedee.logging.ZebedeeReaderLogBuilder.logError;
import static com.github.onsdigital.zebedee.logging.ZebedeeReaderLogBuilder.logWarn;

/**
 * Created by dave on 8/16/16.
 */
public class SplunkClient {

    protected static ExecutorService pool = Executors.newSingleThreadExecutor();
    private static final String UNEXPECTED_RESPONSE_CODE = "Unexpected HTTP response code";

    private Service splunkService = null;
    private Args serviceArgs = null;

    public SplunkClient(Args serviceArgs) {
        this.serviceArgs = serviceArgs;
        this.splunkService = Service.connect(serviceArgs);
    }

    public void send(final String uri, final SplunkRequestMessage splunkRequestMessage) {
        pool.submit(() -> {
            ResponseMessage responseMessage = splunkService.send(uri, splunkRequestMessage);

            if (responseMessage.getStatus() != 200) {
                logWarn(UNEXPECTED_RESPONSE_CODE)
                        .expected(200)
                        .actual(responseMessage.getStatus())
                        .addMessage(parseResponse(responseMessage))
                        .log();
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

    public Args getServiceArgs() {
        return serviceArgs;
    }

    void setSplunkService(Service splunkService) {
        this.splunkService = splunkService;
    }
}
