package com.github.onsdigital.zebedee.content.page.statistics.data;

import com.github.onsdigital.zebedee.content.page.base.PageType;
import com.github.onsdigital.zebedee.content.page.statistics.data.base.StatisticalData;

/**
 * Created by bren on 05/06/15.
 */
public class DataSlice extends StatisticalData {

    @Override
    public PageType getType() {
        return PageType.data_slice;
    }

}
