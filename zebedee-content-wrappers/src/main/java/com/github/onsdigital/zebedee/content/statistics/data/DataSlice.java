package com.github.onsdigital.zebedee.content.statistics.data;

import com.github.onsdigital.zebedee.content.base.ContentType;
import com.github.onsdigital.zebedee.content.statistics.data.base.StatisticalData;

/**
 * Created by bren on 05/06/15.
 */
public class DataSlice extends StatisticalData {

    @Override
    public ContentType getType() {
        return ContentType.data_slice;
    }

}
