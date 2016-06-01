package com.github.onsdigital.zebedee.model.approval.task.timeseries;

import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.model.ContentWriter;
import com.github.onsdigital.zebedee.model.approval.tasks.timeseries.TimeSeriesCompressor;
import com.github.onsdigital.zebedee.model.approval.tasks.timeseries.TimeseriesCompressionResult;
import com.github.onsdigital.zebedee.reader.ContentReader;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class MockTimeSeriesCompressor extends TimeSeriesCompressor {

    List<TimeseriesCompressionResult> results;

    public MockTimeSeriesCompressor(List<TimeseriesCompressionResult> results) {
        this.results = results;
    }

    @Override
    public List<TimeseriesCompressionResult> compressFiles(ContentReader contentReader, ContentWriter contentWriter, boolean isEncrypted) throws ZebedeeException, IOException {
        return super.compressFiles(contentReader, contentWriter, isEncrypted);
    }

    @Override
    public int compressFile(ContentReader contentReader, ContentWriter contentWriter, boolean isEncrypted, Path timeSeriesDirectory, String saveUri) throws IOException, ZebedeeException {
        return super.compressFile(contentReader, contentWriter, isEncrypted, timeSeriesDirectory, saveUri);
    }
}
