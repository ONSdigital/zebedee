package com.github.onsdigital.zebedee.util.versioning;

import com.github.onsdigital.zebedee.content.page.base.PageDescription;
import com.github.onsdigital.zebedee.content.page.statistics.dataset.Dataset;
import com.github.onsdigital.zebedee.content.page.statistics.dataset.Version;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.reader.CollectionReader;
import com.github.onsdigital.zebedee.reader.ContentReader;
import com.github.onsdigital.zebedee.reader.ZebedeeReader;
import com.github.onsdigital.zebedee.session.model.Session;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

public class VersionsServiceImplTest {

    static final String CONTENT_PATH =
            "/economy/economicoutputandproductivity/output/datasets/outputoftheproductionindustries/current/data.json";
    static final String CONTENT_URI =
            "/economy/economicoutputandproductivity/output/datasets/outputoftheproductionindustries/current";
    static final String VERSION_URI =
            "/economy/economicoutputandproductivity/output/datasets/outputoftheproductionindustries/current/previous/v1";
    static final String VERSION_2_URI =
            "/economy/economicoutputandproductivity/output/datasets/outputoftheproductionindustries/current/previous/v2";

    static final String DS_LANDING_PAGE_URI =
            "/economy/economicoutputandproductivity/output/datasets/outputoftheproductionindustries";

    @Mock
    private File file;

    @Mock
    private ZebedeeReader cmsReader;

    @Mock
    private Collection collection;

    @Mock
    private CollectionReader collectionReader;

    @Mock
    private ContentReader contentReader;

    @Mock
    private Session session;

    private List<String> reviewedURIs;
    private List<Version> versions;
    private Version v1;
    private Version v2;
    private Dataset dataset;

    private VersionsServiceImpl service;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        service = new VersionsServiceImpl();

        when(collection.getId())
                .thenReturn("666");

        when(session.getId())
                .thenReturn("777");

        reviewedURIs = new ArrayList<>();
        reviewedURIs.add(CONTENT_PATH);

        v1 = new Version();
        v1.setUri(new URI(VERSION_URI));

        v2 = new Version();
        v2.setUri(new URI(VERSION_2_URI));

        versions = new ArrayList<>();
        versions.add(v1);
        versions.add(v2);

        PageDescription desc = new PageDescription();
        desc.setTitle("Output of the Production Industries");

        dataset = new Dataset();
        dataset.setUri(new URI(CONTENT_URI));
        dataset.setDescription(desc);
        dataset.setVersions(versions);
    }

    @Test
    public void isVersionedURI_NullValue_shouldReturnFalse() {
        assertFalse(service.isVersionedURI(null));
    }

    @Test
    public void isVersionedURI_EmptyValue_shouldReturnFalse() {
        assertFalse(service.isVersionedURI(""));
    }

    @Test
    public void isVersionedURI_NonVersionURI_shouldReturnFalse() {
        assertFalse(service.isVersionedURI("/a/b/c/data.json"));
    }

    @Test
    public void isVersionedURI_VersionURI_shouldReturnFalse() {
        assertTrue(service.isVersionedURI("/a/b/c/previous/v1/data.json"));
    }

    @Test
    public void isVersionDir_nullValue_shouldReturnFalse() {
        assertFalse(service.isVersionDir(null));
    }

    @Test
    public void isVersionDir_fileNotDir_shouldReturnFalse() {
        when(file.isDirectory())
                .thenReturn(false);

        assertFalse(service.isVersionDir(file));
    }

    @Test
    public void isVersionDir_nonVersionName_shouldReturnFalse() {
        when(file.isDirectory())
                .thenReturn(true);

        when(file.getName()).thenReturn("not_a_version");

        assertFalse(service.isVersionDir(file));
    }

    @Test
    public void isVersionDir_validVersionFile_shouldReturnTrue() {
        when(file.isDirectory())
                .thenReturn(true);

        when(file.getName()).thenReturn("v1");

        assertTrue(service.isVersionDir(file));
    }


    @Test
    public void getVersionNameFromURI_uriNull_shouldReturnEmptyString() {
        Optional<String> result = service.getVersionNameFromURI(null);
        assertFalse(result.isPresent());
    }

    @Test
    public void getVersionNameFromURI_uriEmpty_shouldReturnEmptyString() {
        Optional<String> result = service.getVersionNameFromURI("");
        assertFalse(result.isPresent());
    }

    @Test
    public void getVersionNameFromURI_notVersionURI_shouldReturnEmptyString() {
        Optional<String> result = service.getVersionNameFromURI("/a/b/c");
        assertFalse(result.isPresent());
    }

    @Test
    public void getVersionNameFromURI_uriForVersionDir_shouldReturnVersion() {
        Optional<String> result = service.getVersionNameFromURI("/a/b/c/previous/v1");
        assertTrue(result.isPresent());
        assertEquals("v1", result.get());
    }

    @Test
    public void getVersionNameFromURI_uriForVersionFile_shouldReturnVersion() {
        Optional<String> result = service.getVersionNameFromURI("/a/b/c/previous/v1/data.json");
        assertTrue(result.isPresent());
        assertEquals("v1", result.get());
    }
}
