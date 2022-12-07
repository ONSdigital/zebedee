package com.github.onsdigital.zebedee.reader.api.endpoint;


import com.github.onsdigital.zebedee.content.dynamic.browse.ContentNode;
import com.github.onsdigital.zebedee.reader.FakeCollectionReaderFactory;
import com.github.onsdigital.zebedee.reader.ZebedeeReader;
import com.github.onsdigital.zebedee.reader.api.ReadRequestHandler;
import com.github.onsdigital.zebedee.reader.configuration.ReaderConfiguration;
import junit.framework.TestCase;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import javax.servlet.http.HttpServletRequest;
import java.util.Collection;

@RunWith(MockitoJUnitRunner.class)
public class Taxonomy2Test extends TestCase {
    private static final String COLLECTION_ID = "testcollection-3ff3cfe6437e1b2e8fdde2a664cd3f9de239dd99e9acb109557f4a1089e48bd7";
    private static final String SESSION_ID = "SESSION.ID";

    private ReadRequestHandler handler;

    @Mock
    private HttpServletRequest request;

    @BeforeClass
    public static void beforeClass() {
        ReaderConfiguration cfg = ReaderConfiguration.init("target/test-classes/test-content/");
        ZebedeeReader.setCollectionReaderFactory(new FakeCollectionReaderFactory(cfg.getCollectionsDir()));
    }

    @AfterClass
    public static void afterClass() {
        ZebedeeReader.setCollectionReaderFactory(null);
    }

    @Before
    public void initialize() {
        handler = new ReadRequestHandler();
    }



    @Test
    public void testTaxonomy() throws Exception {

        Collection<ContentNode> children = handler.getTaxonomy(request, 1);
        assertTrue(children.size() == 5);


    }
}