package com.github.onsdigital.zebedee.content.staticpage;

import com.github.onsdigital.zebedee.content.base.Content;
import com.github.onsdigital.zebedee.content.base.ContentType;
import com.github.onsdigital.zebedee.content.link.ContentReference;
import com.github.onsdigital.zebedee.content.link.Link;
import com.github.onsdigital.zebedee.content.partial.markdown.MarkdownSection;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by bren on 29/06/15.
 */
public class StaticArticle extends Content {

    private List<ContentReference> relatedData;
    private List<ContentReference> relatedDocuments;
    private List<Link> downloads;

    private List<MarkdownSection> sections = new ArrayList<>();
    private List<MarkdownSection> accordion = new ArrayList<>();


    @Override
    public ContentType getType() {
        return ContentType.static_article;
    }

    public List<ContentReference> getRelatedData() {
        return relatedData;
    }

    public void setRelatedData(List<ContentReference> relatedData) {
        this.relatedData = relatedData;
    }

    public List<ContentReference> getRelatedDocuments() {
        return relatedDocuments;
    }

    public void setRelatedDocuments(List<ContentReference> relatedDocuments) {
        this.relatedDocuments = relatedDocuments;
    }

    public List<MarkdownSection> getSections() {
        return sections;
    }

    public void setSections(List<MarkdownSection> sections) {
        this.sections = sections;
    }

    public List<MarkdownSection> getAccordion() {
        return accordion;
    }

    public void setAccordion(List<MarkdownSection> accordion) {
        this.accordion = accordion;
    }

    public List<Link> getDownloads() {
        return downloads;
    }

    public void setDownloads(List<Link> downloads) {
        this.downloads = downloads;
    }
}
