package com.github.onsdigital.zebedee.util;

import org.apache.commons.lang3.StringUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Converts between Java {@link Date} and Strings suitable for use in Json which can be interpreted by Date.parse() in Javascript.
 */
public class DateConverter {

    static final String pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";

    /**
     * @param date A {@link Date} to be converted to a {@link String}. Can be null.
     * @return A String matching the format pattern {@value #pattern}, or null if the input was null.
     */
    public static String toString(Date date) {
        String result = null;
        if (date != null) {
            result = new SimpleDateFormat(pattern).format(date);
        }
        return result;
    }

    /**
     * @param date A {@link String} to be converted to a {@link Date}. Can be null.
     * @return A Date parsed using the format pattern {@value #pattern}, or null if the input was null.
     */
    public static Date toDate(String date) {
        Date result = null;
        if (StringUtils.isNotBlank(date)) {
            try {
                result = new SimpleDateFormat(pattern).parse(date);
            } catch (ParseException e) {
                // Leave the result null
            }
        }
        return result;
    }
}
