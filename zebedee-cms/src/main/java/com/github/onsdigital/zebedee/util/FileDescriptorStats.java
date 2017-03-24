package com.github.onsdigital.zebedee.util;

import com.sun.management.UnixOperatingSystemMXBean;

import java.text.DecimalFormat;

/**
 * POJO to hold open File Descriptor statistics.
 */
public class FileDescriptorStats {

    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("###.##");

    private Double openCount;
    private Double maxAvailable;
    private Double percentageUsed;

    public FileDescriptorStats(UnixOperatingSystemMXBean unixOS) {
        this.openCount = new Double(unixOS.getOpenFileDescriptorCount());
        this.maxAvailable = new Double(unixOS.getMaxFileDescriptorCount());

        Double percent = (openCount / maxAvailable) * 100;
        this.percentageUsed = Double.valueOf(DECIMAL_FORMAT.format(percent));
    }

    public Double getOpenCount() {
        return openCount;
    }

    public Double getMaxAvailable() {
        return maxAvailable;
    }

    public Double getPercentageUsed() {
        return percentageUsed;
    }
}
