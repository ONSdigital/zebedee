package com.github.onsdigital.zebedee.model;

import com.github.davidcarboni.ResourceUtils;
import com.github.onsdigital.zebedee.Builder;
import com.github.onsdigital.zebedee.Zebedee;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

public class RedirectTableTest {
    Zebedee zebedee;
    Builder bob;

    @Before
    public void setupTests() throws IOException {
        // Create a setup from
        bob = new Builder(RedirectTableTest.class, ResourceUtils.getPath("/bootstraps/basic"));
        zebedee = new Zebedee(bob.zebedee);
    }
    @After
    public void ripdownTests() {
        bob = null;
        zebedee = null;
    }

    //------------------------------------------------------
    //
    // Trivial tests (4 tests)
    //
    // Given - an empty redirect
    // When - we redirect
    // Then - we expect a uri for existing files, null otherwise
    @Test
    public void get_emptyRedirectWithExistingFileURI_shouldReturnURI() throws Exception {
        // Given
        // a uri in the original
        String basicUri = "/themea/landinga/producta/data.json";

        // When
        // we use a basic 301 table
        RedirectTable redirectTable = new RedirectTable(zebedee.published);

        // Then
        // we expect the original
        assertEquals(basicUri, redirectTable.get(basicUri));
    }
    @Test
    public void get_emptyRedirectWithExistingFolderURI_shouldReturnURI() throws Exception {
        // Given
        // a uri in the original
        String basicUri = "/themea/landinga/producta";

        // When
        // we use a basic 301 table
        RedirectTable redirectTable = new RedirectTable(zebedee.published);

        // Then
        // we expect the original
        assertEquals(basicUri, redirectTable.get(basicUri));
    }
    @Test
    public void get_emptyRedirectWithMissingFile_shouldReturnNull() throws Exception {
        // Given
        // a uri in the original
        String basicUri = "/not/a/file.json";

        // When
        // we use a basic 301 table
        RedirectTable redirectTable = new RedirectTable(zebedee.published);

        // Then
        // we expect the original
        assertNull(redirectTable.get(basicUri));
    }
    @Test
    public void get_emptyRedirectWithMissingFolder_shouldReturnNull() throws Exception {
        // Given
        // a uri in the original
        String basicUri = "/not/a/folder";

        // When
        // we use a basic 301 table
        RedirectTable redirectTable = new RedirectTable(zebedee.published);

        // Then
        // we expect the original
        assertNull(redirectTable.get(basicUri));
    }

