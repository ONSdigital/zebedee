package com.github.onsdigital.zebedee.content.page.feedback;

import com.github.onsdigital.zebedee.content.page.base.PageType;
import com.github.onsdigital.zebedee.content.page.staticpage.base.BaseStaticPage;

/**
 * Created by dave on 9/6/16.
 */
public class FeedbackReceived extends BaseStaticPage {

    private String acknowledgement;

    public void setAcknowledgement(String acknowledgement) {
        this.acknowledgement = acknowledgement;
    }

    public String getAcknowledgement() {
        return acknowledgement;
    }

    @Override
    public PageType getType() {
        return PageType.feedback_received;
    }
}
