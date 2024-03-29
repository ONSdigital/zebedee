package com.github.onsdigital.zebedee.util.slack;

import com.github.onsdigital.slack.Profile;
import com.github.onsdigital.slack.client.PostMessageResponse;
import com.github.onsdigital.slack.client.SlackClient;
import com.github.onsdigital.slack.messages.PostMessage;

import static com.github.onsdigital.zebedee.logging.CMSLogEvent.info;

/**
 * No op implementation of {@link SlackClient}
 */
public class NopSlackClientImpl implements SlackClient {

    /**
     * No op implementation - returns hardcoded {@link PostMessageResponse} stub.
     */
    @Override
    public PostMessageResponse sendMessage(PostMessage postMessage) {
        info().data("message", postMessage.getText()).log("NopSlackClientImpl.sendMessage");
        return new PostMessageResponse(true, "stubbed response","");
    }

    /**
     * No op implementation - returns hardcoded {@link PostMessageResponse} stub.
     */
    @Override
    public PostMessageResponse updateMessage(PostMessage postMessage) {
        info().data("message", postMessage.getText()).log("NopSlackClientImpl.updateMessage");
        return new PostMessageResponse(true, "stubbed response","");
    }

    @Override
    public Profile getProfile() {
        return null;
    }
}