package com.github.onsdigital.zebedee.search.indexing.content;

import com.github.onsdigital.zebedee.search.indexing.Department;
import com.github.onsdigital.zebedee.search.indexing.Indexer;
import org.apache.commons.io.IOUtils;
import org.elasticsearch.common.settings.Settings;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.github.onsdigital.zebedee.logging.ZebedeeReaderLogBuilder.elasticSearchLog;
import static com.github.onsdigital.zebedee.logging.ZebedeeReaderLogBuilder.logInfo;

public abstract class ZebedeeContentIndexer {

    protected static final String DEPARTMENT_TYPE = "departments";
    private final static String DEPARTMENTS_FILE = "/search/departments/departments.txt";

    /**
     * Loads the departments file
     * @return
     */
    protected List<Department> loadDepartments() throws IOException {
        List<Department> departments;

        // Use a stream and filter on non-null departments
        try (Stream<String> stream = Files.lines(Paths.get(DEPARTMENTS_FILE))) {
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
        }

        return departments;
    }

    public abstract void indexDepartments();

    public abstract void indexOnsContent();

    /**
     * Loads the index-config.yml file from disk
     * @return
     */
    public static Settings getSettings() {
        Settings.Builder settingsBuilder = Settings.builder().
                loadFromStream("index-config.yml", Indexer.class.getResourceAsStream("/search/index-config.yml"));
        elasticSearchLog("Index settings").addParameter("settings", settingsBuilder.internalMap());
        return settingsBuilder.build();
    }

    /**
     * Loads settings for departments index from departments-index-config.yml file
     * @return
     */
    public static Settings getDepartmentsSettings() {
        Settings.Builder settingsBuilder = Settings.builder().
                loadFromStream("departments-index-config.yml", Indexer.class.getResourceAsStream("/search/departments/departments-index-config.yml"));
        return settingsBuilder.build();
    }

    /**
     * Load the default ONS index mapping
     * @return
     */
    public static String getDefaultMapping() throws IOException {
        InputStream mappingSourceStream = Indexer.class.getResourceAsStream("/search/default-mapping.json");
        String mappingSource = IOUtils.toString(mappingSourceStream);
        elasticSearchLog("defaultMapping").addParameter("mappingSource", mappingSource).log();
        return mappingSource;
    }

    /**
     * Load the departments index mapping
     * @return
     */
    public static String getDepartmentsMapping() throws IOException {
        InputStream mappingSourceStream = Indexer.class.getResourceAsStream("/search/departments/departments-mapping.json");
        String mappingSource = IOUtils.toString(mappingSourceStream);
        elasticSearchLog("departmentsMapping").addParameter("mappingSource", mappingSource).log();
        return mappingSource;
    }

}
