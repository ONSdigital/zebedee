package com.github.onsdigital.zebedee.util;

import com.github.onsdigital.zebedee.Zebedee;

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

    public Penelope(Zebedee zebedee) {
        this.zebedee = zebedee;
    }

    public void knit() {
        checkDatasetsInTheSameStatsBulletinReferenceEachOther();
        checkNondirectionalityInGraph();
        removeReverseRelationshipsToOutdatedStatsBulletins();
    }

    private void checkDatasetsInTheSameStatsBulletinReferenceEachOther() {

    }
    private void checkNondirectionalityInGraph() {

    }
    private void removeReverseRelationshipsToOutdatedStatsBulletins() {

    }
}
