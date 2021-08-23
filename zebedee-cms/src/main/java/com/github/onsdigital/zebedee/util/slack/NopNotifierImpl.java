package com.github.onsdigital.zebedee.util.slack;

import com.github.onsdigital.slack.messages.PostMessage;
import com.github.onsdigital.zebedee.model.Collection;

import static com.github.onsdigital.zebedee.logging.CMSLogEvent.info;

/**
 * No op impl of {@link Notifier}
 */
public class NopNotifierImpl implements Notifier {

    @Override
    public void collectionAlarm(Collection c, String alarm, PostMessageField... args) {
        info().collectionID(c).data("message", alarm).log("NopNotifierImpl collectionAlaram");
    }

    @Override
    public void alarm(String alarm, PostMessageField... args) {
        info().data("message", alarm).log("NopNotifierImpl alarm");
    }

    @Override
    public void sendSlackMessage(PostMessage message) throws Exception {
        info().data("message", message.getText()).log("NopNotifierImpl sendSlackMessage");
    }

    @Override
    public PostMessage createPostMessage(String channel, String text) {
        return null;
    }

    @Override
    public boolean sendCollectionAlarm(Collection c, String channel, String message, Exception ex) {
        info().collectionID(c).data("message", message).log("NopNotifierImpl sendCollectionAlarm");
        return false;
    }

    @Override
    public boolean sendCollectionWarning(Collection c, String channel, String message, AttachmentField... attachments) {
        info().collectionID(c).data("message", message).log("NopNotifierImpl sendCollectionWarning");
        return false;
    }

    @Override
    public boolean sendCollectionAlarm(Collection c, String channel, String message, AttachmentField... attachments) {
        info().collectionID(c).data("message", message).log("NopNotifierImpl sendCollectionAlarm");
        return false;
    }
}
