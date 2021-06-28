package com.github.onsdigital.zebedee.util.slack;

/**
 * AttachmentField returns necessary information such as title and message about a specific collection.
 */
public class AttachmentField {
    private String title;
    private String message;
    private boolean isShort;

    /**
     * @param title   - the string title to apply to the collection
     * @param message - the string message to apply to the collection.
     * @param isShort - boolean to wrap text
     */
    public AttachmentField(String title, String message, boolean isShort) {
        this.title = title;
        this.message = message;
        this.isShort = isShort;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public boolean isShort() {
        return isShort;
    }
}
