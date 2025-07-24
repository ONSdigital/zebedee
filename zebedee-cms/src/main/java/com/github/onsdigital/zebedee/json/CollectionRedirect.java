package com.github.onsdigital.zebedee.json;

import java.util.Objects;

/**
 * Represents a redirect that has been added to a collection.
 */
public class CollectionRedirect {

    private String from;
    private String to;
    private CollectionRedirectAction action;

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public CollectionRedirectAction getAction() {
        return action;
    }

    public void setAction(CollectionRedirectAction action) {
        this.action = action;
    }

    public CollectionRedirect(String from, String to, CollectionRedirectAction action) {
        this.from = from;
        this.to = to;
        this.action = action;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CollectionRedirect that = (CollectionRedirect) o;

        return from.equals(that.getFrom()) &&
            to.equals(that.getTo()) &&
            action.equals(that.getAction());
    }

    @Override
    public int hashCode() {
        return Objects.hash(from, to, action);
    }

}
