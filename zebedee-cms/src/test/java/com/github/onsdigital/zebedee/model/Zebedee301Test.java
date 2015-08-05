package com.github.onsdigital.zebedee.model;

import com.github.davidcarboni.ResourceUtils;
import com.github.onsdigital.zebedee.Builder;
import com.github.onsdigital.zebedee.Zebedee;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

/**
 * Created by thomasridd on 04/08/15.
 */
public class Zebedee301Test {
    Zebedee zebedee;
    Builder bob;

    @Before
    public void setupTests() throws IOException {
        // Create a setup from
        bob = new Builder(Zebedee301Test.class, ResourceUtils.getPath("/bootstraps/basic"));
        zebedee = new Zebedee(bob.zebedee);
    }
    @After
    public void ripdownTests() {
        bob = null;
        zebedee = null;
    }
    @Test
    public void get_withExistingFileURI_shouldReturnURI() throws Exception {
        // Given
        // a uri in the original
        String basicUri = "/themea/landinga/producta/data.json";

        // When
        // we use a basic 301 table
        Zebedee301 zebedee301 = new Zebedee301();

        // Then
        // we expect the original
        assertEquals(basicUri, zebedee301.get(basicUri, zebedee.published));
    }
    @Test
    public void get_withExistingFolderURI_shouldReturnURI() throws Exception {
        // Given
        // a uri in the original
        String basicUri = "/themea/landinga/producta";

        // When
        // we use a basic 301 table
        Zebedee301 zebedee301 = new Zebedee301();

        // Then
        // we expect the original
        assertEquals(basicUri, zebedee301.get(basicUri, zebedee.published));
    }

    @Test
    public void get_withValidRedirect_shouldReturnNewURI() throws Exception {

    }
    @Test
    public void get_withNoContent_shouldReturnNull() throws Exception {

    }
    @Test
    public void get_withEmptyTable_shouldReturnOriginalURI() throws Exception {

    }

}