package com.github.onsdigital.zebedee.data.framework;

import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.api.Collection;
import com.github.onsdigital.zebedee.content.page.base.Page;
import com.github.onsdigital.zebedee.json.Session;
import com.github.onsdigital.zebedee.model.CollectionContentWriter;
import com.github.onsdigital.zebedee.model.ContentWriter;

import java.util.Date;

/**
 * Created by thomasridd on 1/18/16.
 */
public class DataBuilder {
    Zebedee zebedee;
    Session publisher;

    public DataBuilder(Zebedee zebedee, Session publisher) {

    }
    public void addPublishedDataset(String name, Date releaseDate, int timeseries) {

    }
    private void publishPage(Page page, String uri) {
        String publishTo = uri;
        if (publishTo.startsWith("/"))
            publishTo = publishTo.substring(1);
        ContentWriter writer = new ContentWriter(zebedee.published.path);

        zebedee.published.path.resolve(uri);
    }



}
