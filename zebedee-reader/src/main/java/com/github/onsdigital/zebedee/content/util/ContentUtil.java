package com.github.onsdigital.zebedee.content.util;

import com.github.onsdigital.zebedee.content.base.Content;
import com.github.onsdigital.zebedee.content.page.base.Page;
import com.github.onsdigital.zebedee.logging.ZebedeeReaderLogBuilder;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Date;

/**
 * Created by bren on 06/06/15.
 * <p>
 * Several utilities to serialise/deserialize utility with date format supports.
 * <p>
 * Uses ISO Date Time format with time zone as default date format for date fields. (e.g. 1 January 2015, 10 February 2015)
 */
public class ContentUtil {

    /**
     * Returns json string for given object
     *
     * @return json string
     */
    public static String serialise(Object object) {
        return gson().toJson(object);
    }


    /**
     * Returns json string for given object using a custom date format
     *
     * @return json string
     */
    public static String serialise(Object object, String datePattern) {
        return gson(datePattern).toJson(object);
    }


    /**
     * Deserialises json string into given Object type
     *
     * @param json json to be deserialised
     * @param type
     * @return
     */
    public static <O extends Object> O deserialise(String json, Class<O> type) {
        return gson().fromJson(json, type);
    }


    /**
     * Deserialises json string into given Object type using given date pattern
     *
     * @param json json to be deserialised
     * @param type
     * @return
     */
    public static <O extends Object> O deserialise(String json, Class<O> type, String datePattern) {
        return gson(datePattern).fromJson(json, type);
    }


    /**
     * Deserialises json stream into given Object type
     *
     * @param stream json stream to be deserialised
     * @param type
     * @return
     */


    public static <O extends Object> O deserialise(InputStream stream, Class<O> type) {
        return gson().fromJson(new InputStreamReader(stream), type);
    }


    /**
     * Deserialises json stream into given Object type
     *
     * @param stream      json stream to be deserialised
     * @param type
     * @param datePattern
     * @return
     */


    public static <O extends Object> O deserialise(InputStream stream, Class<O> type, String datePattern) {
        return gson(datePattern).fromJson(new InputStreamReader(stream), type);
    }

    /**
     * Resolves page type and deserializes automatically to that implementation. Use if you do not need to know actual class implementation
     *
     * @param stream json stream
     * @return
     */
    public static Page deserialiseContent(InputStream stream) {
        try (InputStreamReader inputStreamReader = new InputStreamReader(stream)) {
            return createBuilder(ContentConstants.JSON_DATE_PATTERN)
                    .registerTypeAdapter(Page.class, new PageTypeResolver())
                    .create().fromJson(inputStreamReader, Page.class);
        } catch (IOException ex) {
            ZebedeeReaderLogBuilder.logError(ex, "Failed to close inputstream reader.").log();
            throw new RuntimeException(ex);
        }
    }


    /**
     * Resolves page type and deserializes automatically to that implementation. Use if you do not need to know actual class implementation,
     *
     * @param stream json stream
     * @param datePattern date pattern to be used when deserialising
     * @return
     */
    public static Page deserialiseContent(InputStream stream, String datePattern) {
        return createBuilder(datePattern).registerTypeAdapter(Page.class, new PageTypeResolver()).create().fromJson(new InputStreamReader(stream), Page.class);
    }


    /**
     * Resolves page type and deserializes automatically to that implementation. Use if you do not need to know actual class implementation
     *
     * @param json
     * @return
     */
    public static Page deserialiseContent(String json) {
        return createBuilder(ContentConstants.JSON_DATE_PATTERN).registerTypeAdapter(Page.class, new PageTypeResolver()).create().fromJson(json, Page.class);
    }

    /**
     * Resolves page type and deserializes automatically to that implementation. Use if you do not need to know actual class implementation
     *
     * @param json
     * @param datePattern
     * @return
     */
    public static Page deserialiseContent(String json, String datePattern) {
        return createBuilder(datePattern).registerTypeAdapter(Page.class, new PageTypeResolver()).create().fromJson(json, Page.class);
    }

    public static String hash(Content content) {
        return DigestUtils.sha1Hex(ContentUtil.serialise(content));
    }

    public static String hash(InputStream stream) throws IOException {
        return DigestUtils.sha1Hex(stream);
    }
    public static String hash(byte[] bytes) throws IOException {
        return DigestUtils.sha1Hex(bytes);
    }

    /**
     * Clones given object and returns a new copy
     *
     * @param o   object to be cloned
     * @param <O> copy of given object
     * @return
     */
    public static <O extends Cloneable> O clone(O o) {
        Cloneable cloneable = o;
        return ObjectUtils.clone(o);
    }

    private static Gson gson() {
        return gson(null);
    }

    private static Gson gson(String datePattern) {
        GsonBuilder builder = createBuilder(datePattern);
        return builder.create();
    }

    private static GsonBuilder createBuilder(String datePattern) {
        GsonBuilder builder = new GsonBuilder();
        if (StringUtils.isNotBlank(datePattern)) {
            builder.registerTypeAdapter(Date.class, new IsoDateSerializer(datePattern));
        } else {
            builder.registerTypeAdapter(Date.class, new IsoDateSerializer(ContentConstants.JSON_DATE_PATTERN));
        }
        return builder;
    }
}
