package com.github.onsdigital.zebedee.data.processing;

import com.github.onsdigital.zebedee.content.page.statistics.data.timeseries.TimeSeries;
import com.github.onsdigital.zebedee.content.util.ContentUtil;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.model.content.item.VersionedContentItem;
import com.github.onsdigital.zebedee.reader.ContentReader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A hashmap storing a mapping from cdid
 */
public class DataIndex {
    Map<String, String> index = new HashMap<>();
    ContentReader contentReader = null;
    boolean indexBuilt = false;
    private static final ExecutorService pool = Executors.newSingleThreadExecutor();

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
     * Set up the data index based on a content reader
     *
     * @param contentReader any content reader
     */
    public DataIndex(ContentReader contentReader) {
        this.contentReader = contentReader;
        reindex();
    }

    /**
     * Build the data index
     */
    public void reindex() {
        indexBuilt = false;
        Runnable build = new Runnable() {
            @Override
            public void run() {
                try {
                    Files.walkFileTree(contentReader.getRootFolder(), new IndexBuilder(index, contentReader));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                System.out.println("Data index built with " + index.size() + " entries");
                indexBuilt = true;
            }
        };
        pool.submit(build);
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
            if (uri.endsWith("data.json") && uri.toString().contains("/timeseries/") && !uri.toString().contains("/" + VersionedContentItem.getVersionDirectoryName() + "/")) {
                uri = uri.substring(0, uri.length() - "/data.json".length());

                TimeSeries timeSeries = null;
                try {
                    timeSeries = (TimeSeries) this.contentReader.getContent(uri);
                    if (timeSeries.getCdid() != null) {

                        this.index.put(timeSeries.getCdid().toLowerCase(), uri);
                    }
                } catch (Exception e) {
                    System.out.println("Error indexing " + uri);
                }

            }
            return FileVisitResult.CONTINUE;
        }
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
}
