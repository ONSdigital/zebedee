package com.github.onsdigital.zebedee;

import java.util.ArrayList;

public class Collections extends ArrayList<Collection> {

    public Collection getCollection(String name) {
        Collection result = null;

        if (name.length() > 0) {

            for (Collection collection : this) {
                if (collection.description.name.equals(name)) {
                    result = collection;
                }
            }
        }

        return result;
    }


}
