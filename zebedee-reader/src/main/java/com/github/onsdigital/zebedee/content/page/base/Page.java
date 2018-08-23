package com.github.onsdigital.zebedee.content.page.base;

import cc.fasttext.Vector;
import com.github.onsdigital.zebedee.content.base.Content;
import com.github.onsdigital.zebedee.content.partial.Link;
import com.github.onsdigital.zebedee.search.fastText.FastTextHelper;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

//    public abstract String getPageSentence();

    public double[] getEmbeddingVector() throws IOException {
        String sentence = this.getPageSentence();

        int dim = FastTextHelper.getInstance().getDimensions();

        double[] sentenceVector = new double[dim];

        if (null != sentence && !sentence.isEmpty()) {
            Vector vector = FastTextHelper.getInstance().getFastText().getSentenceVector(sentence);
            sentenceVector = FastTextHelper.toDoubleArray(vector);
        }

        return sentenceVector;
    }

    public String getEncodedEmbeddingVector() throws IOException {
        return convertArrayToBase64(this.getEmbeddingVector());
    }

    public Map<String, Float> getLabels(int k) throws IOException {
        String sentence = this.getPageSentence();

        if (null != sentence && !sentence.isEmpty()) {
            return FastTextHelper.getInstance().getFastText().predictLine(sentence, k);
        }
        return new HashMap<>();
    }

    public List<String> generateKeywords(int k, float threshold) throws IOException {
        Map<String, Float> labels = this.getLabels(k);

        List<String> filteredLabels = new ArrayList<>();
        for (String key : labels.keySet()) {
            if (labels.get(key) >= threshold) {
                String formattedKey = key.replace(FastTextHelper.PREFIX, "").replace("_", " ").trim();
                filteredLabels.add(formattedKey);
            }
        }

        return filteredLabels;
    }
}
