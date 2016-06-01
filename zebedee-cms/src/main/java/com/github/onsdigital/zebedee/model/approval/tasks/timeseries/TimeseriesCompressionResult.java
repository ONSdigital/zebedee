package com.github.onsdigital.zebedee.model.approval.tasks.timeseries;

import java.nio.file.Path;

/**
 * track the verification of compressed timeseries.
 */
public class TimeseriesCompressionResult {

    public Path sourcePath;
    public Path zipPath;
    public int numberOfFiles;

    public TimeseriesCompressionResult(Path sourcePath, Path zipPath, int numberOfFiles) {
        this.sourcePath = sourcePath;
        this.zipPath = zipPath;
        this.numberOfFiles = numberOfFiles;
    }
}
