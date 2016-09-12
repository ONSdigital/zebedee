package com.github.onsdigital.zebedee.content.page.feedback;

import com.github.onsdigital.zebedee.content.page.base.PageDescription;
import com.github.onsdigital.zebedee.content.page.base.PageType;
import com.github.onsdigital.zebedee.content.page.staticpage.base.BaseStaticPage;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Created by dave on 9/6/16.
 */
public class FeedbackPage extends BaseStaticPage {

    static final String FEEDBACK_URI = "/feedback";
    static final String ACKNOWLEDGEMENT = "Thank you for your feedback";
    static final String TITLE = "Feedback";
    static final String SUMMARY = "Share your thoughts to help us improve the site.";

    private String acknowledgement;

    public void setAcknowledgement(String acknowledgement) {
        this.acknowledgement = acknowledgement;
    }

    public String getAcknowledgement() {
        return acknowledgement;
    }

    @Override
    public PageType getType() {
        return PageType.feedback_page;
    }

    public static FeedbackPage feedbackDefault() {
        try {
            FeedbackPage feedback = new FeedbackPage();
            feedback.setUri(new URI(FEEDBACK_URI));
            feedback.setAcknowledgement(ACKNOWLEDGEMENT);
            feedback.setDescription(new PageDescription());
            feedback.getDescription().setTitle(TITLE);
            feedback.getDescription().setSummary(SUMMARY);
            return feedback;
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
