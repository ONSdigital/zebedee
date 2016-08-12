package com.github.onsdigital.zebedee.util.mertics;

import com.github.davidcarboni.restolino.framework.HttpMethod;
import com.github.onsdigital.zebedee.util.mertics.model.PingEvent;
import com.github.onsdigital.zebedee.util.mertics.model.RequestMetrics;
import com.github.onsdigital.zebedee.util.mertics.model.SplunkEvent;
import com.github.onsdigital.zebedee.util.mertics.model.SplunkRequest;
import com.splunk.Args;
import com.splunk.RequestMessage;
import com.splunk.ResponseMessage;
import com.splunk.Service;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.StringWriter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.github.onsdigital.zebedee.Configuration.SplunkConfiguration.getEventsCollectionURI;
import static com.github.onsdigital.zebedee.logging.ZebedeeReaderLogBuilder.logError;
import static com.github.onsdigital.zebedee.logging.ZebedeeReaderLogBuilder.logInfo;
import static com.github.onsdigital.zebedee.logging.ZebedeeReaderLogBuilder.logWarn;


public class SplunkMetricsServiceImpl extends MetricsService {

    protected static ExecutorService pool = Executors.newSingleThreadExecutor();
    private Service splunkService = null;

    private static final String UNEXPECTED_RESPONSE_CODE = "Unexpected HTTP response code";

    public SplunkMetricsServiceImpl(Args serviceArgs) {
        logInfo("Splunk MetricsService enabled configuring...").log();
        this.splunkService = Service.connect(serviceArgs);

        logInfo("Splunk MetricsService successfully configured").log();
    }

    public void send(SplunkEvent event) {
        RequestMessage requestMessage = new RequestMessage(HttpMethod.POST.name());
        requestMessage.setContent(new SplunkRequest(event).toJson());

        pool.submit(() -> {
            ResponseMessage responseMessage = splunkService.send(getEventsCollectionURI(), requestMessage);
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

    @Override
    public void captureRequestTime() {
        RequestMetrics event = SplunkEvent.getRequestMetrics();
        event.stopTimer();
        event.setStatsType(SplunkEvent.StatsType.REQUEST_TIME);
        send(event);
    }

    @Override
    public void captureError() {
        RequestMetrics event = SplunkEvent.getRequestMetrics();
        event.stopTimer();
        event.setStatsType(SplunkEvent.StatsType.REQUEST_ERROR);
        send(event);
    }

    @Override
    public void capturePing(long ms) {
        send(new PingEvent(ms));
    }
}
