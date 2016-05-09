package com.github.onsdigital.zebedee.data.processing.setup;

import com.github.onsdigital.zebedee.data.processing.DataIndex;
import com.github.onsdigital.zebedee.reader.ContentReader;

public class DataIndexBuilder {
    public static DataIndex buildDataIndex(ContentReader contentReader) throws InterruptedException {
        DataIndex dataIndex = new DataIndex(contentReader);

        while (!dataIndex.isIndexBuilt()) {
            Thread.sleep(1000);
        }
        return dataIndex;
    }
}
