package com.github.onsdigital.zebedee.util.mertics;

import static com.github.onsdigital.zebedee.logging.ZebedeeReaderLogBuilder.logTrace;
import static com.github.onsdigital.zebedee.logging.ZebedeeReaderLogBuilder.logWarn;

public class DummyMetricsServiceImpl extends MetricsService {

    static String MSG_PREFIX = DummyMetricsServiceImpl.class.getSimpleName() + ": ";
    static String REQ_TIME_MSG = "captureRequestTime()";
    static String CAPTURE_ERR_MSG = "captureError()";
    static String CAPTURE_PING_MSG = "capturePing";

    public DummyMetricsServiceImpl() {
        logWarn("No MetricsService configured enabling DummyMetricsServiceImpl").log();
    }

    @Override
    public void captureRequestTime() {
        logTrace(MSG_PREFIX + REQ_TIME_MSG).log();
    }

    @Override
    public void captureError() {
        logTrace(MSG_PREFIX + CAPTURE_ERR_MSG).log();
    }

    @Override
    public void capturePing(long ms) {
        logTrace(MSG_PREFIX + CAPTURE_PING_MSG).log();
    }
}
