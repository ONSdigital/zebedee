package com.github.onsdigital.zebedee.model.approval;

import com.github.onsdigital.zebedee.json.ContentDetail;
import com.github.onsdigital.zebedee.model.Content;
import com.github.onsdigital.zebedee.reader.ContentReader;

import java.util.List;

@FunctionalInterface
public interface ContentDetailResolver {

    List<ContentDetail> resolve(Content reviewedContent, ContentReader reviewedContentReader) throws Exception;
}
