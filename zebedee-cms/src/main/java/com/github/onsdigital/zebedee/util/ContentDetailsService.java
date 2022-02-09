package com.github.onsdigital.zebedee.util;

import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.json.ContentDetail;
import com.github.onsdigital.zebedee.model.Content;
import com.github.onsdigital.zebedee.reader.ContentReader;

import java.io.IOException;

public interface ContentDetailsService {
    ContentDetail resolveDetails(Content content, ContentReader reader) throws IOException, ZebedeeException;
}