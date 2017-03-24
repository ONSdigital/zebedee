package com.github.onsdigital.zebedee.util;

import com.github.davidcarboni.restolino.framework.Startup;
import com.sun.management.UnixOperatingSystemMXBean;

import java.lang.management.ManagementFactory;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.github.onsdigital.zebedee.configuration.Configuration.getFileDescriptorIntervalTime;
import static com.github.onsdigital.zebedee.configuration.Configuration.getFileDescriptorThreshholdLimit;
import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logWarn;

/**
 * Logging / Monitoring for open file descriptors
 */
public class FileDescriptorMonitor implements Startup {

    private static final UnixOperatingSystemMXBean UNIX_OPERATING_SYSTEM_MXBEAN;
    private static final String LOG_MSG = "Open file descriptor statistics";
    private static final String WARN_LOG_MSG = "WARNING Open file descriptors is close to maximum available limit";
    private static Integer WARNING_THRESHHOLD = getFileDescriptorThreshholdLimit();
    private static Integer SCHEDULED_INTERVAL_TIME = getFileDescriptorIntervalTime();
    private static ScheduledExecutorService SCHEDULED_EXECUTOR_SERVICE;

    static {
        UNIX_OPERATING_SYSTEM_MXBEAN = (UnixOperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
    }

    public static FileDescriptorStats getStats() {
        return new FileDescriptorStats(UNIX_OPERATING_SYSTEM_MXBEAN);
    }

    @Override
    public void init() {
        SCHEDULED_EXECUTOR_SERVICE = Executors.newSingleThreadScheduledExecutor();
        SCHEDULED_EXECUTOR_SERVICE.scheduleAtFixedRate(getCommand(getStats()),
                SCHEDULED_INTERVAL_TIME, SCHEDULED_INTERVAL_TIME, TimeUnit.SECONDS);
    }

    private Runnable getCommand(FileDescriptorStats stats) {
        if (stats.getPercentageUsed() >= WARNING_THRESHHOLD) {
            return () -> logWarn(LOG_MSG).fileDescriptorStats(getStats()).log();
        }
        // TODO can hook in alert or something when the warning is triggered.
        return () -> logWarn(WARN_LOG_MSG).fileDescriptorStats(getStats()).log();
    }

}
