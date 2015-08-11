package com.github.onsdigital.zebedee.content.page.home;

import com.github.onsdigital.zebedee.content.base.Content;
import com.github.onsdigital.zebedee.content.partial.Link;

/**
 * Represents sections on homepage with references to a taxonomy browse page and a timeseries page
 *
 * @author Bren
 */

public class HomeSection extends Content implements Comparable<HomeSection> {


    private Integer index; //Used for ordering of sections on homepage
    private Link theme;
    private Link statistics;

    public HomeSection() {
    }

    public HomeSection(Link themeReference, Link statistics) {
        this(themeReference, statistics, null);
    }

    public HomeSection(Link themeReference, Link statistics, Integer index) {
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
