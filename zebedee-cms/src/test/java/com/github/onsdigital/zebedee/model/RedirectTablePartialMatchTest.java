package com.github.onsdigital.zebedee.model;

import com.github.davidcarboni.ResourceUtils;
import com.github.onsdigital.zebedee.Builder;
import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.ZebedeeTestBaseFixture;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.nio.file.Path;

import static org.junit.Assert.*;

/**
 * Created by thomasridd on 17/08/15.
 */
public class RedirectTablePartialMatchTest extends ZebedeeTestBaseFixture {

    @Override
    public void setUp() throws Exception {
        builder = new Builder(ResourceUtils.getPath("/bootstraps/basic"));
        zebedee = builder.getZebedee();
    }

    @Test
    public void redirectTable_whenSetup_shouldNotBeNull() {
        // Given
        // Content to set up the redirect
        RedirectTable table = new RedirectTablePartialMatch(zebedee.getPublished());

        // When
        // We initialise a redirect

        // Then
        // It should not be null
        assertNotNull(table);
    }

    @Test
    public void get_forExistingContent_shouldReturnUri() {
        // Given
        // a table with a redirect
        RedirectTable table = new RedirectTablePartialMatch(zebedee.getPublished());

        // When
        // We get the redirect
        String direct = table.get("themea/data.json");

        // Then
        // It should not be null
        assertNotNull(direct);
        assertEquals("themea/data.json", direct);
    }

    @Test
    public void get_forRedirectWhereContentExists_shouldReturnExistingUri() {
        // Given
        // a table with a redirect from existing data
        RedirectTable table = new RedirectTablePartialMatch(zebedee.getPublished());
        table.addRedirect("themea/data.json", "themeb/data.json");

        // When
        // We get the redirect
        String redirect = table.get("themea/data.json");

        // Then
        // We expect the get to return the existing data
        assertNotNull(redirect);
        assertEquals("themea/data.json", redirect);
    }

    @Test
    public void get_whereRedirectExistsToContent_shouldRedirect() {
        // Given
        // a table with a redirect to real content
        RedirectTable table = new RedirectTablePartialMatch(zebedee.getPublished());
        table.addRedirect("redirect/data.json", "themea/data.json");

        // When
        // We get the redirect
        String redirected = table.get("redirect/data.json");

        // Then
        // It should not be null
        assertNotNull(redirected);
        assertEquals("themea/data.json", redirected);
    }

    @Test
    public void get_whereRedirectContentDoesntExist_shouldReturnNull() {
        // Given
        // a table with a redirect to something that doesn't exist
        RedirectTable table = new RedirectTablePartialMatch(zebedee.getPublished());
        table.addRedirect("redirect/data.json", "does/not/exist/data.json");

        // When
        // We get the redirect
        String redirected = table.get("redirect/data.json");

        // Then
        // It should be null
        assertNull(redirected);
    }

    @Test
    public void get_whereMultipleRedirectsExistFromPartialMatchOrigin_shouldRedirect() {
        // Given
        // a quite complicated situation...
        RedirectTable table = new RedirectTablePartialMatch(zebedee.getPublished());

        // Take a top level node and move it
        table.addRedirect("business", "themea");
        // Recreate the top level node (not necessary in test) and move that
        table.addRedirect("business", "themeb");

        // When
        // We get the redirects from two node that have moved
        String pageThatBelongedToTheOriginalBusiness = table.get("business/landinga/data.json");
        String pageThatBelongedToTheSecondBusiness = table.get("business/landingc/data.json");

        // Then
        // The appropriate links work even though they are to different places
        assertEquals("themea/landinga/data.json", pageThatBelongedToTheOriginalBusiness);
        assertEquals("themeb/landingc/data.json", pageThatBelongedToTheSecondBusiness);

    }

    @Test
    public void add_redirectToEmptyTable_storesRedirect() {
        // Given
        // A standard setup
        RedirectTable table = new RedirectTablePartialMatch(zebedee.getPublished());

        // When
        // We add a link
        table.addRedirect("alpha", "themea");

        // Then
        // We expect the table to have a link
        assertTrue(table.exists("alpha", "themea"));
    }

    @Test
    public void addSecondRedirect_FromSameOrigin_storesBothRedirects() {
        // Given
        // A standard setup
        RedirectTable table = new RedirectTablePartialMatch(zebedee.getPublished());

        // When
        // We add two links from the same origin
        table.addRedirect("alpha", "themea");
        table.addRedirect("alpha", "themeb");

        // Then
        // We expect the table to have both links
        assertTrue(table.exists("alpha", "themea"));
        assertTrue(table.exists("alpha", "themeb"));
    }

    @Test
    public void addSecondRedirect_ThatNullifiesExisting_updatesTheRedirect() {
        // Given
        // A standard setup with a redirect
        RedirectTable table = new RedirectTablePartialMatch(zebedee.getPublished());
        table.addRedirect("custard tarts", "apples");

        // When
        // We add a link from the beta to somewhere new
        table.addRedirect("apples", "rice cakes");

        // Then
        // We expect alpha to redirect to gamma
        assertTrue(table.exists("custard tarts", "rice cakes"));
    }

