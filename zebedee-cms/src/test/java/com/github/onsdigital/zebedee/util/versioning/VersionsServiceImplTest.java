package com.github.onsdigital.zebedee.util.versioning;

import com.github.onsdigital.zebedee.content.page.base.PageDescription;
import com.github.onsdigital.zebedee.content.page.statistics.dataset.Dataset;
import com.github.onsdigital.zebedee.content.page.statistics.dataset.Version;
import com.github.onsdigital.zebedee.content.page.statistics.document.bulletin.Bulletin;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
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

import static com.github.onsdigital.zebedee.util.versioning.VersionNotFoundException.versionsNotFoundException;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
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
        MockitoAnnotations.initMocks(this);

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
        assertThat(result.get(), equalTo("v1"));
    }

    @Test
    public void getVersionNameFromURI_uriForVersionFile_shouldReturnVersion() {
        Optional<String> result = service.getVersionNameFromURI("/a/b/c/previous/v1/data.json");
        assertTrue(result.isPresent());
        assertThat(result.get(), equalTo("v1"));
    }

    @Test
    public void getVersionNumber_validURI_shouldReturnExpectedValue() {
        assertThat(service.getVersionNumberFromURI("/a/b/c/current/previous/v1/data.json"), equalTo(1));
    }

    @Test
    public void getVersionNumberFromURI_nullValue_shouldReturnDefault() {
        assertThat(service.getVersionNumberFromURI(null), equalTo(-1));
    }

    @Test
    public void getVersionNumberFromURI_emptyValue_shouldReturnDefault() {
        assertThat(service.getVersionNumberFromURI(""), equalTo(-1));
    }

    @Test
    public void getVersionNumberFromURI_notVersionURI_shouldReturnDefault() {
        assertThat(service.getVersionNumberFromURI("/a/b/c"), equalTo(-1));
    }

    @Test
    public void getVersionNumberFromURI_uriForVersionDir_shouldReturnDefault() {
        assertThat(service.getVersionNumberFromURI("/a/b/c/previous/v3"), equalTo(3));
    }

    @Test
    public void getVersionNumberFromURI_uriForVersionFile_shouldReturnDefault() {
        assertThat(service.getVersionNumberFromURI("/a/b/c/previous/v3/data.json"), equalTo(3));
    }

    @Test
    public void verifyCollectionDatasets_noDatasets_shouldDoNothing() throws Exception {
        when(collectionReader.getReviewed())
                .thenReturn(contentReader);

        when(contentReader.listUris())
                .thenReturn(reviewedURIs);

        when(contentReader.getContent(CONTENT_URI))
                .thenReturn(new Bulletin());

        service.verifyCollectionDatasets(cmsReader, collection, collectionReader, session);
    }

    @Test(expected = VersionNotFoundException.class)
    public void verifyCollectionDatasets_containsDatasetMissingVersion_shouldThrowException() throws Exception {
        when(collectionReader.getReviewed())
                .thenReturn(contentReader);

        when(contentReader.listUris())
                .thenReturn(reviewedURIs);

        when(contentReader.getContent(CONTENT_URI))
                .thenReturn(dataset);

        when(cmsReader.getCollectionContent(anyString(), anyString(), eq(VERSION_URI)))
                .thenThrow(new NotFoundException(""));

        when(cmsReader.getCollectionContent(anyString(), anyString(), eq(VERSION_2_URI)))
                .thenThrow(new NotFoundException(""));

        when(cmsReader.getPublishedContent(VERSION_URI))
                .thenThrow(new NotFoundException(""));

        when(cmsReader.getPublishedContent(VERSION_2_URI))
                .thenThrow(new NotFoundException(""));

        try {
            service.verifyCollectionDatasets(cmsReader, collection, collectionReader, session);
        } catch (VersionNotFoundException ex) {

            List<MissingVersion> missingVersions = new ArrayList<MissingVersion>() {{
                add(new MissingVersion(v1));
                add(new MissingVersion(v2));
            }};

            assertThat(ex.getMessage(), equalTo(versionsNotFoundException(collection, missingVersions).getMessage()));
            throw ex;
        }
    }

    @Test
    public void verifyCollectionDatasets_containsDatasetVerionsAllPublished_shouldDoNothing() throws Exception {
        when(collectionReader.getReviewed())
                .thenReturn(contentReader);

        when(contentReader.listUris())
                .thenReturn(reviewedURIs);

        when(contentReader.getContent(CONTENT_URI))
                .thenReturn(dataset);

        // the dataset version is not found in the collection.
        when(cmsReader.getCollectionContent(anyString(), anyString(), eq(VERSION_URI)))
                .thenThrow(new NotFoundException(""));

        // The dataset versions are all found in the published content.
        when(cmsReader.getPublishedContent(VERSION_URI))
                .thenReturn(dataset);

        service.verifyCollectionDatasets(cmsReader, collection, collectionReader, session);
    }

    @Test
    public void verifyCollectionDatasets_collectionContainsDatasetAndVersions_shouldDoNothing() throws Exception {
        when(collectionReader.getReviewed())
                .thenReturn(contentReader);

        when(contentReader.listUris())
                .thenReturn(reviewedURIs);

        when(contentReader.getContent(CONTENT_URI))
                .thenReturn(dataset);

        // the dataset version is exists in the collection
        when(cmsReader.getCollectionContent(anyString(), anyString(), eq(VERSION_URI)))
                .thenReturn(dataset);

        service.verifyCollectionDatasets(cmsReader, collection, collectionReader, session);
    }
}
