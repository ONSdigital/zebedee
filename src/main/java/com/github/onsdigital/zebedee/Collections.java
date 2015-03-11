package com.github.onsdigital.zebedee;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;

public class Collections extends ArrayList<Collection> {

    /**
     * Retrieves a collection with the given name.
     *
     * @param name The name to look for.
     * @return If a {@link Collection} matching the given name exists,
     * (according to {@link PathUtils#toFilename(String)}) the collection.
     * Otherwise null.
     */
    public Collection getCollection(String name) {
        Collection result = null;

        if (StringUtils.isNotBlank(name)) {

            String filename = PathUtils.toFilename(name);
            for (Collection collection : this) {
                String collectionFilename = collection.path.getFileName().toString();
                if (StringUtils.equalsIgnoreCase(collectionFilename, filename)) {
                    result = collection;
                    break;
                }
            }
        }

        return result;
    }

    /**
     * Determines whether a collection with the given name exists.
     *
     * @param name The name to check for.
     * @return If {@link #getCollection(String)} returns non-null, true.
     */
    public boolean hasCollection(String name) {
        return getCollection(name) != null;
    }
}
