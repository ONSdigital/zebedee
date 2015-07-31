package com.github.onsdigital.zebedee.content.taxonomy;

import com.github.onsdigital.zebedee.content.base.ContentType;
import com.github.onsdigital.zebedee.content.link.ContentReference;
import com.github.onsdigital.zebedee.content.taxonomy.base.TaxonomyNode;

import java.util.List;

/**
 * Created by bren on 04/06/15.
 *
 * Represents a product node that holds links to related statistics under the product ( e.g. cpi )
 */
public class TaxonomyProductNode extends TaxonomyNode {

    private List<ContentReference> items;
    private List<ContentReference> datasets;
    private List<ContentReference> statsBulletins;
    private List<ContentReference> relatedArticles;

    @Override
    public ContentType getType() {
        return ContentType.product_page;
    }

    public List<ContentReference> getItems() {
        return items;
    }

    public void setItems(List<ContentReference> items) {
        this.items = items;
    }

    public List<ContentReference> getDatasets() {
        return datasets;
    }

    public void setDatasets(List<ContentReference> datasets) {
        this.datasets = datasets;
    }

    public List<ContentReference> getStatsBulletins() {
        return statsBulletins;
    }

    public void setStatsBulletins(List<ContentReference> statsBulletins) {
        this.statsBulletins = statsBulletins;
    }


    public List<ContentReference> getRelatedArticles() {
        return relatedArticles;
    }

    public void setRelatedArticles(List<ContentReference> relatedArticles) {
        this.relatedArticles = relatedArticles;
    }
}
