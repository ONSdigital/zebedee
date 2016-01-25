package com.github.onsdigital.zebedee.data.processing;

import com.github.onsdigital.zebedee.content.page.statistics.data.timeseries.TimeSeries;
import com.github.onsdigital.zebedee.content.util.ContentUtil;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.reader.ContentReader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * A hashmap storing a mapping from cdid
 */
public class DataIndex {
    Map<String, String> index = new HashMap<>();
    boolean indexBuilt = false;

    public String getUriForCdid(String cdid) {
        return index.get(cdid);
    }
    public void setUriForCdid(String cdid, String uri) {
        index.put(cdid, uri);
    }

    public Set<String> cdids() {
        return index.keySet();
    }

    public void buildIndex(ContentReader contentReader) {
        Runnable build = new Runnable() {
            @Override
            public void run() {
                try {
                    Files.walkFileTree(contentReader.getRootFolder(), new IndexBuilder(index, contentReader));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                indexBuilt = true;
            }
        };
        build.run();
    }

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

            // Quick check
            if (uri.endsWith("data.json") && uri.toString().contains("/timeseries/")) {
                uri = uri.substring(0, uri.length() - "data.json".length());

                TimeSeries timeSeries = null;
                try {
                    timeSeries = (TimeSeries) this.contentReader.getContent(uri);
                    if (timeSeries.getCdid() != null) {
                        this.index.put(timeSeries.getCdid(), "/" + this.contentReader.getRootFolder().relativize(file).toString());
                    }
                } catch (ZebedeeException | IOException e) {
                    e.printStackTrace();
                }

            }
            return FileVisitResult.CONTINUE;
        }
    }

}
