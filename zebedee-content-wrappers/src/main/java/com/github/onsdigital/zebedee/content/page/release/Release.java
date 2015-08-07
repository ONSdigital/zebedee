package com.github.onsdigital.zebedee.content.page.release;

import com.github.onsdigital.zebedee.content.page.base.Page;
import com.github.onsdigital.zebedee.content.page.base.PageType;
import com.github.onsdigital.zebedee.content.partial.Link;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by bren on 04/06/15.
 */
public class Release extends Page {

    private List<Link> articles = new ArrayList<>();
    private List<Link> bulletins = new ArrayList<>();
    private List<Link> datasets = new ArrayList<>();

    @Override
    public PageType getType() {
        return PageType.release;
    }


    public List<Link> getArticles() {
        return articles;
    }

    public void setArticles(List<Link> articles) {
        this.articles = articles;
    }

    public List<Link> getBulletins() {
        return bulletins;
    }

    public void setBulletins(List<Link> bulletins) {
        this.bulletins = bulletins;
    }

    public List<Link> getDatasets() {
        return datasets;
    }

    public void setDatasets(List<Link> datasets) {
        this.datasets = datasets;
    }

}
