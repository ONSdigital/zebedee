package com.github.onsdigital.zebedee.content.statistics.document.article;

import com.github.onsdigital.zebedee.content.base.ContentType;
import com.github.onsdigital.zebedee.content.link.ContentReference;
import com.github.onsdigital.zebedee.content.statistics.document.base.StatisticalDocument;

import java.util.List;

/**
 * Created by bren on 04/06/15.
 */
public class Article extends StatisticalDocument {

    /*Body*/
    private List<ContentReference> relatedArticles;

    @Override
    public ContentType getType() {
        return ContentType.article;
    }

    public void setRelatedArticles(List<ContentReference> relatedArticles) {
        this.relatedArticles = relatedArticles;
    }

    public List<ContentReference> getRelatedArticles() {
        return relatedArticles;
    }
}
