package com.github.onsdigital.zebedee;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class CollectionsTest {

    Zebedee zebedee;
    Builder builder;

    @Before
    public void setUp() throws Exception {
        builder = new Builder(this.getClass());
        zebedee = new Zebedee(builder.zebedee);
    }

    @After
    public void tearDown() throws Exception {
        builder.delete();
    }

    @Test
    public void shouldFindCollection() throws IOException {
        Collections collections = new Collections();

        Collection firstCollection = Collection.create("FirstCollection", zebedee);
        Collection secondCollection = Collection.create("SecondCollection", zebedee);

        collections.add(firstCollection);
        collections.add(secondCollection);

        assertEquals("FirstCollection", collections.getCollection("FirstCollection").description.name);
        assertEquals("SecondCollection", collections.getCollection("SecondCollection").description.name);
    }

    @Test
    public void shouldReturnNullIfNotFound() throws IOException {

        Collections collections = new Collections();

        Collection firstCollection = Collection.create("FirstCollection", zebedee);

        collections.add(firstCollection);

        assertNull(collections.getCollection("SecondCollection"));
    }
}
