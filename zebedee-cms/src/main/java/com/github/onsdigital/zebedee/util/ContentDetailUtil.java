package com.github.onsdigital.zebedee.util;

import com.github.davidcarboni.restolino.json.Serialiser;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.json.ContentDetail;
import com.github.onsdigital.zebedee.model.Content;
import com.github.onsdigital.zebedee.model.content.item.VersionedContentItem;
import com.github.onsdigital.zebedee.reader.ContentReader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Helper class to read content in the ContentDetail format used by florence in the browse tree and collection details
 */
public class ContentDetailUtil {


    public static List<ContentDetail> resolveDetails(Content content, ContentReader reader) throws IOException, ZebedeeException {

        List<ContentDetail> details = new ArrayList<>();

        for (String uri : content.uris("*data*.json")) {
            if (!VersionedContentItem.isVersionedUri(uri)) {
                com.github.onsdigital.zebedee.json.ContentDetail contentDetail = Serialiser.deserialise(reader.getResource(uri).getData(), com.github.onsdigital.zebedee.json.ContentDetail.class);
                details.add(contentDetail);
            }
        }

        return details;
    }
}
