package com.github.onsdigital.zebedee.reader.util;

import com.github.onsdigital.zebedee.reader.data.language.ContentLanguage;

import javax.servlet.http.HttpServletRequest;

/**
 * Created by bren on 20/08/15.
 */
public class ReaderRequestUtils {

    /**
     * Gets lang parameter passed with request
     *
     * @return
     */
    public static ContentLanguage getRequestedLanguage(HttpServletRequest request) {
        String lang = request.getParameter("lang");
        if (lang == null) {
            return null;
        }

        try {
            return ContentLanguage.valueOf(lang);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
