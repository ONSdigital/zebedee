package com.github.onsdigital.zebedee.model.publishing.legacycacheapi;

import com.github.onsdigital.zebedee.util.URIUtils;
import org.apache.commons.lang.StringUtils;

import java.util.Arrays;
import java.util.List;

import static com.github.onsdigital.logging.v2.event.SimpleEvent.error;

public class PayloadPathUpdater {
    // Segment input variables
    private static final String BULLETIN_SEGMENT = "/bulletins";
    private static final String ADHOC_SEGMENT = "/adhocs";
    private static final String ARTICLE_SEGMENT = "/articles";
    private static final String DATA_SEGMENT_WITHOUT_SLASH = "data";
    private static final String DATASETS_SEGMENT = "/datasets";
    private static final String LINE_CHART_CONFIG_SEGMENT_WITHOUT_SLASH = "linechartconfig";
    private static final String QMIS_SEGMENT = "/qmis";
    private static final String METHODOLOGY_SEGMENT = "/methodologies";
    private static final String TIME_SERIES_SEGMENT = "/timeseries";
    private static final String VISUALISATIONS_SEGMENT = "/visualisations";

    // Resource types
    private static final List<String> RESOURCE_TYPE_SEGMENTS = Arrays.asList(
            "/chartconfig?",
            "/chartimage?",
            "/embed?",
            "/chart?",
            "/resource?",
            "/generator?",
            "/file?",
            "/export?"
    );

    private PayloadPathUpdater() {}

    public static String getCanonicalPagePath(String incomingUrl, String collectionId) {
        try {
            if (StringUtils.isEmpty(incomingUrl)) {
                return incomingUrl;
            }

            String updatedUri = URIUtils.removeTrailingSlash(incomingUrl.trim());
            if (StringUtils.isEmpty(updatedUri)) {
                return "";
            }

            // Order matters:

            // 1. If starts with /visualisation/ then /visualisation/xxx/xxxx ---> /visualisation/xxx (keep 1 segment) - return here
            if (updatedUri.startsWith(VISUALISATIONS_SEGMENT)) {
                return URIUtils.getNSegmentsAfterSegmentInput(updatedUri, VISUALISATIONS_SEGMENT, 1);
            }

            // 2. If resource then e.g. /embed?uri='/something' ---> /something:
            //      /chartconfig
            //      /chartimage
            //      /embed
            //      /chart
            //      /resource
            //      /generator
            //      /file
            //      /export
            for (String keyword : RESOURCE_TYPE_SEGMENTS) {
                if (updatedUri.startsWith(keyword) && URIUtils.hasQueryParams(updatedUri)) {
                    updatedUri = URIUtils.getQueryParameterFromURL(updatedUri, "uri");
                    break;
                }
            }

            // 3. If bulletin or article then /xxxx/bulletin/xxxx1/xxxx2/xxxx3 --> /bulletin/xxxx1/xxxx2  (keep 2 segment) & RETURN modified
            if (updatedUri.contains(ARTICLE_SEGMENT)) {
                return URIUtils.getNSegmentsAfterSegmentInput(updatedUri, ARTICLE_SEGMENT, 2);
            }
            if (updatedUri.contains(BULLETIN_SEGMENT)) {
                return URIUtils.getNSegmentsAfterSegmentInput(updatedUri, BULLETIN_SEGMENT, 2);
            }
            // 4. If qmis or adhoc or methodologies then /xxxx/qmis/xxx1/xxx2 --> /qmis/xxx1 & RETURNS modified
            if (updatedUri.contains(ADHOC_SEGMENT)) {
                return URIUtils.getNSegmentsAfterSegmentInput(updatedUri, ADHOC_SEGMENT, 1);
            }
            if (updatedUri.contains(QMIS_SEGMENT)) {
                return URIUtils.getNSegmentsAfterSegmentInput(updatedUri, QMIS_SEGMENT, 1);
            }
            if (updatedUri.contains(METHODOLOGY_SEGMENT)) {
                return URIUtils.getNSegmentsAfterSegmentInput(updatedUri, METHODOLOGY_SEGMENT, 1);
            }

            // 5. For the below if (a) is true, skip (b):
            //      (a) If path ends with /data then /xxx1/data/ --> /xxx1
            //      (b) If path ends with filename + extension remove it /xxx/filename.extension --> /xxx
            String lastSegment = URIUtils.getLastSegment(updatedUri);
            if (lastSegment != null) {
                if (lastSegment.equals(DATA_SEGMENT_WITHOUT_SLASH)) {
                    updatedUri = URIUtils.removeLastSegment(updatedUri);
                } else if (URIUtils.isLastSegmentFileExtension(updatedUri)) {
                    updatedUri = URIUtils.removeLastSegment(updatedUri);
                }
            }

            // 6. If timeseries:
            //      (a) then /timeseries/xxx --> /xxx or /timeseries/xxx1/xxx2 --> /xxx1/xxx2 (keep up to 2 segments)
            //      (b) If path ends with /xxx/linechartconfig --> /xxx
            //      (c) RETURN modified
            if (updatedUri.contains(TIME_SERIES_SEGMENT)) {
                updatedUri = URIUtils.getNSegmentsAfterSegmentInput(updatedUri, TIME_SERIES_SEGMENT, 2);
                if (URIUtils.getLastSegment(updatedUri).equals(LINE_CHART_CONFIG_SEGMENT_WITHOUT_SLASH)) {
                    return URIUtils.removeLastSegment(updatedUri);
                }
                return updatedUri;
            }

            // 7. If dataset path then /datasets/xxx --> /xxx or /datasets/xxx1/xxx2/xxx3 --> /xxx1/xxx2 (keep up to 2 segments) & RETURN modified
            if (updatedUri.contains(DATASETS_SEGMENT)) {
                return URIUtils.getNSegmentsAfterSegmentInput(updatedUri, DATASETS_SEGMENT, 2);
            }
            return updatedUri;

        } catch (NullPointerException e) {
            error().data("collectionId", collectionId)
                    .data("incomingUrl", incomingUrl)
                    .logException(e, "failed initialising LegacyCacheApi Payload - failed updating url path for incoming url");
            return null;
        }
    }

    public static boolean isPayloadPathBulletinLatest(String uriToUpdate) {
        return uriToUpdate != null && uriToUpdate.contains(BULLETIN_SEGMENT) && !uriToUpdate.endsWith("/latest");
    }

    public static String getPathForBulletinLatest(String uriToUpdate) {
        if (uriToUpdate.contains(PayloadPathUpdater.BULLETIN_SEGMENT)) {
            return URIUtils.getNSegmentsAfterSegmentInput(uriToUpdate, BULLETIN_SEGMENT, 1) + "/latest";
        }
        return null;
    }
}
