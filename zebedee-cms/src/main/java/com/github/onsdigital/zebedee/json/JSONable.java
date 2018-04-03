package com.github.onsdigital.zebedee.json;

/**
 * A object that can returned a JSON string representation of its self.
 */
@FunctionalInterface
public interface JSONable {

    /**
     * @return the object as a JSON string.
     */
    String toJSON();
}
