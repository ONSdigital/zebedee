package com.github.onsdigital.zebedee.content.home;

import com.github.onsdigital.zebedee.content.link.ContentReference;

/**
 * Represents sections on homepage with references to a taxonomy browse page and a timeseries page
 *
 * @author Bren
 */

public class HomeSection implements Comparable<HomeSection> {


    private Integer index; //Used for ordering of sections on homepage
    private ContentReference theme;
    private ContentReference statistics;

    public HomeSection() {
    }

    public HomeSection(ContentReference themeReference, ContentReference statistics) {
        this(themeReference, statistics, null);
    }

    public HomeSection(ContentReference themeReference, ContentReference statistics, Integer index) {
        this.theme = themeReference;
        this.statistics = statistics;
        this.index = index;
    }

    @Override
    public int compareTo(HomeSection o) {
        if (this.index == null) {
            return -1;
        }
        return Integer.compare(this.index, o.index);
    }
}
