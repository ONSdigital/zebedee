package com.github.onsdigital.zebedee.util;

import com.github.onsdigital.zebedee.content.page.base.PageType;
import com.github.onsdigital.zebedee.content.page.release.Release;
import com.github.onsdigital.zebedee.content.partial.Link;
import com.github.onsdigital.zebedee.json.ContentDetail;
import com.github.onsdigital.zebedee.model.Collection;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;

public class ReleasePopulator {

    /**
     * Add the pages of a collection to a release.
     *
     * @param release
     * @param collection
     * @return
     */
    public static Release populate(Release release, Collection collection) throws IOException {

        for (ContentDetail contentDetail : collection.reviewed.details()) {
            addPageDetailToRelease(release, contentDetail);
        }

        return release;
    }

    private static void addPageDetailToRelease(Release release, ContentDetail contentDetail) {
        if (contentDetail.type.equals(PageType.article.toString())
                || contentDetail.type.equals(PageType.bulletin.toString())) {
            addRelatedDocument(release, contentDetail);
        }

        if (contentDetail.type.equals(PageType.dataset.toString())
                || contentDetail.type.equals(PageType.timeseries_dataset.toString())) {
            addRelatedDataset(release, contentDetail);
        }
    }

    private static void addRelatedDataset(Release release, ContentDetail contentDetail) {
        Link link = createLink(contentDetail);

        if (release.getRelatedDatasets() == null) {
            release.setRelatedDatasets(new ArrayList<Link>());
        }
        release.getRelatedDatasets().add(link);
    }

    private static void addRelatedDocument(Release release, ContentDetail contentDetail) {
        Link link = createLink(contentDetail);
        if (release.getRelatedDocuments() == null) {
            release.setRelatedDocuments(new ArrayList<Link>());
        }
        release.getRelatedDocuments().add(link);
    }

    private static Link createLink(ContentDetail contentDetail) {
        Link link = new Link(URI.create(contentDetail.uri));
        link.setTitle(contentDetail.description.title);
        return link;
    }
}
