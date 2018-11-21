package com.github.onsdigital.zebedee.search.indexing.content;

import com.github.onsdigital.zebedee.content.page.base.Page;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.reader.ZebedeeReader;
import com.github.onsdigital.zebedee.search.configuration.SearchConfiguration;
import com.github.onsdigital.zebedee.search.indexing.Department;
import com.github.onsdigital.zebedee.search.indexing.FileScanner;
import com.github.onsdigital.zebedee.search.indexing.IndexingException;
import com.github.onsdigital.zebedee.search.indexing.SearchBoostTermsResolver;
import org.apache.commons.io.IOUtils;
import org.elasticsearch.common.settings.Settings;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.github.onsdigital.zebedee.logging.ZebedeeReaderLogBuilder.*;

public abstract class ZebedeeContentIndexer {

    private final static String DEPARTMENTS_FILE = "/search/departments/departments.txt";

    private final FileScanner fileScanner;
    private final ZebedeeReader zebedeeReader;

    public ZebedeeContentIndexer() {
        this.fileScanner = new FileScanner();
        this.zebedeeReader = new ZebedeeReader();
    }

    public void reindex() {
        this.indexDepartments();
        this.indexOnsContent();
    }

    public abstract void indexDepartments();

    public abstract void indexOnsContent();

    public abstract void indexByUri(String uri);

    protected final Page loadPageByUri(String uri) throws ZebedeeException, IOException {
        Page page = this.zebedeeReader.getPublishedContent(uri);
        if (null != page) {
            List<String> terms = resolveSearchTerms(uri);
            page.setSearchTerms(terms);
        }
        return page;
    }

    protected final List<Page> loadPages() throws IOException {
        return this.loadPages(null);
    }

    /**
     * Load pages from disk
     * @return
     */
    protected final List<Page> loadPages(String uri) throws IOException {
        return this.fileScanner.scan(uri).stream()
                .map(document -> {
                    try {
                        return this.loadPageByUri(document.getUri());
                    } catch (ZebedeeException | IOException e) {
                        logError(e)
                                .addMessage("Failed getting published content for uri")
                                .addParameter("uri", document.getUri())
                                .log();
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Loads the departments file
     * @return
     */
    protected final List<Department> loadDepartments() throws IOException {
        List<Department> departments;

        // Use a stream and filter on non-null departments
        Path path = null;
        try {
            path = Paths.get(this.getClass().getResource(DEPARTMENTS_FILE).toURI());
        } catch (URISyntaxException e) {
            String message = "Failed to load departments file";
            logError(e)
                    .addMessage(message)
                    .addParameter("filename", DEPARTMENTS_FILE)
                    .log();
            throw new IndexingException(message, e);
        }

        try (Stream<String> stream = Files.lines(path, Charset.defaultCharset())) {
            departments = stream
                    .map(line -> {
                        String[] split = line.split(" *=> *");
                        if (split.length != 4) {
                            logInfo("Skipping invalid external department")
                                    .addParameter("line", line)
                                    .log();
                            return null;
                        }
                        String[] terms = split[3].split(" *, *");
                        if (terms.length == 0) {
                            logInfo("Skipping invalid external department")
                                    .addParameter("line", line)
                                    .log();
                            return null;
                        }

                        return new Department(split[0], split[1], split[2], terms);
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            return departments;
        }
    }

    /**
     * Generates a new index name with time stamp
     * @return
     */
    protected String getOnsIndexName() {
        return SearchConfiguration.getSearchAlias() + System.currentTimeMillis();
    }

    private static List<String> resolveSearchTerms(String uri) {
        if (null == uri) {
            return null;
        }

        logDebug("Resolving terms for uri")
                .addParameter("uri", uri)
                .log();

        SearchBoostTermsResolver resolver = SearchBoostTermsResolver.getSearchTermResolver();
        List<String> terms = resolver.getTerms(uri);

        String[] segments = uri.split("/");
        for (String segment : segments) {
            String documentUri = "/" + segment;
            List<String> prefixTerms = resolver.getTermsForPrefix(documentUri);
            if (null != prefixTerms) {
                terms.addAll(prefixTerms);
            }
        }
        return terms;
    }

    /**
     * Loads the index-config.yml file from disk
     * @return
     */
    public static Settings getSettings() throws IOException {
        try (InputStream inputStream = ZebedeeContentIndexer.class.getResourceAsStream("/search/index-config.yml")) {
            Settings.Builder settingsBuilder = Settings.builder().
                    loadFromStream("index-config.yml", inputStream);
            elasticSearchLog("Index settings").addParameter("settings", settingsBuilder.internalMap());
            return settingsBuilder.build();
        }
    }

    /**
     * Loads settings for departments index from departments-index-config.yml file
     * @return
     */
    public static Settings getDepartmentsSettings() throws IOException {
        try (InputStream inputStream = ZebedeeContentIndexer.class.getResourceAsStream("/search/departments/departments-index-config.yml")) {
            Settings.Builder settingsBuilder = Settings.builder().
                    loadFromStream("departments-index-config.yml", inputStream);
            return settingsBuilder.build();
        }
    }

    /**
     * Load the default ONS index mapping
     * @return
     */
    public static String getDefaultMapping() throws IOException {
        try (InputStream mappingSourceStream = ZebedeeContentIndexer.class.getResourceAsStream("/search/default-mapping.json")) {
            String mappingSource = IOUtils.toString(mappingSourceStream);
            elasticSearchLog("defaultMapping").addParameter("mappingSource", mappingSource).log();
            return mappingSource;
        }
    }

    /**
     * Load the departments index mapping
     * @return
     */
    public static String getDepartmentsMapping() throws IOException {
        try (InputStream mappingSourceStream = ZebedeeContentIndexer.class.getResourceAsStream("/search/departments/departments-mapping.json")) {
            String mappingSource = IOUtils.toString(mappingSourceStream);
            elasticSearchLog("departmentsMapping").addParameter("mappingSource", mappingSource).log();
            return mappingSource;
        }
    }

}