    //------------------------------------------------------
    //
    // Single Redirect (8 tests)
    //
    // Given - a uri link to redirect
    // When - we redirect
    // Then - we expect trivial responses unless we are using the link
    @Test
    public void get_populatedRedirectWithExistingFileURI_shouldReturnURI() throws Exception {
        // Given
        // uri's to link
        String linkFrom = "/redirect/from/here/to/producta/data.json";
        String linkTo = "/themea/landinga/producta/data.json";
        String otherFile = "/themea/landingb/productc/data.json";

        // When
        // we use a basic 301 table
        RedirectTable redirectTable = new RedirectTable(zebedee.published);
        redirectTable.addRedirect(linkFrom, linkTo);

        // Then
        // we expect the original
        assertEquals(linkTo, redirectTable.get(linkTo));
        assertEquals(otherFile, redirectTable.get(otherFile));
    }
    @Test
    public void get_populatedRedirectWithExistingFolderURI_shouldReturnURI() throws Exception {
        // Given
        // uri's to link
        String linkFrom = "/redirect/from/here/to/producta";
        String linkTo = "/themea/landinga/producta";
        String otherFolder = "/themea/landingb/productc";

        // When
        // we use a basic 301 table
        RedirectTable redirectTable = new RedirectTable(zebedee.published);
        redirectTable.addRedirect(linkFrom, linkTo);

        // Then
        // we expect the original
        assertEquals(linkTo, redirectTable.get(linkTo));
        assertEquals(otherFolder, redirectTable.get(otherFolder));
    }
    @Test
    public void get_populatedRedirectWithMissingFileURI_shouldReturnNull() throws Exception {
        // Given
        // uri's to link
        String linkFrom = "/redirect/from/here/to/producta/data.json";
        String linkTo = "/themea/landinga/producta/data.json";
        String otherFile = "/not/a/file.json";

        // When
        // we use a basic redirect table
        RedirectTable redirectTable = new RedirectTable(zebedee.published);
        redirectTable.addRedirect(linkFrom, linkTo);

        // Then
        // we expect null
        assertNull(redirectTable.get(otherFile));
    }
    @Test
    public void get_populatedRedirectWithMissingFolderURI_shouldReturnNull() throws Exception {
        // Given
        // uri's to link
        String linkFrom = "/redirect/from/here/to/producta/data.json";
        String linkTo = "/themea/landinga/producta/data.json";
        String otherFolder = "/not/a/folder";

        // When
        // we use a basic redirect table
        RedirectTable redirectTable = new RedirectTable(zebedee.published);
        redirectTable.addRedirect(linkFrom, linkTo);

        // Then
        // we expect null
        assertNull(redirectTable.get(otherFolder));
    }
    @Test
    public void get_populatedRedirectWithRedirectedFileURI_shouldReturnLinkedURI() throws Exception {
        // Given
        // uri's to link
        String linkFrom = "/redirect/from/here/to/producta/data.json";
        String linkTo = "/themea/landinga/producta/data.json";

        // When
        // we use a basic redirect table
        RedirectTable redirectTable = new RedirectTable(zebedee.published);
        redirectTable.addRedirect(linkFrom, linkTo);

        // Then
        // we expect the linked URI
        assertEquals(linkTo, redirectTable.get(linkFrom));
    }
    @Test
    public void get_populatedRedirectWithRedirectedFolderURI_shouldReturnLinkedURI() throws Exception {
        // Given
        // uri's to link
        String linkFrom = "/redirect/from/here/to/producta";
        String linkTo = "/themea/landinga/producta";

        // When
        // we use a basic redirect table
        RedirectTable redirectTable = new RedirectTable(zebedee.published);
        redirectTable.addRedirect(linkFrom, linkTo);

        // Then
        // we expect the linked URI
        assertEquals(linkTo, redirectTable.get(linkFrom));
    }
    @Test
    public void get_populatedRedirectWithRedirectFromExistingFolderURI_shouldNotReturnRedirect() throws Exception {
        // Given
        // linkFrom exists, linkTo is anything
        String linkFrom = "/themea/landinga/producta";
        String linkTo = "/should/not/be/linked";

        // When
        // we use a populated redirect table
        RedirectTable redirectTable = new RedirectTable(zebedee.published);
        redirectTable.addRedirect(linkFrom, linkTo);

        // Then
        // we expect the original URI
        assertEquals(linkFrom, redirectTable.get(linkFrom));
    }
    @Test
    public void get_populatedRedirectWithRedirectFromExistingFileURI_shouldNotReturnRedirect() throws Exception {
        // Given
        // linkFrom exists, linkTo is anything
        String linkFrom = "/themea/landinga/producta/data.json";
        String linkTo = "/should/not/be/linked.json";

        // When
        // we use a populated redirect table
        RedirectTable redirectTable = new RedirectTable(zebedee.published);
        redirectTable.addRedirect(linkFrom, linkTo);

        // Then
        // we expect the original URI
        assertEquals(linkFrom, redirectTable.get(linkFrom));
    }

