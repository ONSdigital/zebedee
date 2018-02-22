package com.github.onsdigital.zebedee.data.processing;

import com.github.onsdigital.zebedee.content.page.statistics.data.timeseries.TimeSeries;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.model.content.item.VersionedContentItem;
import com.github.onsdigital.zebedee.reader.ContentReader;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logError;
import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logInfo;

/**
 * A hashmap storing an entry for each timeseries - mapping the CDID to the url of the timeseries.
 */
public class DataIndex {
    private static final ExecutorService pool = Executors.newSingleThreadExecutor();
    Map<String, String> index = new HashMap<>();
    ContentReader contentReader = null;
    boolean indexBuilt = false;

    /**
     * Set up the data index based on a content reader
     *
     * @param contentReader any content reader
     */
    public DataIndex(ContentReader contentReader) {
        this.contentReader = contentReader;
        reindex();
    }

    public DataIndex() {
    }

    public String getUriForCdid(String cdid) {
        return index.get(cdid);
    }

    public void setUriForCdid(String cdid, String uri) {
        index.put(cdid, uri);
    }

    public Set<String> cdids() {
        return index.keySet();
    }

    /**
     * Build the data index
     */
    public void reindex() {
        indexBuilt = false;
        Runnable build = () -> {
            logInfo("Start building data index.").log();
            long startTime = System.nanoTime();
            try {
                Files.walkFileTree(contentReader.getRootFolder(), new IndexBuilder(index, contentReader));
            } catch (IOException e) {
                logError(e, "Failed to build data index").log();
            }
            long duration = System.nanoTime() - startTime;
            logInfo("Finished building data index.").addParameter("entries", index.size()).addParameter("duration_ns", duration).log();
            indexBuilt = true;
        };
        pool.submit(build);
    }

    /**
     * Wait until complete with return value
     *
     * @param maxSeconds max wait time
     * @return build is completed
     * @throws InterruptedException
     */
    private boolean waitWhileIncomplete(int maxSeconds) throws InterruptedException {
        int tries = 0;
        while (!indexBuilt) {
            Thread.sleep(100);
            if (tries++ > 10 * maxSeconds)
                return false;
        }
        return true;
    }

    /**
     * Wait until data indexing is complete
     *
     * @param maxSeconds timeout in seconds before an error is thrown
     * @throws BadRequestException if the pause takes too long or is interupted
     */
    public void pauseUntilComplete(int maxSeconds) throws BadRequestException {
        try {
            if (!waitWhileIncomplete(maxSeconds)) {
                throw new BadRequestException("DataIndex build in progress");
            }
        } catch (InterruptedException e) {
            throw new BadRequestException("DataIndex build in progress");
        }
    }

    public boolean isIndexBuilt() {
        return indexBuilt;
    }

    /**
     * Inner class for walking the file tree
     */
    private static class IndexBuilder extends SimpleFileVisitor<Path> {

        Map<String, String> index = new HashMap<>();
        ContentReader contentReader = null;

        public IndexBuilder(Map<String, String> index, ContentReader contentReader) {
            this.index = index;
            this.contentReader = contentReader;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attr) {
            // Get the uri
            String uri = "/" + this.contentReader.getRootFolder().relativize(file).toString();

            // Check json files in timeseries directories (excluding versions)
            if (uri.endsWith("data.json") && uri.contains("/timeseries/") && !uri.contains("/" + VersionedContentItem.getVersionDirectoryName() + "/")) {
                uri = uri.substring(0, uri.length() - "/data.json".length());

                TimeSeries timeSeries;
                try {
                    timeSeries = (TimeSeries) this.contentReader.getContent(uri);
                    if (timeSeries.getCdid() != null) {

                        // get the parent path so that we are referencing the timeseries landing page instead of dataset specific timeseries.
                        String timeseriesLandingPageUri = uri; //if the parent directory is the timeseries folder just use the uri.
                        Path path = Paths.get(uri);

                        if (!path.getParent().getFileName().toString().equals("timeseries")) {
                            timeseriesLandingPageUri = path.getParent().toString(); // else use the parent CDID based directory.
                        }

                        this.index.put(timeSeries.getCdid().toLowerCase(), timeseriesLandingPageUri);
                    }
                } catch (Exception e) {
                    logError(e, "Error indexing uri").addParameter("uri", uri).log();
                }

            }
            return FileVisitResult.CONTINUE;
        }
    }
}
