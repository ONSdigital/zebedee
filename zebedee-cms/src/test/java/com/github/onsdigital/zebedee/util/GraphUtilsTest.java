package com.github.onsdigital.zebedee.util;

import com.github.davidcarboni.ResourceUtils;
import com.github.onsdigital.zebedee.Builder;
import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.content.page.statistics.dataset.DatasetLandingPage;
import com.github.onsdigital.zebedee.content.page.statistics.document.article.Article;
import com.github.onsdigital.zebedee.content.page.statistics.document.bulletin.Bulletin;
import com.github.onsdigital.zebedee.content.page.taxonomy.ProductPage;
import com.github.onsdigital.zebedee.content.page.taxonomy.TaxonomyLandingPage;
import com.github.onsdigital.zebedee.content.partial.Link;
import org.junit.Test;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

/**
 * Created by thomasridd on 16/07/15.
 */
public class GraphUtilsTest {

    @Test
    public void graphUtilsShouldIdentifyRelatedLinksOnABulletin() {
        // Given
        // a bulletin with a bunch of links to various media
        Bulletin bulletin = new Bulletin();

        List<Link> references = new ArrayList<>();
        references.add(new Link(URI.create("relatedbulletin1")));
        references.add(new Link(URI.create("relatedbulletin2")));
        bulletin.setRelatedBulletins(references);

        references = new ArrayList<>();
        references.add(new Link(URI.create("relateddataset1")));
        references.add(new Link(URI.create("relateddataset2")));
        bulletin.setRelatedData(references);

        // When
        // we get the related list
        List<String> relatedUris = GraphUtils.relatedUris(bulletin);
        Collections.sort(relatedUris);

        // Then
        // we expect the links above
        List<String> expected = Arrays.asList("relatedbulletin1", "relatedbulletin2", "relateddataset1", "relateddataset2");
        assertArrayEquals(expected.toArray(), relatedUris.toArray());
    }

    @Test
    public void graphUtilsShouldIdentifyRelatedLinksOnADataset() {
        // Given
        // a dataset with a bunch of links to various media
        DatasetLandingPage dataset = new DatasetLandingPage();

        List<Link> references = new ArrayList<>();
        references.add(new Link(URI.create("relatedarticle1")));
        references.add(new Link(URI.create("relatedbulletin1")));
        dataset.setRelatedDocuments(references);

        references = new ArrayList<>();
        references.add(new Link(URI.create("relateddataset1")));
        references.add(new Link(URI.create("relateddataset2")));
        dataset.setRelatedDatasets(references);

        // When
        // we get the related list
        List<String> relatedUris = GraphUtils.relatedUris(dataset);
        Collections.sort(relatedUris);

        // Then
        // we expect the links above repeated
        List<String> expected = Arrays.asList("relatedarticle1", "relatedbulletin1", "relateddataset1", "relateddataset2");
        assertArrayEquals(expected.toArray(), relatedUris.toArray());
    }

    @Test
    public void graphUtilsShouldIdentifyRelatedLinksOnAnArticle() {
        // Given
        // a dataset with a bunch of links to various media
        Article article = new Article();

        List<Link> references = new ArrayList<>();
        references.add(new Link(URI.create("relatedarticle1")));
        references.add(new Link(URI.create("relatedarticle2")));
        article.setRelatedArticles(references);

        references = new ArrayList<>();
        references.add(new Link(URI.create("relateddataset1")));
        references.add(new Link(URI.create("relateddataset2")));
        article.setRelatedData(references);

        // When
        // we get the related list
        List<String> relatedUris = GraphUtils.relatedUris(article);
        Collections.sort(relatedUris);

        // Then
        // we expect the links above repeated
        List<String> expected = Arrays.asList("relatedarticle1", "relatedarticle2", "relateddataset1", "relateddataset2");
        assertArrayEquals(expected.toArray(), relatedUris.toArray());
    }

    @Test
    public void graphUtilsShouldIdentifyRelatedLinksOnAProductPage() {
        // Given
        // a dataset with a bunch of links to various media
        ProductPage productPage = new ProductPage();

        List<Link> references = new ArrayList<>();
        references.add(new Link(URI.create("relatedarticle1")));
        references.add(new Link(URI.create("relatedarticle2")));
        productPage.setRelatedArticles(references);

        references = new ArrayList<>();
        references.add(new Link(URI.create("relatedbulletin1")));
        references.add(new Link(URI.create("relatedbulletin2")));
        productPage.setStatsBulletins(references);

        references = new ArrayList<>();
        references.add(new Link(URI.create("relateddataset1")));
        references.add(new Link(URI.create("relateddataset2")));
        productPage.setDatasets(references);

        references = new ArrayList<>();
        references.add(new Link(URI.create("relatedtimeseries1")));
        references.add(new Link(URI.create("relatedtimeseries2")));
        productPage.setItems(references);

        // When
        // we get the related list
        List<String> relatedUris = GraphUtils.relatedUris(productPage);
        Collections.sort(relatedUris);

        // Then
        // we expect the links above repeated
        List<String> expected = Arrays.asList("relatedarticle1", "relatedarticle2",
                "relatedbulletin1", "relatedbulletin2",
                "relateddataset1", "relateddataset2", "relatedtimeseries1", "relatedtimeseries2");
        assertArrayEquals(expected.toArray(), relatedUris.toArray());
    }

    @Test
    public void graphUtilsShouldIdentifyRelatedLinksOnALandingPage() {
        // Given
        // a dataset with a bunch of links to various media
        TaxonomyLandingPage landingPage = new TaxonomyLandingPage();

        List<Link> references = new ArrayList<>();
        references.add(new Link(URI.create("section1")));
        references.add(new Link(URI.create("section2")));
        landingPage.setSections(references);

        // When
        // we get the related list
        List<String> relatedUris = GraphUtils.relatedUris(landingPage);
        Collections.sort(relatedUris);

        // Then
        // we expect the links above repeated
        List<String> expected = Arrays.asList("section1", "section2");
        assertArrayEquals(expected.toArray(), relatedUris.toArray());
    }

    @Test
    public void graphUtilsShouldIdentifyProductPage() throws Exception {
        // With
        // The Basic zebedee setup
        Builder bob = new Builder(GraphUtils.class, ResourceUtils.getPath("/bootstraps/basic"));
        Zebedee zebedee = new Zebedee(bob.zebedee);


        // When
        // We identify the product page for
        String uri = "/themea/landinga/producta/bulletins/bulletina/2015-01-01";
        String pageUri = GraphUtils.productPageURIForPageWithURI(zebedee.launchpad, uri);

        // Then
        // we expect
        String expected = "/themea/landinga/producta";
        assertEquals(expected, pageUri);

    }
}