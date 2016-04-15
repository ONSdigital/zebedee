package com.github.onsdigital.zebedee.model.approval;

import com.github.onsdigital.zebedee.content.page.base.PageType;
import com.github.onsdigital.zebedee.content.page.release.Release;
import com.github.onsdigital.zebedee.content.partial.Link;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.json.ContentDetail;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.model.CollectionWriter;
import com.github.onsdigital.zebedee.reader.CollectionReader;
import com.github.onsdigital.zebedee.util.Log;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class ReleasePopulator {



    /**
     * Add the pages of a collection to a release.
     *
     * @param release
     * @return
     */
    public static Release populate(Release release, List<ContentDetail> collectionContent) throws IOException, ZebedeeException {

        release.setRelatedDatasets(new ArrayList<>());
        release.setRelatedDocuments(new ArrayList<>());
        release.setRelatedMethodology(new ArrayList<>());
        release.setRelatedMethodologyArticle(new ArrayList<>());

        for (ContentDetail contentDetail : collectionContent) {
            addPageDetailToRelease(release, contentDetail);
        }

        return release;
    }

    private static void addPageDetailToRelease(Release release, ContentDetail contentDetail) {
        if (contentDetail.type.equals(PageType.article.toString())
                || contentDetail.type.equals(PageType.article_download.toString())
                || contentDetail.type.equals(PageType.bulletin.toString())
                || contentDetail.type.equals(PageType.compendium_landing_page.toString())) {
            Log.print("Adding document: %s as a link to release %s", contentDetail.description.title, release.getDescription().getTitle());
            addRelatedDocument(release, contentDetail);
        }

        if (contentDetail.type.equals(PageType.dataset_landing_page.toString())) {
            Log.print("Adding dataset: %s as a link to release %s", contentDetail.description.title, release.getDescription().getTitle());
            addRelatedDataset(release, contentDetail);
        }

        if (contentDetail.type.equals(PageType.static_qmi.toString())) {
            Log.print("Adding qmi: %s as a link to release %s", contentDetail.description.title, release.getDescription().getTitle());
            addRelatedQMI(release, contentDetail);
        }
        if (contentDetail.type.equals(PageType.static_methodology.toString())
                || contentDetail.type.equals(PageType.static_methodology_download.toString())) {
            Log.print("Adding methodology article: %s as a link to release %s", contentDetail.description.title, release.getDescription().getTitle());
            addRelatedMethodologyArticle(release, contentDetail);
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

    private static void addRelatedMethodologyArticle(Release release, ContentDetail contentDetail) {
        Link link = createLink(contentDetail);
        if (release.getRelatedMethodologyArticle() == null) {
            release.setRelatedMethodologyArticle(new ArrayList<Link>());
        }
        release.getRelatedMethodologyArticle().add(link);
    }

    private static void addRelatedQMI(Release release, ContentDetail contentDetail) {
        Link link = createLink(contentDetail);
        if (release.getRelatedMethodology() == null) {
            release.setRelatedMethodology(new ArrayList<Link>());
        }
        release.getRelatedMethodology().add(link);
    }

    private static Link createLink(ContentDetail contentDetail) {
        Link link = new Link(URI.create(contentDetail.uri));
        link.setTitle(contentDetail.description.title);
        return link;
    }

    public static void populateQuietly(Collection collection,
                                       CollectionReader collectionReader,
                                       CollectionWriter collectionWriter,
                                       List<ContentDetail> collectionContent) throws IOException {
        if (collection.isRelease()) {
            Log.print("Release identified for collection %s, populating the page links...", collection.description.name);
            try {
                collection.populateRelease(collectionReader, collectionWriter, collectionContent);
            } catch (ZebedeeException e) {
                Log.print(e, "Failed to populate release page for collection %s", collection.description.name);
            }
        }
    }
}
