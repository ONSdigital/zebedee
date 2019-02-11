package com.github.onsdigital.zebedee.reader;

import com.github.onsdigital.logging.v2.DPLogger;
import com.github.onsdigital.logging.v2.config.nop.NopConfig;

public class LoggingTestHelper {

    private LoggingTestHelper() {
    }

    public static void initDPLogger() {
        DPLogger.init(new NopConfig());
    }
}
