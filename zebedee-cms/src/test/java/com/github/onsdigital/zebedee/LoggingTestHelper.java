package com.github.onsdigital.zebedee;

import com.github.onsdigital.logging.v2.DPLogger;
import com.github.onsdigital.logging.v2.config.LoggerConfig;
import com.github.onsdigital.logging.v2.serializer.EventSerialiser;
import org.slf4j.Logger;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LoggingTestHelper {

    private LoggingTestHelper() {
    }

    public static void initDPLogger(Class c) {
        Logger loggerMock = mock(Logger.class);
        when(loggerMock.getName()).thenReturn(c.getName());

        EventSerialiser eventSerialiserMock = mock(EventSerialiser.class);

        LoggerConfig loggerConfigMock = new LoggerConfig(loggerMock, eventSerialiserMock, c.getName());
        DPLogger.init(loggerConfigMock);
    }
}
