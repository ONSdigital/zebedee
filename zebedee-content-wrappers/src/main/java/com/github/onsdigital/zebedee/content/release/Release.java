package com.github.onsdigital.zebedee.content.release;

import com.github.onsdigital.zebedee.content.base.Content;
import com.github.onsdigital.zebedee.content.base.ContentType;
import com.github.onsdigital.zebedee.content.link.ContentReference;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by bren on 04/06/15.
 */
public class Release extends Content {

    private List<ContentReference> articles = new ArrayList<>();
    private List<ContentReference> bulletins = new ArrayList<>();
    private List<ContentReference> datasets = new ArrayList<>();

    @Override
    public ContentType getType() {
        return ContentType.release;
    }


    public List<ContentReference> getArticles() {
        return articles;
    }

    public void setArticles(List<ContentReference> articles) {
        this.articles = articles;
    }

    public List<ContentReference> getBulletins() {
        return bulletins;
    }

    public void setBulletins(List<ContentReference> bulletins) {
        this.bulletins = bulletins;
    }

    public List<ContentReference> getDatasets() {
        return datasets;
    }

    public void setDatasets(List<ContentReference> datasets) {
        this.datasets = datasets;
    }

}
