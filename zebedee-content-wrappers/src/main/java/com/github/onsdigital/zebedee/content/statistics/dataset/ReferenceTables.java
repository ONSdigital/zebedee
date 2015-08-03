package com.github.onsdigital.zebedee.content.statistics.dataset;

import com.github.onsdigital.zebedee.content.base.ContentType;

/**
 * Created by bren on 01/07/15.
 */
public class ReferenceTables extends Dataset {

    private boolean migrated;

    @Override
    public ContentType getType() {
        return ContentType.reference_tables;
    }

    public boolean isMigrated() {
        return migrated;
    }

    public void setMigrated(boolean migrated) {
        this.migrated = migrated;
    }
}
