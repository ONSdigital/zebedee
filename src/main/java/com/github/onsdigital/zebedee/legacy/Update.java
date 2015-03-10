package com.github.onsdigital.zebedee.legacy;

import com.github.davidcarboni.restolino.json.Serialiser;
import com.github.onsdigital.zebedee.Collection;
import com.github.onsdigital.zebedee.api.Collections;
import com.github.onsdigital.zebedee.json.Item;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.POST;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Created by david on 06/03/2015.
 */
//@Api
public class Update {

    @POST
    public boolean update(HttpServletRequest request, HttpServletResponse response, Item item) throws IOException {
        boolean result = false;

        if (item != null && item.json != null) {
            Collection collection = Collections.getCollection(request) ;
            Path path = collection.find(item.uri);
            try (OutputStream outputStream = Files.newOutputStream(path)) {
                Serialiser.serialise(outputStream, item.json);
                result = true;
            }
        }

        return result;
    }
}
