package com.github.onsdigital.zebedee.model.approval.tasks;

import com.github.onsdigital.zebedee.content.page.release.Release;
import com.github.onsdigital.zebedee.content.partial.Link;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.json.ContentDetail;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;

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
        switch (contentDetail.getType()) {
        case ARTICLE:
        case ARTICLE_DOWNLOAD:
        case BULLETIN:
        case COMPENDIUM_LANDING_PAGE:
            info().data("contentTitle", contentDetail.description.title).data("releaseTitle", release.getDescription().getTitle())
                    .log("Adding document as a link to release");

            addRelatedDocument(release, contentDetail);
            break;
        case DATASET_LANDING_PAGE:
            info().data("contentTitle", contentDetail.description.title)
                    .data("releaseTitle", release.getDescription().getTitle())
                    .log("Adding dataset as a link to release");

            addRelatedDataset(release, contentDetail);
            break;
            case API_DATASET_LANDING_PAGE:
            info().data("contentTitle", contentDetail.description.title)
                    .data("releaseTitle", release.getDescription().getTitle())
                    .log("Adding cantabular or cmd dataset as a link to release");

            addRelatedAPIDataset(release, contentDetail);
            break;
        case STATIC_QMI:
            info().data("contentTitle", contentDetail.description.title)
                    .data("releaseTitle", release.getDescription().getTitle())
                    .log("Adding qmi as a link to release");

            addRelatedQMI(release, contentDetail);
            break;
        case STATIC_METHODOLOGY:
        case STATIC_METHODOLOGY_DOWNLOAD:
            info().data("contentTitle", contentDetail.description.title)
                    .data("releaseTitle", release.getDescription().getTitle())
                    .log("Adding methodology article as a link to release");

            addRelatedMethodologyArticle(release, contentDetail);
            break;
        default: // Do nothing for other types
        }
    }

    private static void addRelatedDataset(Release release, ContentDetail contentDetail) {
        Link link = createLink(contentDetail);

        if (release.getRelatedDatasets() == null) {
            release.setRelatedDatasets(new ArrayList<>());
        }
        release.getRelatedDatasets().add(link);
    }

    private static void addRelatedAPIDataset(Release release, ContentDetail contentDetail) {
        Link link = createLink(contentDetail);
        if (release.getRelatedAPIDatasets() == null) {
            release.setRelatedAPIDatasets(new ArrayList<>());
        }
        release.getRelatedAPIDatasets().add(link);
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

}
