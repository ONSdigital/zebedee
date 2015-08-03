package com.github.onsdigital.zebedee.content.page.home;

import com.github.onsdigital.zebedee.content.base.Content;
import com.github.onsdigital.zebedee.content.partial.PageReference;

/**
 * Represents sections on homepage with references to a taxonomy browse page and a timeseries page
 *
 * @author Bren
 */

public class HomeSection extends Content implements Comparable<HomeSection> {


    private Integer index; //Used for ordering of sections on homepage
    private PageReference theme;
    private PageReference statistics;

    public HomeSection() {
    }

    public HomeSection(PageReference themeReference, PageReference statistics) {
        this(themeReference, statistics, null);
    }

    public HomeSection(PageReference themeReference, PageReference statistics, Integer index) {
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
