package com.github.onsdigital.zebedee.content.page.statistics.document.article;

import com.github.onsdigital.zebedee.content.page.base.PageType;
import com.github.onsdigital.zebedee.content.partial.Link;
import com.github.onsdigital.zebedee.content.page.statistics.document.base.StatisticalDocument;

import java.util.List;

/**
 * Created by bren on 04/06/15.
 */
public class Article extends StatisticalDocument {

    /*Body*/
    private List<Link> relatedArticles;

    @Override
    public PageType getType() {
        return PageType.article;
    }

    public void setRelatedArticles(List<Link> relatedArticles) {
        this.relatedArticles = relatedArticles;
    }

    public List<Link> getRelatedArticles() {
        return relatedArticles;
    }
}
