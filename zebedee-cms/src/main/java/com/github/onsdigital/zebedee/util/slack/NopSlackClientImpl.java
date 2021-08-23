package com.github.onsdigital.zebedee.util.slack;

import com.github.onsdigital.slack.Profile;
import com.github.onsdigital.slack.client.PostMessageResponse;
import com.github.onsdigital.slack.client.SlackClient;
import com.github.onsdigital.slack.messages.PostMessage;

public class NopSlackClientImpl implements SlackClient {

    @Override
    public PostMessageResponse sendMessage(PostMessage postMessage) {
        return null;
    }

    @Override
    public PostMessageResponse updateMessage(PostMessage postMessage) {
        return null;
    }

    @Override
    public Profile getProfile() {
        return null;
    }
}
