package com.github.onsdigital.zebedee.util;

import com.github.onsdigital.zebedee.content.base.ContentLanguage;
import com.github.onsdigital.zebedee.content.page.base.Page;
import com.github.onsdigital.zebedee.content.page.statistics.document.figure.chart.Chart;
import com.github.onsdigital.zebedee.content.page.statistics.document.figure.image.Image;
import com.github.onsdigital.zebedee.content.page.statistics.document.figure.table.Table;
import com.github.onsdigital.zebedee.content.util.ContentUtil;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.json.ContentDetail;
import com.github.onsdigital.zebedee.model.Content;
import com.github.onsdigital.zebedee.model.content.item.VersionedContentItem;
import com.github.onsdigital.zebedee.reader.ContentReader;
import com.github.onsdigital.zebedee.reader.Resource;

import java.io.IOException;
import java.net.URI;
import java.util.HashSet;
import java.util.Set;

import static com.github.onsdigital.logging.v2.event.SimpleEvent.error;
import static com.github.onsdigital.zebedee.util.URIUtils.removeLastSegment;
import static org.apache.commons.lang3.StringUtils.removeEnd;

/**
 * Helper class to read content in the ContentDetail format used by florence in the browse tree and collection details
 */
public class ContentDetailUtil {


    public static Set<ContentDetail> resolveDetails(Content content, ContentReader reader) throws IOException, ZebedeeException {

        Set<ContentDetail> details = new HashSet<>();

        for (String uri : content.uris("*data*.json")) {
            if (!VersionedContentItem.isVersionedUri(uri)) {

                Page page = null;
                try (Resource resource = reader.getResource(uri)) {
                    try {
                        page = ContentUtil.deserialiseContent(resource.getData());

                        String pageUri = resource.getUri().toString();
                        page.setUri(resolveUri(pageUri, page));
                    } catch (Exception e) {
                        error().data("resourceUri", resource.getUri()).logException(e, "Failed to deserialise json");
                    }
                }

                if (page != null) { //Contents without type is null when deserialised. There should not be no such data
                    ContentDetail contentDetail = new ContentDetail(page.getDescription().getTitle(), page.getUri().toString(), page.getType());
                    contentDetail.setContentPath(page.getUri().toString());
                    contentDetail.getDescription().setEdition(page.getDescription().getEdition());
                    ContentLanguage lang = ContentLanguage.getById(page.getDescription().getLanguage()).orElse(ContentLanguage.ENGLISH);
                    contentDetail.getDescription().setLanguage(lang);
                    details.add(contentDetail);
                }
            }
        }

        return details;
    }

    private static URI resolveUri(String uriString, Page page) {
        URI uri;
        if (page instanceof Table || page instanceof Chart || page instanceof Image) {
            uri = URI.create(removeEnd(uriString, ".json"));
        } else {
            uri = URI.create(removeLastSegment(uriString));
        }
        return uri;
    }
}
