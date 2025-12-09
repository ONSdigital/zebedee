package com.github.onsdigital.zebedee;

import ch.qos.logback.classic.LoggerContext;
import com.github.davidcarboni.restolino.framework.Priority;
import com.github.davidcarboni.restolino.framework.Startup;
import com.github.onsdigital.logging.v2.DPLogger;
import com.github.onsdigital.logging.v2.Logger;
import com.github.onsdigital.logging.v2.LoggerImpl;
import com.github.onsdigital.logging.v2.LoggingException;
import com.github.onsdigital.logging.v2.config.Builder;
import com.github.onsdigital.logging.v2.serializer.JacksonLogSerialiser;
import com.github.onsdigital.logging.v2.serializer.LogSerialiser;
import com.github.onsdigital.logging.v2.storage.LogStore;
import com.github.onsdigital.logging.v2.storage.MDCLogStore;
import com.github.onsdigital.zebedee.reader.configuration.ReaderConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.LoggerFactory;

import static com.github.onsdigital.zebedee.logging.ReaderLogger.info;

@Priority(1)
public class ReaderInit implements Startup {

    private static final String FORMAT_LOGS_KEY = "FORMAT_LOGGING";
    private static final String DEFAULT_LOGGER_NAME_KEY = "default.logger.name";
    private static final String READER_LOGGER_NAME = "zebedee";

    @Override
    public void init() {
        if (initReaderLogger()) {
            LogSerialiser serialiser = getLogSerialiser();
            LogStore store = new MDCLogStore(serialiser);
            Logger logger = new LoggerImpl(READER_LOGGER_NAME);

            try {
                DPLogger.init(new Builder()
                        .serialiser(serialiser)
                        .logStore(store)
                        .logger(logger)
                        .create());
            } catch (LoggingException ex) {
                System.err.println(ex);
                System.exit(1);
            }
        }

        info().log("initialising Zebedee Reader");

        info().log("loading zebedee reader configuration");
        ReaderConfiguration.get();

        info().log("Zebedee Reader initialised");
    }

    private boolean initReaderLogger() {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        String defaultName = loggerContext.getProperty(DEFAULT_LOGGER_NAME_KEY);
        String loggerName = DPLogger.logConfig().getLogger().getName();
        return StringUtils.equals(defaultName, loggerName);
    }

    private LogSerialiser getLogSerialiser() {
        boolean formatLogging = Boolean.valueOf(System.getenv(FORMAT_LOGS_KEY));
        return new JacksonLogSerialiser(formatLogging);
    }
}
