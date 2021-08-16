package com.github.onsdigital.zebedee.util.slack;

import com.github.onsdigital.slack.Profile;
import com.github.onsdigital.slack.client.PostMessageResponse;
import com.github.onsdigital.slack.client.SlackClient;
import com.github.onsdigital.slack.messages.PostMessage;

import static com.github.onsdigital.zebedee.logging.CMSLogEvent.info;

public class NoopSlackClientImpl implements SlackClient {

    @Override
    public PostMessageResponse sendMessage(PostMessage postMessage) {
        info().data("message", postMessage).log("noop-slackClient sendMessage");
        return null;
    }

    @Override
    public PostMessageResponse updateMessage(PostMessage postMessage) {
        info().data("message", postMessage).log("noop-slackClient updateMessage");
        return null;
    }

    @Override
    public Profile getProfile() {
        return null;
    }
}
