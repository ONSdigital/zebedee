package com.github.onsdigital.zebedee.json;

import java.util.List;

/**
 * Class to hold a file uri and any other properties required from that file.
 */
public class ContentDetail {
    public String uri;
    public String name;
    public String type;

    public List<ContentDetail> children;
}
