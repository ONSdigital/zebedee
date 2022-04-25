package com.github.onsdigital.zebedee.model;

import com.github.onsdigital.zebedee.json.ContentDetail;
import com.github.onsdigital.zebedee.json.ContentDetailDescription;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

import static com.github.onsdigital.logging.v2.event.SimpleEvent.error;

public class PublishedContent extends Content {
    private Path publishedContentPath;

    /**
     * Create a new instance using an injected publishedContentPath.
     *
     * @param publishedContentPath
     */
    public PublishedContent(Path publishedContentPath) {
        super(publishedContentPath);
        this.publishedContentPath = publishedContentPath;
    }

    /**
     * Returns a list of details with the details of child page details nested.
     *
     * @return
     * @throws IOException
     */
    public ContentDetail nestedDetails() throws IOException {
        return nestedDetails(getPath());
    }

    private ContentDetail nestedDetails(Path contentPath) throws IOException {
        ContentDetail detail = details(contentPath.resolve("data.json"));

        // if the folder is empty put in an empty node with just a name.
        if (detail == null) {
            detail = new ContentDetail();
            detail.description = new ContentDetailDescription(contentPath.getFileName().toString());
            detail.uri = "";
        }

        detail.contentPath = "/" + getPath().relativize(contentPath);
        detail.children = new ArrayList<>();

        // todo: remove timeseries filter once we are caching the browse tree.
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(contentPath)) {
            for (Path entry : stream) {
                if (isVisible(entry)) {
                    ContentDetail child = nestedDetails(entry);
                    if (child != null) {
                        detail.children.add(child);
                    }
                }
            }
        }

        try {
            if (detail.children.size() > 1) {
                java.util.Collections.sort(detail.children, (o1, o2) -> {

                    if ((o1.description == null || o1.description.title == null) && (o2.description == null || o2.description.title == null)) {
                        return 0; // if both are null
                    }
                    if (o1.description == null || o1.description.title == null) {
                        return 1;//nulls last
                    }
                    if (o2.description == null || o2.description.title == null) {
                        return -1;
                    }
                    return o1.description.title.compareTo(o2.description.title);
                });
            }
        } catch (IllegalArgumentException e) {
            error().data("path", contentPath.toString()).logException(e, "Failed to sort content detail items");
        }

        return detail;
    }
}
