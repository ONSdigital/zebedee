package com.github.onsdigital.zebedee.model.approval.tasks.timeseries;

/**
 * track the verification of compressed timeseries.
 */
public class TimeseriesCompressionResult {

    public String path;
    public int numberOfFiles;

    public TimeseriesCompressionResult(String path, int numberOfFiles) {
        this.path = path;
        this.numberOfFiles = numberOfFiles;
    }
}
