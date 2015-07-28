package com.github.onsdigital.zebedee.json.converter;

import org.apache.commons.lang.ArrayUtils;

import java.util.*;

/**
 * Created by thomasridd on 13/05/15.
 */
public class ChartObject {
    public String title;
    public String subtitle;
    public String unit;
    public String source;
    private String _categoryKey = null;

    public List<String> series;
    public List<HashMap<String, String>> data;

    public ChartObject() {
        series = new ArrayList<>();
        data = new ArrayList<>();
    }

    // works out the data dictionary key
    public String categoryKey() {
        if(_categoryKey == null) {
            if(data.size() > 0) {
                Set<String> allKeys = data.get(0).keySet();
                String[] seriesNames = series.toArray(new String[series.size()]);

                for(String key: allKeys) {
                    if(!ArrayUtils.contains(seriesNames, key)) {
                        _categoryKey = key;
                        return _categoryKey;
                    }
                }
            }
        }
        return _categoryKey;
    }
}
