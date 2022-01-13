package com.github.onsdigital.zebedee.model.approval.tasks;

import com.github.onsdigital.zebedee.content.page.base.PageType;
import com.github.onsdigital.zebedee.content.page.release.Release;
import com.github.onsdigital.zebedee.content.partial.Link;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.json.ContentDetail;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.model.CollectionWriter;
import com.github.onsdigital.zebedee.reader.CollectionReader;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;

import static com.github.onsdigital.logging.v2.event.SimpleEvent.error;
import static com.github.onsdigital.logging.v2.event.SimpleEvent.info;

public class ReleasePopulator {


    /**
     * Add the pages of a collection to a release.
     *
     * @param release
     * @return
     */
    public static Release populate(Release release, Iterable<ContentDetail> collectionContent) throws IOException, ZebedeeException {

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

        if (contentDetail.type.equals(PageType.ARTICLE.toString())
                || contentDetail.type.equals(PageType.ARTICLE_DOWNLOAD.toString())
                || contentDetail.type.equals(PageType.BULLETIN.toString())
                || contentDetail.type.equals(PageType.COMPENDIUM_LANDING_PAGE.toString())) {

            info().data("contentTitle", contentDetail.description.title).data("releaseTitle", release.getDescription().getTitle())
                    .log("Adding document as a link to release");

            addRelatedDocument(release, contentDetail);
        }

        if (contentDetail.type.equals(PageType.DATASET_LANDING_PAGE.toString())
                || contentDetail.type.equals(PageType.API_DATASET_LANDING_PAGE.toString())) {

            info().data("contentTitle", contentDetail.description.title)
                    .data("releaseTitle", release.getDescription().getTitle())
                    .log("Adding dataset as a link to release");

            addRelatedDataset(release, contentDetail);
        }

        if (contentDetail.type.equals(PageType.STATIC_QMI.toString())) {

            info().data("contentTitle", contentDetail.description.title)
                    .data("releaseTitle", release.getDescription().getTitle())
                    .log("Adding qmi as a link to release");

            addRelatedQMI(release, contentDetail);
        }

        if (contentDetail.type.equals(PageType.STATIC_METHODOLOGY.toString())
                || contentDetail.type.equals(PageType.STATIC_METHODOLOGY_DOWNLOAD.toString())) {

            info().data("contentTitle", contentDetail.description.title)
                    .data("releaseTitle", release.getDescription().getTitle())
                    .log("Adding methodology article as a link to release");

            addRelatedMethodologyArticle(release, contentDetail);
        }
    }

    private static void addRelatedDataset(Release release, ContentDetail contentDetail) {
        Link link = createLink(contentDetail);

        if (release.getRelatedDatasets() == null) {
            release.setRelatedDatasets(new ArrayList<>());
        }
        release.getRelatedDatasets().add(link);
    }

    private static void addRelatedDocument(Release release, ContentDetail contentDetail) {
        Link link = createLink(contentDetail);
        if (release.getRelatedDocuments() == null) {
            release.setRelatedDocuments(new ArrayList<>());
        }
        release.getRelatedDocuments().add(link);
    }

    private static void addRelatedMethodologyArticle(Release release, ContentDetail contentDetail) {
        Link link = createLink(contentDetail);
        if (release.getRelatedMethodologyArticle() == null) {
            release.setRelatedMethodologyArticle(new ArrayList<>());
        }
        release.getRelatedMethodologyArticle().add(link);
    }

    private static void addRelatedQMI(Release release, ContentDetail contentDetail) {
        Link link = createLink(contentDetail);
        if (release.getRelatedMethodology() == null) {
            release.setRelatedMethodology(new ArrayList<>());
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
                                       Iterable<ContentDetail> collectionContent) throws IOException {
        if (collection.isRelease()) {
            info().data("collectionId", collection.getDescription().getId())
                    .log("Release identified for collection, populating the page links");

            try {
                collection.populateRelease(collectionReader, collectionWriter, collectionContent);
            } catch (ZebedeeException e) {
                error().data("collectionId", collection.getDescription().getId())
                        .logException(e, "Failed to populate release page for collection");
            }
        }
    }
}
