package com.github.onsdigital.zebedee.util.serialiser;

import com.github.davidcarboni.restolino.json.Serialiser;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

import static com.github.onsdigital.logging.v2.event.SimpleEvent.error;

/**
 * Created by dave on 04/07/2017.
 */
public class JSONSerialiser<T> {

    protected static final String DESERIALISE_ERROR_DETAILS_MSG = "Warning Corrupt JSON file encountered. JSON could " +
            "not be deserialised. It is highly recommended that you investigate the cause of this issue";

    protected static final String DESERIALISE_ERROR_DETAILS_KEY = "details";
    protected static final String DESERIALISATION_ERROR_MSG = "Failed to deserialise JSON";

    protected Class<T> t;

    public JSONSerialiser(Class<T> t) {
        this.t = t;
    }

    public T deserialiseQuietly(Path p) {
        try {
            return Serialiser.deserialise(p, t);
        } catch (Exception e) {
            error().data(DESERIALISE_ERROR_DETAILS_KEY, DESERIALISE_ERROR_DETAILS_MSG)
                    .data("path", p.toString())
                    .logException(e, DESERIALISATION_ERROR_MSG);
            return null;
        }
    }

    public T deserialiseQuietly(InputStream inputStream, Path p) {
        try {
            return Serialiser.deserialise(inputStream, t);
        } catch (Exception e) {
            error().data(DESERIALISE_ERROR_DETAILS_KEY, DESERIALISE_ERROR_DETAILS_MSG)
                    .data("path", p.toString())
                    .logException(e, DESERIALISATION_ERROR_MSG);
            return null;
        }
    }

    public T deserialise(Path p) throws IOException {
        return Serialiser.deserialise(p, t);
    }

    public void serialise(Path p, T t) throws IOException {
        Serialiser.serialise(p, t);
    }
}