    @Test
    public void addSecondRedirect_ThatImpactsExisting_updatesTheRedirect() {
        // Given
        // A standard setup with a redirect
        RedirectTable table = new RedirectTablePartialMatch(zebedee.getPublished());
        table.addRedirect("carrot/cake", "poached/pears");

        // When
        // we change our mind and update poached to caramelised
        table.addRedirect("poached", "caramelised");

        // Then
        // The redirect has been updated
        assertFalse(table.exists("carrot/cake", "poached/pears"));
        assertTrue(table.exists("carrot/cake", "caramelised/pears"));
    }

    @Test
    public void addSecondRedirect_ThatImpactsExistingBuriedInAMultidirect_updatesTheRedirect() {
        // Given
        // A standard setup with a redirect
        RedirectTable table = new RedirectTablePartialMatch(zebedee.getPublished());
        table.addRedirect("carrot/cake", "sloe gin/eton mess");
        table.addRedirect("carrot/cake", "stewed/rhubarb");
        table.addRedirect("carrot/cake", "poached/pears");

        // When
        // we change our mind and update poached to caramelised
        table.addRedirect("poached", "caramelised");

        // Then
        // The redirect has been updated
        assertFalse(table.exists("carrot/cake", "poached/pears"));
        assertTrue(table.exists("carrot/cake", "caramelised/pears"));
    }

    @Test
    public void mergeTable_intoCurrentTable_addsLinksIntoCurrentTable() {
        // Given
        // A couple of redirect table
        RedirectTable currentTable = new RedirectTablePartialMatch(zebedee.getPublished());
        currentTable.addRedirect("beef", "lentils");
        currentTable.addRedirect("chicken", "tofu");

        RedirectTable mergeTable = new RedirectTablePartialMatch(zebedee.getPublished());
        mergeTable.addRedirect("lamb", "chickpea");
        mergeTable.addRedirect("bacon", "egg");

        // When
        // we merge into currentTable (in git fashion)
        currentTable.merge(mergeTable);

        // Then
        // Current table has been updated
        assertTrue(currentTable.exists("beef", "lentils"));
        assertTrue(currentTable.exists("chicken", "tofu"));
        assertTrue(currentTable.exists("lamb", "chickpea"));
        assertTrue(currentTable.exists("bacon", "egg"));
    }

    @Test
    public void mergeTable_intoCurrentTable_doesntUpdateMergeTable() {
        // Given
        // A couple of redirect table
        RedirectTable currentTable = new RedirectTablePartialMatch(zebedee.getPublished());
        currentTable.addRedirect("beef", "lentils");
        currentTable.addRedirect("chicken", "tofu");

        RedirectTable mergeTable = new RedirectTablePartialMatch(zebedee.getPublished());
        mergeTable.addRedirect("lamb", "chickpea");
        mergeTable.addRedirect("bacon", "egg");

        // When
        // we merge into currentTable (in git fashion)
        currentTable.merge(mergeTable);

        // Then
        // Current table has been updated
        assertFalse(mergeTable.exists("beef", "lentils"));
        assertFalse(mergeTable.exists("chicken", "tofu"));
        assertTrue(mergeTable.exists("lamb", "chickpea"));
        assertTrue(mergeTable.exists("bacon", "egg"));
    }

    @Test
    public void mergeTable_whereCurrentTableHasSameOriginLinks_includesAllLinks() {
        // Given
        // A couple of redirect table
        RedirectTable currentTable = new RedirectTablePartialMatch(zebedee.getPublished());
        currentTable.addRedirect("beef", "lentils");
        currentTable.addRedirect("chicken", "tofu");

        RedirectTable mergeTable = new RedirectTablePartialMatch(zebedee.getPublished());
        mergeTable.addRedirect("chicken", "chickpea");
        mergeTable.addRedirect("beef", "egg");

        // When
        // we merge into currentTable (in git fashion)
        currentTable.merge(mergeTable);

        // Then
        // Current table has been updated
        assertTrue(currentTable.exists("beef", "lentils"));
        assertTrue(currentTable.exists("chicken", "tofu"));
        assertTrue(currentTable.exists("chicken", "chickpea"));
        assertTrue(currentTable.exists("beef", "egg"));
    }

    @Test
    public void mergeTable_whereMergeTableImpactsCurrentTable_updatesLinks() {
        // Given
        // A couple of redirect table
        RedirectTable currentTable = new RedirectTablePartialMatch(zebedee.getPublished());
        currentTable.addRedirect("roast/beef", "morrocan/lentils");
        currentTable.addRedirect("roast/chicken", "stirfry/tofu");

        RedirectTable mergeTable = new RedirectTablePartialMatch(zebedee.getPublished());
        mergeTable.addRedirect("morrocan", "curried");
        mergeTable.addRedirect("stirfry", "no");

        // When
        // we merge into currentTable (in git fashion)
        currentTable.merge(mergeTable);

        // Then
        // Current table has been updated
        assertTrue(currentTable.exists("roast/beef", "curried/lentils"));
        assertTrue(currentTable.exists("roast/chicken", "no/tofu"));
        assertTrue(currentTable.exists("morrocan", "curried"));
        assertTrue(currentTable.exists("stirfry", "no"));
    }
}