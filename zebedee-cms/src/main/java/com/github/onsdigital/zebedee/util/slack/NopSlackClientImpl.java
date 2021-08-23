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

    @Override
    public PostMessageResponse sendMessage(PostMessage postMessage) {
        info().data("message", postMessage.getText()).log("NopSlackClientImpl.sendMessage");
        return null;
    }

    @Override
    public PostMessageResponse updateMessage(PostMessage postMessage) {
        info().data("message", postMessage.getText()).log("NopSlackClientImpl.updateMessage");
        return null;
    }

    @Override
    public Profile getProfile() {
        return null;
    }
}