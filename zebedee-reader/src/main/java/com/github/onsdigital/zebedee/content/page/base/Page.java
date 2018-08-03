package com.github.onsdigital.zebedee.content.page.base;

import cc.fasttext.Vector;
import com.github.onsdigital.zebedee.content.base.Content;
import com.github.onsdigital.zebedee.content.partial.Link;
import com.github.onsdigital.zebedee.search.fastText.FastTextHelper;
import com.google.common.collect.Sets;

import java.net.URI;
import java.util.*;

import static com.github.onsdigital.zebedee.search.fastText.FastTextHelper.convertArrayToBase64;

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

    public String getPageSentence() {
        if (null != this.getDescription() && null != this.getDescription().getTitle()) {
            String title = this.getDescription().getTitle();
            title = title.replaceAll("[^a-zA-Z ]", "").toLowerCase().replaceAll("\\s+", " ");

            return title;
        }
        return null;
    }

//    public abstract String getPageSentence();

    public double[] getEmbeddingVector() {
        String sentence = this.getPageSentence();

        int dim = FastTextHelper.getInstance().getDimensions();

        double[] sentenceVector = new double[dim];

        if (null != sentence && !sentence.isEmpty()) {
            Vector vector = FastTextHelper.getInstance().getFastText().getSentenceVector(sentence);
            sentenceVector = FastTextHelper.toDoubleArray(vector);
        }

        return sentenceVector;
    }

    public String getEncodedEmbeddingVector() {
        return convertArrayToBase64(this.getEmbeddingVector());
    }

    public Map<String, Float> getLabels(int k) {
        String sentence = this.getPageSentence();

        if (null != sentence && !sentence.isEmpty()) {
            return FastTextHelper.getInstance().getFastText().predictLine(sentence, k);
        }
        return new HashMap<>();
    }

    public List<String> generateKeywords(int k, float threshold) {
        Map<String, Float> labels = this.getLabels(k);

        List<String> filteredLabels = new ArrayList<>();
        for (String key : labels.keySet()) {
            if (labels.get(key) >= threshold) {
                String formattedKey = key.replace(FastTextHelper.PREFIX, "").replace("_", " ");
                filteredLabels.add(formattedKey);
            }
        }

        return filteredLabels;
    }
}
