package com.github.onsdigital.zebedee.util;

import com.github.onsdigital.zebedee.content.dynamic.browse.ContentNode;

import java.net.URI;
import java.util.Comparator;
import java.util.Map;

/**
 * Created by bren on 04/08/15.
 *
 * Used to order content map alphabetically by title
 *
 */
public class ContentNodeComparator implements Comparator<URI> {

    Map<URI, ContentNode> map;

    public ContentNodeComparator(Map<URI,ContentNode> nodes) {
        this.map = nodes;
    }

    @Override
    public int compare(URI uri1, URI uri2) {
        ContentNode node1 = map.get(uri1);
        ContentNode node2 = map.get(uri2);
        if (node1 == null) {
            return 1;//nulls last
        }
        if (node2 == null) {
            return -1;
        }

        return node1.compareTo(node2);
    }
}
