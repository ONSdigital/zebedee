package com.github.onsdigital.zebedee.util;

import static com.github.onsdigital.zebedee.configuration.Configuration.getDatasetWhitelist;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class DatasetWhitelistChecker {
    public static final String REG_EX_STR = "(?i)(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)20[2-9][0-9]";

    public static Set<String> getWhitelistSet() {

        //Retrieve the whitelist from zebedee configuration
        String dataSetWhitelist = getDatasetWhitelist();
        return Arrays.stream(dataSetWhitelist.split(","))
                .collect(Collectors.toSet());
    }

    public static boolean isWhitelisted(String filename) {
        Set<String> whitelist = getWhitelistSet();

        // Remove the 'upload-' prefix
        if (filename.startsWith("upload-")) {
            filename = filename.substring(7);
        }
        // Remove the file extension if present
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex != -1) {
            filename = filename.substring(0, dotIndex);
        }

        String baseFilename = filename.replaceAll(REG_EX_STR,"");

        return whitelist.contains(baseFilename);
    }
}
