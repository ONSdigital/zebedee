package com.github.onsdigital.zebedee.content.page.base;

import com.github.onsdigital.zebedee.content.base.Content;
import com.github.onsdigital.zebedee.content.partial.Link;
import com.github.onsdigital.zebedee.search.fastText.FastTextHelper;
import com.github.onsdigital.zebedee.search.model.SearchDocument;
import com.google.common.collect.Sets;

import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

import static com.github.onsdigital.zebedee.logging.ZebedeeReaderLogBuilder.elasticSearchLog;

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

    public SearchDocument toSearchDocument() {
        SearchDocument indexDocument = new SearchDocument();
        indexDocument.setUri(this.getUri());
        indexDocument.setTopics(getTopics(this.getTopics()));
        indexDocument.setType(this.getType());
        indexDocument.setSearchBoost(this.getSearchTerms());

        PageDescription pageDescription = this.getDescription();

        if (FastTextHelper.Configuration.INDEX_EMBEDDING_VECTORS) {
            // Get fastText generated keywords
            List<String> generatedKeywords = this.getGeneratedKeywords();

            // Combine with original keywords
            if (null != generatedKeywords && !generatedKeywords.isEmpty()) {

                if (null != pageDescription.getKeywords() && !pageDescription.getKeywords().isEmpty()) {
                    generatedKeywords.addAll(pageDescription.getKeywords());
                }
                Set<String> set = Sets.newLinkedHashSet(generatedKeywords);
                pageDescription.setKeywords(new LinkedList<>(new LinkedList<>(set)));
            }

            // Index word embedding vector
            indexDocument.setEmbedding_vector(this.getEncodedEmbeddingVector());
        }
        indexDocument.setDescription(pageDescription);

        return indexDocument;
    }

    private static ArrayList<URI> getTopics(List<Link> topics) {
        if (topics == null) {
            return null;
        }
        ArrayList<URI> uriList = new ArrayList<>();
        for (Link topic : topics) {
            uriList.add(topic.getUri());
        }

        return uriList;
    }
}
