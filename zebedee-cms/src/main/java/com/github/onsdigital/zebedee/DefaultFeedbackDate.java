package com.github.onsdigital.zebedee;

import com.github.davidcarboni.restolino.framework.Startup;
import com.github.onsdigital.zebedee.model.ContentWriter;
import com.github.onsdigital.zebedee.reader.ZebedeeReader;

import java.nio.file.Path;
import java.nio.file.Paths;

import static com.github.onsdigital.zebedee.content.page.feedback.FeedbackPage.feedbackDefault;
import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logDebug;

/**
 *
 */
public class DefaultFeedbackDate implements Startup {

    static final String FEEDBACK_URI = "/feedback";
    static final String DATA_JSON = "/data.json";
    static final String ZEB_ROOT = "zebedee_root";
    static final String MASTER = "master";
    static final String FEEDBACK_JSON_EXISTS_MSG = "Feedback json file exists no action required.";
    static final String FEEDBACK_JSON_MISSING_MSG = "No Feedback json file exists. A default will be created.";

    @Override
    public void init() {
        if (!feedbackExists()) {
            try {
                Path feedbackPath = Paths.get(System.getenv(ZEB_ROOT))
                        .resolve("zebedee")
                        .resolve(MASTER)
                        .resolve("feedback");

                new ContentWriter(feedbackPath).writeObject(feedbackDefault(), DATA_JSON);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    private boolean feedbackExists() {
        try {
            new ZebedeeReader().getPublishedContent(FEEDBACK_URI);
            logDebug(FEEDBACK_JSON_EXISTS_MSG).log();
        } catch (Exception ex) {
            logDebug(FEEDBACK_JSON_MISSING_MSG).log();
            return false;
        }
        return true;
    }
}
