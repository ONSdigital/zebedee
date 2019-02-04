package com.github.onsdigital.zebedee;

import com.github.onsdigital.logging.v2.DPLogger;
import com.github.onsdigital.logging.v2.config.Config;
import com.github.onsdigital.logging.v2.config.Builder;
import com.github.onsdigital.logging.v2.serializer.JacksonLogSerialiser;
import org.slf4j.LoggerFactory;

import static org.mockito.Mockito.mock;

public class LoggingTestHelper {

    private LoggingTestHelper() {
    }

    public static void initDPLogger(Class c) {
        DPLogger loggerMock = mock(DPLogger.class);

        Config config = new Builder()
                .logger(LoggerFactory.getLogger("com.zebedee.app"))
                .serialiser(new JacksonLogSerialiser())
                .dataNamespace("zebedee.data")
                .create();

        DPLogger.init(config);
    }
}
