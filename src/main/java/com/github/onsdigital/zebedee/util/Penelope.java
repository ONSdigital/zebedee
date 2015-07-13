package com.github.onsdigital.zebedee.util;

import com.github.onsdigital.content.link.PageReference;
import com.github.onsdigital.content.page.statistics.document.bulletin.Bulletin;
import com.github.onsdigital.content.util.ContentUtil;
import com.github.onsdigital.zebedee.Zebedee;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
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
public class Penelope {
    Zebedee zebedee;
    Yaffle librarian;

    public Penelope(Zebedee zebedee) {
        this.zebedee = zebedee;
        this.librarian = new Yaffle(zebedee);
    }

    public void knit() throws IOException {
        librarian.catalogue(); // Builds an index to the website

        checkDatasetsInTheSameStatsBulletinReferenceEachOther();
        checkNondirectionalityInGraph();
        removeReverseRelationshipsToOutdatedStatsBulletins();
    }

    private void checkDatasetsInTheSameStatsBulletinReferenceEachOther() throws IOException {
        for (HashMap<String, String> bulletinDict: librarian.bulletins) {
            String uri = bulletinDict.get("Uri");
            try (InputStream stream = Files.newInputStream(zebedee.launchpad.get(uri))) {
                Bulletin bulletin = ContentUtil.deserialise(stream, Bulletin.class);
                List<PageReference> relatedData = bulletin.getRelatedData();
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
}
