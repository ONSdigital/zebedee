package com.github.onsdigital.zebedee.reader.configuration;

import com.github.onsdigital.zebedee.util.VariableUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * Created by bren on 29/07/15.
 * <p>
 * Content reader configuration
 */
public class ReaderConfiguration {
    private final static String ZEBEDEE_ROOT =  "ZEBEDEE_ROOT";
    private static final String DEFAULT_ZEBEDEE_ROOT = "zebedee-reader/target/zebedee";

    private static final String IN_PROGRESS_FOLDER_NAME = "inprogress";
    private static final String COMPLETE_FOLDER_NAME = "complete";
    private static final String REVIEWED_FOLDER_NAME = "reviewed";

    public static String getZebedeeRoot() {
        return StringUtils.defaultIfBlank(VariableUtils.getVariableValue(ZEBEDEE_ROOT), DEFAULT_ZEBEDEE_ROOT);
    }

    public static String getInProgressFolderName() {
        return IN_PROGRESS_FOLDER_NAME;
    }

    public static String getCompleteFolderName() {
        return COMPLETE_FOLDER_NAME;
    }

    public static String getReviewedFolderName() {
        return REVIEWED_FOLDER_NAME;
    }
}
