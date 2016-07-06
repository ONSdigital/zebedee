package com.github.onsdigital.zebedee.model;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.util.List;

/**
 * POJO representing a click event - a click event is any UI click action in Florence.
 */
public class ClickEvent {

    private String user;
    private Trigger trigger;
    private Collection collection;

    public ClickEvent setUser(String user) {
        this.user = user;
        return this;
    }

    public ClickEvent setTrigger(Trigger trigger) {
        this.trigger = trigger;
        return this;
    }

    public ClickEvent setCollection(Collection collection) {
        this.collection = collection;
        return this;
    }

    /**
     * @return the user who caused the event.
     */
    public String getUser() {
        return user;
    }

    /**
     * @return details about the event.
     */
    public Trigger getTrigger() {
        return trigger;
    }

    /**
     * @return details about the collection the event was created in.
     */
    public Collection getCollection() {
        return collection;
    }

    @Override
    public boolean equals(Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.JSON_STYLE);
    }

    /**
     * POJO encapsulating the details that triggered the event.
     */
    public static class Trigger {

        /**
         * The HTML element ID.
         */
        private String elementId;
        /**
         * The CSS / JS classes on the element.
         */
        private List<String> elementClasses;

        public String getElementId() {
            return elementId;
        }

        public List<String> getElementClasses() {
            return elementClasses;
        }

        public Trigger setElementId(String elementId) {
            this.elementId = elementId;
            return this;
        }

        public Trigger setElementClasses(List<String> elementClasses) {
            this.elementClasses = elementClasses;
            return this;
        }

        @Override
        public boolean equals(Object obj) {
            return EqualsBuilder.reflectionEquals(this, obj);
        }

        @Override
        public int hashCode() {
            return HashCodeBuilder.reflectionHashCode(this);
        }

        @Override
        public String toString() {
            return ToStringBuilder.reflectionToString(this, ToStringStyle.JSON_STYLE);
        }

    }

    /**
     * POJO encapsulating details about the {@link com.github.onsdigital.zebedee.model.Collection} being worked on
     * while the event took place.
     */
    public static class Collection {
        private String id;
        private String name;
        private String type;

        public String getId() {
            return id;
        }

        public Collection setId(String id) {
            this.id = id;
            return this;
        }

        public Collection setType(String type) {
            this.type = type;
            return this;
        }

        public Collection setName(String name) {
            this.name = name;
            return this;
        }

        public String getName() {
            return name;
        }

        public String getType() {
            return type;
        }

        @Override
        public boolean equals(Object obj) {
            return EqualsBuilder.reflectionEquals(this, obj);
        }

        @Override
        public int hashCode() {
            return HashCodeBuilder.reflectionHashCode(this);
        }

        @Override
        public String toString() {
            return ToStringBuilder.reflectionToString(this, ToStringStyle.JSON_STYLE);
        }

    }
}
