package com.github.onsdigital.zebedee;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;

public class Collections extends ArrayList<Collection> {

    public Collection getCollection(String name) {
        Collection result = null;

        if (StringUtils.isNotBlank(name)) {

            String filename = PathUtils.toFilename(name);
            for (Collection collection : this) {
                String collectionFilename = collection.path.getFileName().toString();
                if (StringUtils.equalsIgnoreCase(collectionFilename,filename)) {
                    result = collection;
                }
            }
        }

        return result;
    }


}
