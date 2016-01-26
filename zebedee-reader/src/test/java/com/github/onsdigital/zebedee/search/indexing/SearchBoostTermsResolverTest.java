package com.github.onsdigital.zebedee.search.indexing;

import junit.framework.TestCase;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

/**
 * Created by bren on 25/01/16.
 */
public class SearchBoostTermsResolverTest extends TestCase {

    @Test
    public void test() throws IOException {
        SearchBoostTermsResolver.loadTerms("/boost-test.txt");
        List<String> terms = SearchBoostTermsResolver.getSearchTermResolver().getTerms("/economy/grossdomesticproductgdp/bulletins/grossdomesticproductpreliminaryestimate/2015-10-27");
        assertTrue(terms.size() == 2);
        assertEquals(terms.get(0), "preliminary");
        assertEquals(terms.get(1), "estimates");
    }

}
