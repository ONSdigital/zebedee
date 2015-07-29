package com.github.onsdigital.zebedee.content.statistics.document.bulletin;

import com.github.onsdigital.zebedee.content.base.ContentType;
import com.github.onsdigital.zebedee.content.link.ContentReference;
import com.github.onsdigital.zebedee.content.statistics.document.base.StatisticalDocument;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by bren on 04/06/15.
// */
public class Bulletin extends StatisticalDocument {

    private List<ContentReference> relatedBulletins = new ArrayList<>();

    @Override
    public ContentType getType() {
        return ContentType.bulletin;
    }

    public List<ContentReference> getRelatedBulletins() {
        return relatedBulletins;
    }

    public void setRelatedBulletins(List<ContentReference> relatedBulletins) {
        this.relatedBulletins = relatedBulletins;
    }


    public static void main(String args[]) {
        Bulletin bulletin = new Bulletin();
    }
}
