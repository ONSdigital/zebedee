package com.github.onsdigital.zebedee.content.page.base;

import com.github.onsdigital.zebedee.content.base.Content;
import com.github.onsdigital.zebedee.content.partial.Link;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by bren on 10/06/15.
 * <p>
 * This is the generic content object that that has common properties of all page types on the website
 */
public abstract class Page extends Content {

    protected PageType type;

    private URI uri;

    private PageDescription description;

    private List<Link> topics;

    private List<String> generatedKeywords;

    private String encodedEmbeddingVector;

    private List<String> searchTerms;

    public Page() {
        this.type = getType();
    }

    public abstract PageType getType();

    public PageDescription getDescription() {
        return description;
    }

    public void setDescription(PageDescription description) {
        this.description = description;
    }

    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    public List<Link> getTopics() {
        return topics;
    }

    public void setTopics(List<Link> topics) {
        this.topics = topics;
    }

    public List<String> getGeneratedKeywords() {
        return generatedKeywords;
    }

    public void setGeneratedKeywords(List<String> generatedKeywords) {
        // Remove underscores from keywords
        List<String> keywords = generatedKeywords.stream()
                .map(x -> x.replace("_", " "))
                .collect(Collectors.toList());
        this.generatedKeywords = keywords;
    }

    public String getEncodedEmbeddingVector() {
        return encodedEmbeddingVector;
    }

    public void setEncodedEmbeddingVector(String encodedEmbeddingVector) {
        this.encodedEmbeddingVector = encodedEmbeddingVector;
    }

    public List<String> getSearchTerms() {
        return searchTerms;
    }

    public void setSearchTerms(List<String> searchTerms) {
        this.searchTerms = searchTerms;
    }

    public String getPageSentence() {
        String sentence = null;
        if (null != this.getDescription() && null != this.getDescription().getSummary()) {
            sentence = this.getDescription().getSummary();
        } else if (null != this.getDescription() && null != this.getDescription().getTitle()) {
            sentence = this.getDescription().getTitle();
        }

        if (null != sentence) {
            sentence = sentence.replaceAll("[^a-zA-Z ]", "").toLowerCase().replaceAll("\\s+", " ");
        }
        return sentence;
    }
}
