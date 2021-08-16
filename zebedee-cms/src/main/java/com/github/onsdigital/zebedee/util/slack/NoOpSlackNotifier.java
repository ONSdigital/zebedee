package com.github.onsdigital.zebedee.util.slack;

import com.github.onsdigital.slack.messages.PostMessage;
import com.github.onsdigital.zebedee.model.Collection;

public class NoOpSlackNotifier implements Notifier {

    @Override
    public void collectionAlarm(Collection c, String alarm, PostMessageField... args) {

    }

    @Override
    public void alarm(String alarm, PostMessageField... args) {

    }

    @Override
    public void sendSlackMessage(PostMessage message) throws Exception {

    }

    @Override
    public PostMessage createPostMessage(String channel, String text) {
        return null;
    }

    @Override
    public boolean sendCollectionAlarm(Collection c, String channel, String message, Exception ex) {
        return false;
    }

    @Override
    public boolean sendCollectionWarning(Collection c, String channel, String message, AttachmentField... attachments) {
        return false;
    }

    @Override
    public boolean sendCollectionAlarm(Collection c, String channel, String message, AttachmentField... attachments) {
        return false;
    }
}
