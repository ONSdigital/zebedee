package com.github.onsdigital.zebedee.reader.util;

import com.github.onsdigital.zebedee.content.dynamic.browse.ContentNode;

import java.util.Comparator;

/**
 * Created by bren on 27/08/15.
 *
 * Compares by release date, content released later comes before the earlier ones
 *
 */
public class ReleaseDateComparator implements Comparator<ContentNode> {
    @Override
    public int compare(ContentNode o1, ContentNode o2) {
        if (isNoDate(o1)) {
            return 1;
        }
        if (isNoDate(o2)) {
            return -1;
        }

        return -1 *  o1.getDescription().getReleaseDate().compareTo(o2.getDescription().getReleaseDate());

    }


    private boolean isNoDate(ContentNode node) {
        return node == null || node.getDescription() == null || node.getDescription().getReleaseDate() == null;
    }
}