    //------------------------------------------------------
    //
    // Chained Redirect (3 tests)
    //
    // Given - a chain of redirects
    // When - we redirect
    // Then - we expect the chain to redirect to the final file
    @Test
    public void chainedRedirect_WithMultipleRedirects_shouldReturnFinalFileURI() throws Exception {
        // Given
        // linkTo exists, other links chain
        String linkFrom = "/original/link/data.json";
        String chainOne = "/chain/one/data.json";
        String chainTwo = "/chain/two/data.json";
        String chainThree = "/chain/three/data.json";
        String linkTo = "/themea/data.json";

        // When
        // we use a populated redirect table
        RedirectTable redirectTable = new RedirectTable(zebedee.published);
        redirectTable.addRedirect(linkFrom, chainOne);
        redirectTable.addRedirect(chainOne, chainTwo);
        redirectTable.addRedirect(chainTwo, chainThree);
        redirectTable.addRedirect(chainThree, linkTo);

        // Then
        // we expect the chain to send us to linkTo
        assertEquals(linkTo, redirectTable.get(linkFrom));
    }
    @Test
    public void chainedRedirect_WithMultipleRedirects_shouldReturnFinalFolderURI() throws Exception {
        // Given
        // linkTo exists, other links chain
        String linkFrom = "/original/link";
        String chainOne = "/chain/one";
        String chainTwo = "/chain/two";
        String chainThree = "/chain/three";
        String linkTo = "/themea";

        // When
        // we use a populated redirect table
        RedirectTable redirectTable = new RedirectTable(zebedee.published);
        redirectTable.addRedirect(linkFrom, chainOne);
        redirectTable.addRedirect(chainOne, chainTwo);
        redirectTable.addRedirect(chainTwo, chainThree);
        redirectTable.addRedirect(chainThree, linkTo);

        // Then
        // we expect the chain to send us to linkTo
        assertEquals(linkTo, redirectTable.get(linkFrom));
    }
    @Test
    public void chainedRedirect_WithChainEndingInInvalidURI_shouldReturnNull() throws Exception {
        // Given
        // linkTo exists, other links chain
        String linkFrom = "/original/link";
        String chainOne = "/chain/one";
        String chainTwo = "/chain/two";
        String chainThree = "/chain/three";
        String linkTo = "/this/is/not/a/file";

        // When
        // we use a populated redirect table
        RedirectTable redirectTable = new RedirectTable(zebedee.published);
        redirectTable.addRedirect(linkFrom, chainOne);
        redirectTable.addRedirect(chainOne, chainTwo);
        redirectTable.addRedirect(chainTwo, chainThree);
        redirectTable.addRedirect(chainThree, linkTo);

        // Then
        // we expect the chain to send us to linkTo
        assertNull(redirectTable.get(linkFrom));
    }
    @Test
    public void chainedRedirect_withCycle_shouldReturnNull() throws Exception {
        // Given
        // linkTo exists, other links chain
        String one = "/one";
        String two = "/two";
        String three = "/three";

        // When
        // we use a populated redirect table
        RedirectTable redirectTable = new RedirectTable(zebedee.published);
        redirectTable.addRedirect(one, two);
        redirectTable.addRedirect(two, three);
        redirectTable.addRedirect(three, one);

        // Then
        // we expect the chain to send us to linkTo
        assertNull(redirectTable.get(one));
    }
    //------------------------------------------------------
    //
    // Child Redirect (6 tests)
    //
    // Given - a redirect with a child
    // When - we redirect
    // Then - we expect the chain to redirect using the child redirect table as backup
    @Test
    public void parentChildRedirect_withUciNotListedByParent_shouldTryRedirectWithChild() throws Exception {
        // Given
        // sufficient links to set up a child parent table
        String linkFrom = "/parent/link/data.json";
        String linkTo = "/themea/data.json";
        String childFrom = "/child/link/data.json";
        String childTo = "/themeb/data.json";

        // When
        // we set up a parent-child redirect table
        RedirectTable parent = new RedirectTable(zebedee.published);
        parent.addRedirect(linkFrom, linkTo);

        RedirectTable child = new RedirectTable(zebedee.published);
        child.addRedirect(childFrom, childTo);

        parent.setChild(child);

        // Then
        // the parent should inherit the child link
        assertEquals(childTo, parent.get(childFrom));
    }
    @Test
    public void parentChildRedirect_withValidParentRedirect_shouldNotRedirectWithChild() throws Exception {
        // Given
        // linkTo exists, other links chain
        String linkFrom = "/link/data.json";
        String linkTo = "/themea/data.json";
        String childFrom = "/link/data.json";
        String childTo = "/themeb/data.json";

        // When
        // we set up a parent-child redirect table
        RedirectTable parent = new RedirectTable(zebedee.published);
        parent.addRedirect(linkFrom, linkTo);

        RedirectTable child = new RedirectTable(zebedee.published);
        child.addRedirect(childFrom, childTo);

        parent.setChild(child);

        // Then
        // the parent should inherit the child link
        assertEquals(linkTo, parent.get(linkFrom));
    }
    @Test
    public void parentChildRedirect_withInvalidParentRedirect_shouldTryRedirectWithChild() throws Exception {
        // Given
        // linkTo exists, other links chain
        String linkFrom = "/link/data.json";
        String linkTo = "/not/a/file.json";
        String childFrom = "/link/data.json";
        String childTo = "/themeb/data.json";

        // When
        // we set up a parent-child redirect table
        RedirectTable parent = new RedirectTable(zebedee.published);
        parent.addRedirect(linkFrom, linkTo);

        RedirectTable child = new RedirectTable(zebedee.published);
        child.addRedirect(childFrom, childTo);

        parent.setChild(child);

        // Then
        // the parent should inherit the child link
        assertEquals(childTo, parent.get(childFrom));
    }
    @Test
    public void parentChildRedirect_withValidParentRedirectThatIsChained_shouldTryChainBeforeTryingChild() throws Exception {
        // Given
        // linkTo exists, other links chain

        String linkFrom = "/original/link";
        String chainOne = "/chain/one";
        String chainTwo = "/chain/two";
        String chainThree = "/chain/three";
        String linkTo = "/themea";

        String childFrom = "/original/link";
        String childTo = "/themeb";

        // When
        // we set up a parent-child redirect table
        RedirectTable parent = new RedirectTable(zebedee.published);
        parent.addRedirect(linkFrom, chainOne);
        parent.addRedirect(chainOne, chainTwo);
        parent.addRedirect(chainTwo, chainThree);
        parent.addRedirect(chainThree, linkTo);

        RedirectTable child = new RedirectTable(zebedee.published);
        child.addRedirect(childFrom, childTo);

        parent.setChild(child);

        // Then
        // the parent should work through the chain before going to the child
        assertEquals(linkTo, parent.get(linkFrom));
    }
    @Test
    public void parentChildRedirect_withChainThatContinuesFromParentToChild_shouldCompleteChainWithChild() throws Exception {
        // Given
        // linkTo exists, other links chain
        String linkFrom = "/original/link";
        String chainOne = "/chain/one";

        String chainTwo = "/chain/two";
        String chainThree = "/chain/three";
        String linkTo = "/themea";

        // When
        // we set up a parent-child redirect table with a long chain
        RedirectTable parent = new RedirectTable(zebedee.published);
        parent.addRedirect(linkFrom, chainOne);

        RedirectTable child = new RedirectTable(zebedee.published);
        child.addRedirect(chainOne, chainTwo);
        child.addRedirect(chainTwo, chainThree);
        child.addRedirect(chainThree, linkTo);

        parent.setChild(child);

        // Then
        // the chain should pass
        assertEquals(linkTo, parent.get(linkFrom));
    }
    @Test
    public void parentChildRedirect_withChildOfChild_shouldTryRedirectWithChildOfChild() throws Exception {
        // Given
        // some false links
        String parentRedirect = "/redirect/parent";
        String childOneRedirect = "/redirect/child/one";

        String linkFrom = "/original/link";
        String linkTo = "/themea";

        // When
        // we set up a parent-child-grandchild
        RedirectTable parent = new RedirectTable(zebedee.published);
        parent.addRedirect(parentRedirect, "X");

        RedirectTable child = new RedirectTable(zebedee.published);
        child.addRedirect(childOneRedirect, "X");

        RedirectTable grandchild = new RedirectTable(zebedee.published);
        grandchild.addRedirect(linkFrom, linkTo);

        child.setChild(grandchild);
        parent.setChild(child);

        // Then
        // the chain should pass through the whole hierarchy
        assertEquals(linkTo, parent.get(linkFrom));
    }

    //------------------------------------------------------
    //
    // Different content for in parent-child
    //
    // Parent-child redirect can be used in Zebedee with a parent child
    // chain of inProgress, Complete, Reviewed, Published
    //
    // Given - child (the published
    // When - we redirect
    // Then - we expect the combination to process redirects with moves made in collections taking priority

}