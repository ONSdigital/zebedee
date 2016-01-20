package com.github.onsdigital.zebedee.util;

import com.github.davidcarboni.httpino.Serialiser;
import com.google.gson.JsonSyntaxException;

import java.io.IOException;
import java.io.InputStream;

public class JsonUtils {

    public static boolean isValidJson(InputStream inputStream) {
        try {
            Serialiser.deserialise(inputStream, Object.class);
            return true;
        } catch (IOException | JsonSyntaxException e) {
            return false;
        }
    }
}
