package com.github.onsdigital.zebedee.content.page.release;

import com.github.onsdigital.zebedee.content.page.base.PageType;
import com.github.onsdigital.zebedee.content.page.statistics.document.base.StatisticalDocument;
import com.github.onsdigital.zebedee.content.partial.markdown.MarkdownSection;

import java.util.List;

/**
 * Created by bren on 04/06/15.
 */
public class Release extends StatisticalDocument {

    private MarkdownSection preRelease;

    private List<ReleaseDateChange> dateChanges;

    @Override
    public PageType getType() {
        return PageType.release;
    }

    public MarkdownSection getPreRelease() {
        return preRelease;
    }

    public void setPreRelease(MarkdownSection preRelease) {
        this.preRelease = preRelease;
    }

    public List<ReleaseDateChange> getDateChanges() {
        return dateChanges;
    }

    public void setDateChanges(List<ReleaseDateChange> dateChanges) {
        this.dateChanges = dateChanges;
    }
}
