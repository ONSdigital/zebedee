package com.github.onsdigital.zebedee.content.page.taxonomy;

import com.github.onsdigital.zebedee.content.page.base.PageType;
import com.github.onsdigital.zebedee.content.partial.PageReference;
import com.github.onsdigital.zebedee.content.page.taxonomy.base.TaxonomyNode;

import java.util.List;

/**
 * Created by bren on 04/06/15.
 *
 * Represents a product node that holds links to related statistics under the product ( e.g. cpi )
 */
public class TaxonomyProductNode extends TaxonomyNode {

    private List<PageReference> items;
    private List<PageReference> datasets;
    private List<PageReference> statsBulletins;
    private List<PageReference> relatedArticles;

    @Override
    public PageType getType() {
        return PageType.product_page;
    }

    public List<PageReference> getItems() {
        return items;
    }

    public void setItems(List<PageReference> items) {
        this.items = items;
    }

    public List<PageReference> getDatasets() {
        return datasets;
    }

    public void setDatasets(List<PageReference> datasets) {
        this.datasets = datasets;
    }

    public List<PageReference> getStatsBulletins() {
        return statsBulletins;
    }

    public void setStatsBulletins(List<PageReference> statsBulletins) {
        this.statsBulletins = statsBulletins;
    }


    public List<PageReference> getRelatedArticles() {
        return relatedArticles;
    }

    public void setRelatedArticles(List<PageReference> relatedArticles) {
        this.relatedArticles = relatedArticles;
    }
}
