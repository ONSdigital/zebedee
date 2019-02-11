package com.github.onsdigital.zebedee;

import com.github.onsdigital.logging.v2.DPLogger;
import com.github.onsdigital.logging.v2.config.nop.NopConfig;

public class LoggingTestHelper {

    private LoggingTestHelper() {
    }

    public static void initDPLogger(Class c) {
        DPLogger.init(new NopConfig());
    }
}
