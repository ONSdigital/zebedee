package com.github.onsdigital.zebedee.search.indexing;

import junit.framework.TestCase;
import org.junit.Test;

import java.io.IOException;

/**
 * Created by bren on 25/01/16.
 */
public class SearchBoostTest extends TestCase {

    @Test
    public void test() throws IOException {
        SearchBoost.readFile("/boost-test.txt");
        String[] terms = SearchBoost.getTerms("/economy/grossdomesticproductgdp/bulletins/grossdomesticproductpreliminaryestimate/2015-10-27");
        assertTrue(terms.length == 2);
        assertEquals(terms[0], "preliminary");
    }

}
