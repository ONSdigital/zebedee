package com.github.onsdigital.zebedee.util.mertics;

import static com.github.onsdigital.zebedee.Configuration.SplunkConfiguration.getServiceArgs;
import static com.github.onsdigital.zebedee.Configuration.SplunkConfiguration.isSplunkEnabled;


public abstract class MetricsService {

    protected static MetricsService service = null;

    public static MetricsService getInstance() {
        if (service == null) {
            if (isSplunkEnabled()) {
                service = new SplunkMetricsServiceImpl(getServiceArgs());
            } else {
                service = new DummyMetricsServiceImpl();
            }
        }
        return service;
    }

    public abstract void captureRequestTime();

    public abstract void captureError();

    public abstract void capturePing(long ms);
}
