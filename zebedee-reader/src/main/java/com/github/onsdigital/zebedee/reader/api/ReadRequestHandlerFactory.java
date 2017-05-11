package com.github.onsdigital.zebedee.reader.api;

import com.github.onsdigital.zebedee.reader.data.language.ContentLanguage;

/**
 * Created by dave on 09/05/2017.
 */
@FunctionalInterface
public interface ReadRequestHandlerFactory {

    ReadRequestHandler get(ContentLanguage language);
}
