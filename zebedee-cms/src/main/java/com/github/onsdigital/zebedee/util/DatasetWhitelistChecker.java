package com.github.onsdigital.zebedee.util;

import static com.github.onsdigital.zebedee.configuration.Configuration.getDatasetWhitelist;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import com.github.onsdigital.zebedee.configuration.Configuration;

public class DatasetWhitelistChecker {

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

        // Check if the filename is in the whitelist
        return whitelist.contains(filename);
    }

}