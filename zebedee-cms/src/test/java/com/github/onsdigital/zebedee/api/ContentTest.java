package com.github.onsdigital.zebedee.api;

import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.json.CollectionDescription;
import com.github.onsdigital.zebedee.keyring.CollectionKeyring;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.model.Collections;
import com.github.onsdigital.zebedee.model.ZebedeeCollectionReaderFactory;
import com.github.onsdigital.zebedee.model.encryption.EncryptionKeyFactory;
import com.github.onsdigital.zebedee.model.encryption.EncryptionKeyFactoryImpl;
import com.github.onsdigital.zebedee.permissions.service.PermissionsService;
import com.github.onsdigital.zebedee.reader.ZebedeeReader;
import com.github.onsdigital.zebedee.reader.api.endpoint.Data;
import com.github.onsdigital.zebedee.service.InteractivesService;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.session.service.Sessions;
import com.github.onsdigital.zebedee.user.service.UsersService;
import com.github.onsdigital.zebedee.util.EncryptionUtils;
import com.github.onsdigital.zebedee.util.slack.Notifier;
import com.github.onsdigital.zebedee.util.versioning.VersionsServiceImpl;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletResponse;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * An Integration test for the end-to-end of creating a piece of content in Zebedee
 *
 * Note: this should probably use FailSafe and be renamed to ContentTestIT but the current
 * zebedee CI pipeline does not execute the verify maven phase, so would never execute these tests.
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class ContentTest {

    @Mock
    private Zebedee mockZebedee;
    @Mock
    private Sessions mockSessions;
    @Mock
    private Session mockSession;
    @Mock
    private CollectionKeyring mockCollectionKeyring;
    @Mock
    private PermissionsService mockPermissionsService;
    @Mock
    UsersService mockUsersService;
    @Mock
    Notifier mockNotifier;
    @Mock
    InteractivesService interactivesService;
    Path tempBasePath;

    CollectionDescription collectionDescription;
    private EncryptionKeyFactory encryptionKeyFactory;
    private SecretKey secretKey;
    private VersionsServiceImpl versionsService;
    private com.github.onsdigital.zebedee.model.Content content;
    private Collections collections;

    @Before
    public void setUp() throws Exception {
        tempBasePath = Files.createTempDirectory("tempZebedee");

        System.setProperty("ENABLE_DATASET_IMPORT", "false");
        System.setProperty("zebedee_root", tempBasePath.toString());

        collectionDescription = new CollectionDescription("AK Testing");
        encryptionKeyFactory = new EncryptionKeyFactoryImpl();
        secretKey = KeyGenerator.getInstance("AES").generateKey();
        versionsService = new VersionsServiceImpl();

        content = new com.github.onsdigital.zebedee.model.Content(tempBasePath);
        collections = new Collections(tempBasePath, mockPermissionsService, versionsService, interactivesService, content);

        when(mockPermissionsService.canEdit(mockSession)).thenReturn(true);
        when(mockPermissionsService.canView(any(), any())).thenReturn(true);

        when(mockSessions.get()).thenReturn(mockSession);

        when(mockCollectionKeyring.get(any(), any())).thenReturn(secretKey);

        when(mockZebedee.getCollections()).thenReturn(collections);
        when(mockZebedee.getSessions()).thenReturn(mockSessions);
        when(mockZebedee.getEncryptionKeyFactory()).thenReturn(encryptionKeyFactory);
        when(mockZebedee.getPermissionsService()).thenReturn(mockPermissionsService);
        when(mockZebedee.getUsersService()).thenReturn(mockUsersService);
        when(mockZebedee.getCollectionKeyring()).thenReturn(mockCollectionKeyring);
        when(mockZebedee.getSlackNotifier()).thenReturn(mockNotifier);
        Root.zebedee = mockZebedee;
    }

    @Test
    public void responseContainsFileDownloadLinkWhenJSONV1IsWritten() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        String collectionId = "aktesting";
        request.setPathInfo("/content/" + collectionId);
        String datasetURL = "/peoplepopulationandcommunity/birthsdeathsandmarriages/livebirths/datasets/babynamesenglandandwalesbabynamesstatisticsboys/2022/data.json";
        request.addParameter("uri", datasetURL);
        request.setContent(getV1RequestContent());

        Path versionFile = createCollectionAndPaths(collectionId);

        // Given V1 Of the JSON was written
        Content contentApi = new Content();
        contentApi.saveContent(request, new MockHttpServletResponse());


        byte[] bytes = IOUtils.toByteArray(EncryptionUtils.encryptionInputStream(versionFile, secretKey));
        Assert.assertEquals(EXPECTED_CONTENT_V1, new String(bytes));

        // When the content is requested from Content API
        MockHttpServletRequest dataRequest = new MockHttpServletRequest();
        dataRequest.setRequestURI("/data/" + collectionId);

        String dataURL = "/peoplepopulationandcommunity/birthsdeathsandmarriages/livebirths/datasets/babynamesenglandandwalesbabynamesstatisticsboys/2022";
        dataRequest.addParameter("uri", dataURL);

        ZebedeeCollectionReaderFactory factory = new ZebedeeCollectionReaderFactory(mockZebedee);
        ZebedeeReader.setCollectionReaderFactory(factory);

        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        ByteArrayOutputStream content = new ByteArrayOutputStream(1024);
        when(response.getOutputStream())
                .thenReturn(new StubServletOutputStream(content));

        Data dataAPI = new Data();
        dataAPI.read(dataRequest, response);

        // Then I should receive a structured V1 content back with file download link
        verify(response).setStatus(200);
        JSONAssert.assertEquals(EXPECTED_CONTENT_V1, content.toString(), false);
    }

    @Test
    public void responseContainsUriDownloadLinkWhenJSONV2IsWritten() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        String collectionId = "aktesting";
        request.setPathInfo("/content/" + collectionId);
        String datasetURL = "/peoplepopulationandcommunity/birthsdeathsandmarriages/livebirths/datasets/babynamesenglandandwalesbabynamesstatisticsboys/2022/data.json";
        request.addParameter("uri", datasetURL);
        request.setContent(getV2RequestContent());

        Path versionFile = createCollectionAndPaths(collectionId);

        Content contentApi = new Content();
        contentApi.saveContent(request, new MockHttpServletResponse());

        byte[] bytes = IOUtils.toByteArray(EncryptionUtils.encryptionInputStream(versionFile, secretKey));
        Assert.assertEquals(EXPECTED_CONTENT_V2, new String(bytes));

        // read the version content via the content API.
        MockHttpServletRequest dataRequest = new MockHttpServletRequest();
        dataRequest.setRequestURI("/data/" + collectionId);

        String dataURL = "/peoplepopulationandcommunity/birthsdeathsandmarriages/livebirths/datasets/babynamesenglandandwalesbabynamesstatisticsboys/2022";
        dataRequest.addParameter("uri", dataURL);

        ZebedeeCollectionReaderFactory factory = new ZebedeeCollectionReaderFactory(mockZebedee);
        ZebedeeReader.setCollectionReaderFactory(factory);

        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        ByteArrayOutputStream content = new ByteArrayOutputStream(1024);
        when(response.getOutputStream())
                .thenReturn(new StubServletOutputStream(content));

        Data dataAPI = new Data();
        dataAPI.read(dataRequest, response);

        // Then I should receive a structured V2 content back with uri download link
        verify(response).setStatus(200);
        JSONAssert.assertEquals(EXPECTED_CONTENT_V2, content.toString(), false);
    }

    private Path createCollectionAndPaths(String collectionId) throws IOException, ZebedeeException {
        Collection.create(collectionDescription, mockZebedee, mockSession);
        Path pathToCreate = Paths.get(tempBasePath.toString(), collectionId, "inprogress","peoplepopulationandcommunity", "birthsdeathsandmarriages", "livebirths", "datasets", "babynamesenglandandwalesbabynamesstatisticsboys", "2022");
        Path datasetPath = Files.createDirectories(pathToCreate);
        Path versionFile = Paths.get(datasetPath.toString(), "data.json");
        Files.createFile(versionFile);
        return versionFile;
    }

    private byte[] getV1RequestContent() {
        return EXPECTED_CONTENT_V1.getBytes();
    }

    private static final String EXPECTED_CONTENT_V1 =
            "{\"downloads\":" +
                    "[{\"file\":\"ac2be72c.xls\"}]," +
                    "\"type\":\"dataset\"," +
                    "\"uri\":\"/peoplepopulationandcommunity/birthsdeathsandmarriages/livebirths/datasets/babynamesenglandandwalesbabynamesstatisticsboys/2022\"," +
                    "\"description\":" +
                    "{\"title\":\"Baby Names Statistics Boys\"," +
                    "\"summary\":\"Ranks and counts for boys' baby names in England and Wales and also by region and month.\"," +
                    "\"keywords\":[\"top,ten,boys,girls,most,popular\"]," +
                    "\"metaDescription\":\"Ranks and counts for boys' baby names in England and Wales and also by region and month.\"," +
                    "\"nationalStatistic\":true," +
                    "\"contact\":{\"email\":\"vsob@ons.gov.uk\",\"name\":\"Elizabeth McLaren\",\"telephone\":\"+44 (0)1329 444110\"}," + "\"releaseDate\":\"2015-08-16T23:00:00.000Z\"," +
                    "\"nextRelease\":\"August - September 16 (provisional date)\"," +
                    "\"edition\":\"2022\"," +
                    "\"datasetId\":\"\"," +
                    "\"unit\":\"\",\"preUnit\":\"\"," +
                    "\"source\":\"\"," +
                    "\"versionLabel\":\"Testing\"}," +
                    "\"versions\":[{\"correctionNotice\":\"\"," +
                    "\"updateDate\":\"2022-05-03T11:31:21.283Z\"," +
                    "\"uri\":\"/peoplepopulationandcommunity/birthsdeathsandmarriages/livebirths/datasets/babynamesenglandandwalesbabynamesstatisticsboys/2022/previous/v1\"," +
                    "\"label\":\"Testing\"}]" +
                    "}";

    private byte[] getV2RequestContent() {
        return EXPECTED_CONTENT_V2.getBytes();
    }

    private static final String EXPECTED_CONTENT_V2 =
            "{\"downloads\":[{\"url\":\"some/path/some/file.csv\",\"version\":\"v2\"}]," +
                    "\"type\":\"dataset\"," +
                    "\"uri\":\"/peoplepopulationandcommunity/birthsdeathsandmarriages/livebirths/datasets/babynamesenglandandwalesbabynamesstatisticsboys/2022\"," +
                    "\"description\":" +
                    "{\"title\":\"Baby Names Statistics Boys\"," +
                    "\"summary\":\"Ranks and counts for boys' baby names in England and Wales and also by region and month.\"," +
                    "\"keywords\":[\"top,ten,boys,girls,most,popular\"]," +
                    "\"metaDescription\":\"Ranks and counts for boys' baby names in England and Wales and also by region and month.\"," +
                    "\"nationalStatistic\":true," +
                    "\"contact\":{\"email\":\"vsob@ons.gov.uk\",\"name\":\"Elizabeth McLaren\",\"telephone\":\"+44 (0)1329 444110\"}," + "\"releaseDate\":\"2015-08-16T23:00:00.000Z\"," +
                    "\"nextRelease\":\"August - September 16 (provisional date)\"," +
                    "\"edition\":\"2022\"," +
                    "\"datasetId\":\"\"," +
                    "\"unit\":\"\",\"preUnit\":\"\"," +
                    "\"source\":\"\"," +
                    "\"versionLabel\":\"Testing\"}," +
                    "\"versions\":[{\"correctionNotice\":\"\"," +
                    "\"updateDate\":\"2022-05-03T11:31:21.283Z\"," +
                    "\"uri\":\"/peoplepopulationandcommunity/birthsdeathsandmarriages/livebirths/datasets/babynamesenglandandwalesbabynamesstatisticsboys/2022/previous/v1\"," +
                    "\"label\":\"Testing\"}]" +
                    "}";
}