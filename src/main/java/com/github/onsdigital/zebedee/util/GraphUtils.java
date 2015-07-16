package com.github.onsdigital.zebedee.util;

import com.github.onsdigital.content.link.PageReference;
import com.github.onsdigital.content.page.base.PageType;
import com.github.onsdigital.content.page.staticpage.Methodology;
import com.github.onsdigital.content.page.statistics.dataset.Dataset;
import com.github.onsdigital.content.page.statistics.document.article.Article;
import com.github.onsdigital.content.page.statistics.document.bulletin.Bulletin;
import com.github.onsdigital.content.page.taxonomy.ProductPage;
import com.github.onsdigital.content.page.taxonomy.TaxonomyLandingPage;
import com.github.onsdigital.content.page.taxonomy.base.TaxonomyPage;
import com.github.onsdigital.content.partial.DownloadSection;
import com.github.onsdigital.content.partial.FigureSection;
import com.github.onsdigital.content.util.ContentUtil;
import com.github.onsdigital.zebedee.Zebedee;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by thomasridd on 09/07/15.
 *
 * Penelope the knitting spider is a character from the magic roundabout
 *
 * Penelope's single function is to knit together the relationship network into a non-directional graph
 * She then removes edges that reference outdated bulletin nodes from datasets
 *
 * Standard algorithms exist but I'm not going to bother with optimisation just yet
 *
 * This is done on the launchpad
 */
public class GraphUtils {
    Zebedee zebedee;
    Librarian librarian;

    public GraphUtils(Zebedee zebedee) {
        this.zebedee = zebedee;
        this.librarian = new Librarian(zebedee);
    }

    // Knit functionality ----------------------------------------------------------------------------------------------
    /**
     * Knit checks through links made in the data and ensures a dense mesh of references without having to hand check
     *
     * TODO: Use and test this functionality in a wider context
     *
     * @throws IOException
     */
    public void knit() throws IOException {
        librarian.catalogue(); // Builds an index to the website

        // Same bulletin dataset<->dataset references
        checkDatasetsInTheSameStatsBulletinReferenceEachOther();

        //
        checkNondirectionalityInGraph();

        // Except links to bulletins which should be to the current version
        removeReverseRelationshipsToOutdatedStatsBulletins();
    }

    private void checkDatasetsInTheSameStatsBulletinReferenceEachOther() throws IOException {
        // For every bulletin
        for (HashMap<String, String> bulletinDict: librarian.bulletins) {
            String uri = bulletinDict.get("Uri");
            try (InputStream stream = Files.newInputStream(zebedee.launchpad.get(uri))) {
                Bulletin bulletin = ContentUtil.deserialise(stream, Bulletin.class);
                List<PageReference> relatedData = bulletin.getRelatedData();

                // For every pair of datasets referenced
                for(int i = 0; i < relatedData.size() - 1; i++) {
                    for (int j = i + 1; j < relatedData.size(); j++) {
                        // Ensure they reference each other
                        ensureDatasetsBidirectional(relatedData.get(i).getUri().toString(),
                                relatedData.get(j).getUri().toString());
                    }
                }
            }
        }
    }
    private void checkNondirectionalityInGraph() {

    }
    private void removeReverseRelationshipsToOutdatedStatsBulletins() {

    }
    private void ensureDatasetsBidirectional(String uri1, String uri2) {

    }
    private void ensureBulletinsBidirectional(String uri1, String uri2) {

    }
    private void ensureDatasetsToPages(String uri1, String uri2) {

    }

    // Related links ---------------------------------------------------------------------------------------------------
    /**
     * Find all links within the bulletin/article/dataset/...
     *
     * @param bulletin
     * @return
     */
    public static List<String> relatedUris(Bulletin bulletin) {
        List<String > results = new ArrayList<>();
        for (PageReference ref: bulletin.getRelatedBulletins()) {
            results.add(ref.getUri().toString());
        }
        for (PageReference ref: bulletin.getRelatedData()) {
            results.add(ref.getUri().toString());
        }
        for (FigureSection ref: bulletin.getCharts()) {
            results.add(ref.getUri().toString() + ".json");
            results.add(ref.getUri().toString() + ".png");
            results.add(ref.getUri().toString() + "-download.png");
        }
        for (FigureSection ref: bulletin.getTables()) {
            results.add(ref.getUri().toString() + ".json");
            results.add(ref.getUri().toString() + ".html");
            results.add(ref.getUri().toString() + ".xls");
        }
        return results;
    }
    public static List<String> relatedUris(Article article) {
        List<String > results = new ArrayList<>();
        for (PageReference ref: article.getRelatedArticles()) {
            results.add(ref.getUri().toString());
        }
        for (PageReference ref: article.getRelatedData()) {
            results.add(ref.getUri().toString());
        }
        for (FigureSection ref: article.getCharts()) {
            results.add(ref.getUri().toString() + ".json");
            results.add(ref.getUri().toString() + ".png");
            results.add(ref.getUri().toString() + "-download.png");
        }
        for (FigureSection ref: article.getTables()) {
            results.add(ref.getUri().toString() + ".json");
            results.add(ref.getUri().toString() + ".html");
            results.add(ref.getUri().toString() + ".xls");
        }
        return results;
    }
    public static List<String> relatedUris(Dataset dataset) {
        List<String > results = new ArrayList<>();
        for (PageReference ref: dataset.getRelatedDocuments()) {
            results.add(ref.getUri().toString());
        }
        for (DownloadSection ref: dataset.getDownloads()) {
            results.add(ref.getFile());
        }
        for (PageReference ref: dataset.getRelatedDatasets()) {
            results.add(ref.getUri().toString());
        }

        if (dataset.getRelatedMethodology() != null) {
            for (PageReference ref : dataset.getRelatedMethodology()) {
                if (ref.getUri() != null) {
                    results.add(ref.getUri().toString());
                }
            }
        }
        return results;
    }
    public static List<String> relatedUris(ProductPage productPage) {
        List<String > results = new ArrayList<>();
        if (productPage.getStatsBulletins() != null) {
            for (PageReference ref : productPage.getStatsBulletins()) {
                results.add(ref.getUri().toString());
            }
        }
        if (productPage.getItems() != null) {
            for (PageReference ref : productPage.getItems()) {
                results.add(ref.getUri().toString());
            }
        }
        if (productPage.getDatasets() != null) {
            for (PageReference ref : productPage.getDatasets()) {
                results.add(ref.getUri().toString());
            }
        }
        if (productPage.getRelatedArticles() != null) {
            for (PageReference ref : productPage.getRelatedArticles()) {
                results.add(ref.getUri().toString());
            }
        }

        return results;
    }
    public static List<String> relatedUris(TaxonomyLandingPage landingPage) {
        List<String > results = new ArrayList<>();

        if (landingPage.getSections() != null) {
            for (PageReference ref : landingPage.getSections()) {
                if (ref.getUri() != null) {
                    results.add(ref.getUri().toString());
                }
            }
        }
        return results;
    }

}
