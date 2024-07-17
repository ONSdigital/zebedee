package com.github.onsdigital.zebedee.util;

import static com.github.onsdigital.zebedee.configuration.Configuration.getDatasetWhitelist;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class DatasetWhitelistChecker {

    //drsi,mm23,mm22,ppi,dataset1,pusf,a01,x09,cla01,pn2,mgdp,diop,ios1,mret,mq10

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

        String baseFilename = filename.replaceAll("(?i)(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)20[2-9][4-9]", "");

        return whitelist.contains(baseFilename);
    }
}
