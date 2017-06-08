package com.github.onsdigital.zebedee.model;

import com.github.davidcarboni.ResourceUtils;
import com.github.onsdigital.zebedee.Builder;
import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.ZebedeeTestBaseFixture;
import com.github.onsdigital.zebedee.exceptions.CollectionNotFoundException;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class RedirectTableChainedTest extends ZebedeeTestBaseFixture {

    @Test
    public void get_emptyRedirectWithExistingFileURI_shouldReturnURI() throws Exception {
        // Given
        // a uri in the original
        String basicUri = "/themea/landinga/producta/data.json";

        // When
        // we use a basic 301 table
        RedirectTableChained redirectTableChained = new RedirectTableChained(zebedee.getPublished());

        // Then
        // we expect the original
        assertEquals(basicUri, redirectTableChained.get(basicUri));
    }
    @Test
    public void get_emptyRedirectWithExistingFolderURI_shouldReturnURI() throws Exception {
        // Given
        // a uri in the original
        String basicUri = "/themea/landinga/producta";

        // When
        // we use a basic 301 table
        RedirectTableChained redirectTableChained = new RedirectTableChained(zebedee.getPublished());

        // Then
        // we expect the original
        assertEquals(basicUri, redirectTableChained.get(basicUri));
    }
    @Test
    public void get_emptyRedirectWithMissingFile_shouldReturnNull() throws Exception {
        // Given
        // a uri in the original
        String basicUri = "/not/a/file.json";

        // When
        // we use a basic 301 table
        RedirectTableChained redirectTableChained = new RedirectTableChained(zebedee.getPublished());

        // Then
        // we expect the original
        assertNull(redirectTableChained.get(basicUri));
    }
    @Test
    public void get_emptyRedirectWithMissingFolder_shouldReturnNull() throws Exception {
        // Given
        // a uri in the original
        String basicUri = "/not/a/folder";

        // When
        // we use a basic 301 table
        RedirectTableChained redirectTableChained = new RedirectTableChained(zebedee.getPublished());

        // Then
        // we expect the original
        assertNull(redirectTableChained.get(basicUri));
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
        RedirectTableChained redirectTableChained = new RedirectTableChained(zebedee.getPublished());
        redirectTableChained.addRedirect(linkFrom, linkTo);

        // Then
        // we expect the original
        assertEquals(linkTo, redirectTableChained.get(linkTo));
        assertEquals(otherFile, redirectTableChained.get(otherFile));
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
        RedirectTableChained redirectTableChained = new RedirectTableChained(zebedee.getPublished());
        redirectTableChained.addRedirect(linkFrom, linkTo);

        // Then
        // we expect the original
        assertEquals(linkTo, redirectTableChained.get(linkTo));
        assertEquals(otherFolder, redirectTableChained.get(otherFolder));
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
        RedirectTableChained redirectTableChained = new RedirectTableChained(zebedee.getPublished());
        redirectTableChained.addRedirect(linkFrom, linkTo);

        // Then
        // we expect null
        assertNull(redirectTableChained.get(otherFile));
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
        RedirectTableChained redirectTableChained = new RedirectTableChained(zebedee.getPublished());
        redirectTableChained.addRedirect(linkFrom, linkTo);

        // Then
        // we expect null
        assertNull(redirectTableChained.get(otherFolder));
    }
    @Test
    public void get_populatedRedirectWithRedirectedFileURI_shouldReturnLinkedURI() throws Exception {
        // Given
        // uri's to link
        String linkFrom = "/redirect/from/here/to/producta/data.json";
        String linkTo = "/themea/landinga/producta/data.json";

        // When
        // we use a basic redirect table
        RedirectTableChained redirectTableChained = new RedirectTableChained(zebedee.getPublished());
        redirectTableChained.addRedirect(linkFrom, linkTo);

        // Then
        // we expect the linked URI
        assertEquals(linkTo, redirectTableChained.get(linkFrom));
    }
    @Test
    public void get_populatedRedirectWithRedirectedFolderURI_shouldReturnLinkedURI() throws Exception {
        // Given
        // uri's to link
        String linkFrom = "/redirect/from/here/to/producta";
        String linkTo = "/themea/landinga/producta";

        // When
        // we use a basic redirect table
        RedirectTableChained redirectTableChained = new RedirectTableChained(zebedee.getPublished());
        redirectTableChained.addRedirect(linkFrom, linkTo);

        // Then
        // we expect the linked URI
        assertEquals(linkTo, redirectTableChained.get(linkFrom));
    }
    @Test
    public void get_populatedRedirectWithRedirectFromExistingFolderURI_shouldNotReturnRedirect() throws Exception {
        // Given
        // linkFrom exists, linkTo is anything
        String linkFrom = "/themea/landinga/producta";
        String linkTo = "/should/not/be/linked";

        // When
        // we use a populated redirect table
        RedirectTableChained redirectTableChained = new RedirectTableChained(zebedee.getPublished());
        redirectTableChained.addRedirect(linkFrom, linkTo);

        // Then
        // we expect the original URI
        assertEquals(linkFrom, redirectTableChained.get(linkFrom));
    }
    @Test
    public void get_populatedRedirectWithRedirectFromExistingFileURI_shouldNotReturnRedirect() throws Exception {
        // Given
        // linkFrom exists, linkTo is anything
        String linkFrom = "/themea/landinga/producta/data.json";
        String linkTo = "/should/not/be/linked.json";

        // When
        // we use a populated redirect table
        RedirectTableChained redirectTableChained = new RedirectTableChained(zebedee.getPublished());
        redirectTableChained.addRedirect(linkFrom, linkTo);

        // Then
        // we expect the original URI
        assertEquals(linkFrom, redirectTableChained.get(linkFrom));
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
        RedirectTableChained redirectTableChained = new RedirectTableChained(zebedee.getPublished());
        redirectTableChained.addRedirect(linkFrom, chainOne);
        redirectTableChained.addRedirect(chainOne, chainTwo);
        redirectTableChained.addRedirect(chainTwo, chainThree);
        redirectTableChained.addRedirect(chainThree, linkTo);

        // Then
        // we expect the chain to send us to linkTo
        assertEquals(linkTo, redirectTableChained.get(linkFrom));
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
        RedirectTableChained redirectTableChained = new RedirectTableChained(zebedee.getPublished());
        redirectTableChained.addRedirect(linkFrom, chainOne);
        redirectTableChained.addRedirect(chainOne, chainTwo);
        redirectTableChained.addRedirect(chainTwo, chainThree);
        redirectTableChained.addRedirect(chainThree, linkTo);

        // Then
        // we expect the chain to send us to linkTo
        assertEquals(linkTo, redirectTableChained.get(linkFrom));
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
        RedirectTableChained redirectTableChained = new RedirectTableChained(zebedee.getPublished());
        redirectTableChained.addRedirect(linkFrom, chainOne);
        redirectTableChained.addRedirect(chainOne, chainTwo);
        redirectTableChained.addRedirect(chainTwo, chainThree);
        redirectTableChained.addRedirect(chainThree, linkTo);

        // Then
        // we expect the chain to send us to linkTo
        assertNull(redirectTableChained.get(linkFrom));
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
        RedirectTableChained redirectTableChained = new RedirectTableChained(zebedee.getPublished());
        redirectTableChained.addRedirect(one, two);
        redirectTableChained.addRedirect(two, three);
        redirectTableChained.addRedirect(three, one);

        // Then
        // we expect the chain to send us to linkTo
        assertNull(redirectTableChained.get(one));
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
        RedirectTableChained parent = new RedirectTableChained(zebedee.getPublished());
        parent.addRedirect(linkFrom, linkTo);

        RedirectTableChained child = new RedirectTableChained(zebedee.getPublished());
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
        RedirectTableChained parent = new RedirectTableChained(zebedee.getPublished());
        parent.addRedirect(linkFrom, linkTo);

        RedirectTableChained child = new RedirectTableChained(zebedee.getPublished());
        child.addRedirect(childFrom, childTo);

        parent.setChild(child);

        // Then
        // the parent should inherit the child link
        assertEquals(linkTo, parent.get(linkFrom));
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
        RedirectTableChained parent = new RedirectTableChained(zebedee.getPublished());
        parent.addRedirect(linkFrom, chainOne);
        parent.addRedirect(chainOne, chainTwo);
        parent.addRedirect(chainTwo, chainThree);
        parent.addRedirect(chainThree, linkTo);

        RedirectTableChained child = new RedirectTableChained(zebedee.getPublished());
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
        RedirectTableChained parent = new RedirectTableChained(zebedee.getPublished());
        parent.addRedirect(linkFrom, chainOne);

        RedirectTableChained child = new RedirectTableChained(zebedee.getPublished());
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
        RedirectTableChained parent = new RedirectTableChained(zebedee.getPublished());
        parent.addRedirect(parentRedirect, "X");

        RedirectTableChained child = new RedirectTableChained(zebedee.getPublished());
        child.addRedirect(childOneRedirect, "X");

        RedirectTableChained grandchild = new RedirectTableChained(zebedee.getPublished());
        grandchild.addRedirect(linkFrom, linkTo);

        child.setChild(grandchild);
        parent.setChild(child);

        // Then
        // the chain should pass through the whole hierarchy
        assertEquals(linkTo, parent.get(linkFrom));
    }

    //------------------------------------------------------
    //
    // File saving and loading (4 tests)
    //
    //
    // Given - simple redirects
    // When - we save and load
    // Then - we expect the table to load back up and still work
    @Test
    public void fileSave_withSimpleTable_savesExpectedData() throws IOException {
        // Given
        // a one line table
        RedirectTableChained redirectTableChained = new RedirectTableChained(zebedee.getPublished());
        redirectTableChained.addRedirect("a", "b");

        // When
        // we save to a file
        Path path = File.createTempFile("redirect", "txt").toPath();
        redirectTableChained.save(path);

        // Then
        // we expect the
        List<String> lines = lines(path);
        assertEquals(1, lines.size());
        assertEquals("a\tb", lines.get(0));
    }

    @Test
    public void fileSave_withMultipleLines_savesExpectedData() throws IOException {
        // Given
        // a two line table
        RedirectTableChained redirectTableChained = new RedirectTableChained(zebedee.getPublished());
        redirectTableChained.addRedirect("a", "b");
        redirectTableChained.addRedirect("c", "d");

        // When
        // we save to a file
        Path path = File.createTempFile("redirect", "txt").toPath();
        redirectTableChained.save(path);

        // Then
        // we expect the file to contain the redirects
        List<String> lines = lines(path);
        java.util.Collections.sort(lines);

        assertEquals(2, lines.size());
        assertEquals("a\tb", lines.get(0));
        assertEquals("c\td", lines.get(1));
    }

    @Test
    public void fileSave_withRealData_savesExpected() throws IOException {
        // Given
        // a one line table
        RedirectTableChained redirectTableChained = new RedirectTableChained(zebedee.getPublished());
        String linkFrom1 = "/from/one/data.json";
        String linkTo1 = "/themea/data.json";
        String linkFrom2 = "/from/two/data.json";
        String linkTo2 = "/themeb/data.json";

        redirectTableChained.addRedirect(linkFrom1, linkTo1);
        redirectTableChained.addRedirect(linkFrom2, linkTo2);

        // When
        // we save to a file
        Path path = File.createTempFile("redirect", "txt").toPath();
        redirectTableChained.save(path);

        // Then
        // we expect the
        List<String> lines = lines(path);
        java.util.Collections.sort(lines);

        assertEquals(2, lines.size());
        assertEquals(linkFrom1 + '\t' + linkTo1, lines.get(0));
        assertEquals(linkFrom2 + '\t' + linkTo2, lines.get(1));
    }

    @Test
    public void fileLoad_withRealData_loadsWorkingTable() throws IOException {
        // Given
        // a simple table that we save
        RedirectTableChained redirectTableChained = new RedirectTableChained(zebedee.getPublished());
        String linkFrom1 = "/from/one/data.json";
        String linkTo1 = "/themea/data.json";
        String linkFrom2 = "/from/two/data.json";
        String linkTo2 = "/themeb/data.json";

        redirectTableChained.addRedirect(linkFrom1, linkTo1);
        redirectTableChained.addRedirect(linkFrom2, linkTo2);
        Path path = File.createTempFile("redirect", "txt").toPath();
        redirectTableChained.save(path);

        // When
        // we reload
        RedirectTableChained loadedTable = new RedirectTableChained(zebedee.getPublished(), path);

        // Then
        // we expect the
        assertEquals(linkTo1, loadedTable.get(linkFrom1));
        assertEquals(linkTo2, loadedTable.get(linkFrom2));
    }

    /**
     * Convenience method to pull all strings out of a file
     *
     * @param path
     * @return
     * @throws FileNotFoundException
     */
    private List<String> lines(Path path) throws FileNotFoundException {
        Scanner sc = new Scanner(path.toFile());
        List<String> lineList = new ArrayList<String>();
        while (sc.hasNextLine()) {
            lineList.add(sc.nextLine());
        }
        return lineList;
    }

    @Override
    public void setUp() throws Exception {
        builder = new Builder(ResourceUtils.getPath("/bootstraps/basic"));
        zebedee = builder.getZebedee();
    }
}