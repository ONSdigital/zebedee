package com.github.onsdigital.zebedee;

import com.github.davidcarboni.restolino.helpers.Path;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Collections extends ArrayList<Collection> {

    public Collection getCollection(HttpServletRequest request)
            throws IOException {

        Path path = Path.newInstance(request);
        List<String> segments = path.segments();

        String collectionName = "";
        if (segments.size() > 1) {
            collectionName = segments.get(1);
        }

        for (String segment : path.segments()) {
            System.out.println(" - " + segment);
        }

        return getCollection(collectionName);
    }

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
