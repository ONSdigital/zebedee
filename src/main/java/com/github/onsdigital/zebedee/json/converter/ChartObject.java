package com.github.onsdigital.zebedee.json.converter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Created by thomasridd on 13/05/15.
 */
public class ChartObject {
    public String title;
    public String subtitle;
    public String unit;
    public String source;

    public List<String> series;
    public LinkedHashMap<String,HashMap<String, String>> data;

    public ChartObject() {
        series = new ArrayList<>();
        data = new LinkedHashMap<>();
    }
}
