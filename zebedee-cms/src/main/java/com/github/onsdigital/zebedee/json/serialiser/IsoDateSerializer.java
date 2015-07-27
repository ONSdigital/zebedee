package com.github.onsdigital.zebedee.json.serialiser;

import com.google.gson.*;
import org.apache.commons.lang3.time.FastDateFormat;

import java.lang.reflect.Type;
import java.sql.Timestamp;
import java.text.ParseException;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Configure GsonBuilder to force used of ISO-8601 format with UTC timezone
 * https://code.google.com/p/google-gson/issues/detail?id=281
 */
public class IsoDateSerializer implements JsonSerializer<Date>, JsonDeserializer<Date> {

    private final FastDateFormat iso8601Format;

    public IsoDateSerializer() {
        this.iso8601Format = FastDateFormat.getInstance("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", TimeZone.getTimeZone("UTC"), Locale.UK);
    }

    public JsonElement serialize(Date src, Type typeOfSrc, JsonSerializationContext context) {
        String dateFormatAsString = iso8601Format.format(src);
        return new JsonPrimitive(dateFormatAsString);
    }

    public Date deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {
        if (!(json instanceof JsonPrimitive)) {
            throw new JsonParseException("The date should be a string value");
        }
        Date date = deserializeToDate(json);
        if (typeOfT == Date.class) {
            return date;
        } else if (typeOfT == Timestamp.class) {
            return new Timestamp(date.getTime());
        } else if (typeOfT == java.sql.Date.class) {
            return new java.sql.Date(date.getTime());
        } else {
            throw new IllegalArgumentException(getClass() + " cannot deserialize to " + typeOfT);
        }
    }

    private Date deserializeToDate(JsonElement json) {
        try {
            return iso8601Format.parse(json.getAsString());
        } catch (ParseException e) {
            throw new JsonSyntaxException(json.getAsString(), e);
        }
    }
}