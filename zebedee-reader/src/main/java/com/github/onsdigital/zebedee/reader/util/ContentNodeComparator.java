package com.github.onsdigital.zebedee.reader.util;

import com.github.onsdigital.zebedee.content.dynamic.browse.ContentNode;

import java.net.URI;
import java.util.Comparator;
import java.util.Map;

/**
 * Created by bren on 04/08/15.
 *
 * Used to order content map alphabetically by title. Can sort in reverse order
 *
 *
 */
public class ContentNodeComparator implements Comparator<URI> {

    private Map<URI, ContentNode> map;
    private boolean reverse;

    public ContentNodeComparator(Map<URI,ContentNode> nodes, boolean reverse) {
        this.map = nodes;
        this.reverse = reverse;
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

        return (reverse ? -1 : 1) * node1.compareTo(node2);
    }
}
