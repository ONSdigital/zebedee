package com.github.onsdigital.zebedee.content.page.statistics.document.bulletin;

import com.github.onsdigital.zebedee.content.page.base.PageType;
import com.github.onsdigital.zebedee.content.partial.PageReference;
import com.github.onsdigital.zebedee.content.page.statistics.document.base.StatisticalDocument;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by bren on 04/06/15.
// */
public class Bulletin extends StatisticalDocument {

    private List<PageReference> relatedBulletins = new ArrayList<>();

    @Override
    public PageType getType() {
        return PageType.bulletin;
    }

    public List<PageReference> getRelatedBulletins() {
        return relatedBulletins;
    }

    public void setRelatedBulletins(List<PageReference> relatedBulletins) {
        this.relatedBulletins = relatedBulletins;
    }


    public static void main(String args[]) {
        Bulletin bulletin = new Bulletin();
    }
}
