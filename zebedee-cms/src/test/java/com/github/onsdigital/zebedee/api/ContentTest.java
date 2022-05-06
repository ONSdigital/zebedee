package com.github.onsdigital.zebedee.api;

import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.json.CollectionDescription;
import com.github.onsdigital.zebedee.keyring.CollectionKeyring;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.model.Collections;
import com.github.onsdigital.zebedee.model.encryption.EncryptionKeyFactory;
import com.github.onsdigital.zebedee.model.encryption.EncryptionKeyFactoryImpl;
import com.github.onsdigital.zebedee.permissions.service.PermissionsService;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.session.service.Sessions;
import com.github.onsdigital.zebedee.user.service.UsersService;
import com.github.onsdigital.zebedee.util.EncryptionUtils;
import com.github.onsdigital.zebedee.util.slack.Notifier;
import com.github.onsdigital.zebedee.util.versioning.VersionsServiceImpl;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ContentTest {
    
    @Mock
    private Zebedee mockZebedee;

    @Mock
    private Sessions mockSessions;

    @Mock
    private Session mockSession;

    @Mock
    private Collections mockCollections;

    @Mock
    private CollectionKeyring mockCollectionKeyring;


    @Mock
    private PermissionsService mockPermissionsService;

    @Mock
    UsersService mockUsersService;

    @Mock
    Notifier mockNotifier;

    @Test
    public void WriteVersionFileV1() throws Exception {
        Path tempBasePath = Files.createTempDirectory("tempZebedee");

        System.setProperty("ENABLE_DATASET_IMPORT", "false");
        System.setProperty("zebedee_root", tempBasePath.toString());

        MockHttpServletRequest request = new MockHttpServletRequest();
        String collectionId = "aktesting";
        request.setPathInfo("/content/" + collectionId);
        String datasetURL = "/peoplepopulationandcommunity/birthsdeathsandmarriages/livebirths/datasets/babynamesenglandandwalesbabynamesstatisticsboys/2022/data.json";
        request.addParameter("uri", datasetURL);
        request.setContent(getV1RequestContent());

        CollectionDescription collectionDescription = new CollectionDescription("AK Testing");
        collectionDescription.setEncrypted(false);
        EncryptionKeyFactory encryptionKeyFactory = new EncryptionKeyFactoryImpl();

        SecretKey secretKey = KeyGenerator.getInstance("AES").generateKey();

        VersionsServiceImpl versionsService = new VersionsServiceImpl();
        com.github.onsdigital.zebedee.model.Content content = new com.github.onsdigital.zebedee.model.Content(tempBasePath);

        Collections collections = new Collections(tempBasePath, mockPermissionsService, versionsService, content);

        when(mockPermissionsService.canEdit(mockSession)).thenReturn(true);

        when(mockSessions.get()).thenReturn(mockSession);

        when(mockCollections.getPath()).thenReturn(tempBasePath);

        when(mockCollectionKeyring.get(any(), any())).thenReturn(secretKey);

        when(mockNotifier.sendCollectionWarning(any(), any(), any())).thenReturn(true);
        when(mockZebedee.getCollections()).thenReturn(collections);
        when(mockZebedee.getSessions()).thenReturn(mockSessions);
        when(mockZebedee.getEncryptionKeyFactory()).thenReturn(encryptionKeyFactory);
        when(mockZebedee.getPermissionsService()).thenReturn(mockPermissionsService);
        when(mockZebedee.getUsersService()).thenReturn(mockUsersService);
        when(mockZebedee.getCollectionKeyring()).thenReturn(mockCollectionKeyring);
        when(mockZebedee.getSlackNotifier()).thenReturn(mockNotifier);
        Root.zebedee = mockZebedee;

        Collection.create(collectionDescription, mockZebedee, mockSession);

        Path pathToCreate = Paths.get(tempBasePath.toString(), collectionId, "inprogress","peoplepopulationandcommunity", "birthsdeathsandmarriages", "livebirths", "datasets", "babynamesenglandandwalesbabynamesstatisticsboys", "2022");
        Path datasetPath = Files.createDirectories(pathToCreate);
        Path versionFile = Paths.get(datasetPath.toString(), "data.json");
        Files.createFile(versionFile);

        try {
            Content contentApi = new Content();
            boolean result = contentApi.saveContent(request, new MockHttpServletResponse());
            Assert.assertTrue(result);
        } catch (Exception e) {
            Assert.fail(e.getMessage() + e.toString());
            e.printStackTrace();
            throw e;
        }

        byte[] bytes = IOUtils.toByteArray(EncryptionUtils.encryptionInputStream(versionFile, secretKey));

        Assert.assertEquals(expectedContentV1, new String(bytes));
    }

    private byte[] getV1RequestContent() {
        return expectedContentV1.getBytes();
    }

    private static final String expectedContentV1 =
            "{\"downloads\":" +
                "[{\"file\":\"ac2be72c.xls\"}]," +
            "\"type\":\"dataset\"," +
            "\"uri\":\"/peoplepopulationandcommunity/birthsdeathsandmarriages/livebirths/datasets/babynamesenglandandwalesbabynamesstatisticsboys/2014\"," +
            "\"description\":" +
                "{\"title\":\"Baby Names Statistics Boys\"," +
                "\"summary\":\"Ranks and counts for boys' baby names in England and Wales and also by region and month.\"," +
                "\"keywords\":[\"top,ten,boys,girls,most,popular\"]," +
                "\"metaDescription\":\"Ranks and counts for boys' baby names in England and Wales and also by region and month.\"," +
                "\"nationalStatistic\":true," +
                "\"contact\":{\"email\":\"vsob@ons.gov.uk\",\"name\":\"Elizabeth McLaren\",\"telephone\":\"+44 (0)1329 444110\"}," + "\"releaseDate\":\"2015-08-16T23:00:00.000Z\"," +
                "\"nextRelease\":\"August - September 16 (provisional date)\"," +
                "\"edition\":\"2014\"," +
                "\"datasetId\":\"\"," +
                "\"unit\":\"\",\"preUnit\":\"\"," +
                "\"source\":\"\"," +
                "\"versionLabel\":\"Testing\"}," +
            "\"versions\":[{\"correctionNotice\":\"\"," +
                "\"updateDate\":\"2022-05-03T11:31:21.283Z\"," +
                "\"uri\":\"/peoplepopulationandcommunity/birthsdeathsandmarriages/livebirths/datasets/babynamesenglandandwalesbabynamesstatisticsboys/2014/previous/v1\"," +
                "\"label\":\"Testing\"}]" +
            "}";

    @Test
    public void WriteVersionFileV2() throws Exception {
        Path tempBasePath = Files.createTempDirectory("tempZebedee");

        System.setProperty("ENABLE_DATASET_IMPORT", "false");
        System.setProperty("zebedee_root", tempBasePath.toString());

        MockHttpServletRequest request = new MockHttpServletRequest();
        String collectionId = "aktesting";
        request.setPathInfo("/content/" + collectionId);
        String datasetURL = "/peoplepopulationandcommunity/birthsdeathsandmarriages/livebirths/datasets/babynamesenglandandwalesbabynamesstatisticsboys/2022/data.json";
        request.addParameter("uri", datasetURL);
        request.setContent(getV2RequestContent());

        CollectionDescription collectionDescription = new CollectionDescription("AK Testing");
        collectionDescription.setEncrypted(false);
        EncryptionKeyFactory encryptionKeyFactory = new EncryptionKeyFactoryImpl();

        SecretKey secretKey = KeyGenerator.getInstance("AES").generateKey();

        VersionsServiceImpl versionsService = new VersionsServiceImpl();
        com.github.onsdigital.zebedee.model.Content content = new com.github.onsdigital.zebedee.model.Content(tempBasePath);

        Collections collections = new Collections(tempBasePath, mockPermissionsService, versionsService, content);

        when(mockPermissionsService.canEdit(mockSession)).thenReturn(true);

        when(mockSessions.get()).thenReturn(mockSession);

        when(mockCollections.getPath()).thenReturn(tempBasePath);

        when(mockCollectionKeyring.get(any(), any())).thenReturn(secretKey);

        when(mockNotifier.sendCollectionWarning(any(), any(), any())).thenReturn(true);
        when(mockZebedee.getCollections()).thenReturn(collections);
        when(mockZebedee.getSessions()).thenReturn(mockSessions);
        when(mockZebedee.getEncryptionKeyFactory()).thenReturn(encryptionKeyFactory);
        when(mockZebedee.getPermissionsService()).thenReturn(mockPermissionsService);
        when(mockZebedee.getUsersService()).thenReturn(mockUsersService);
        when(mockZebedee.getCollectionKeyring()).thenReturn(mockCollectionKeyring);
        when(mockZebedee.getSlackNotifier()).thenReturn(mockNotifier);
        Root.zebedee = mockZebedee;

        Collection.create(collectionDescription, mockZebedee, mockSession);

        Path pathToCreate = Paths.get(tempBasePath.toString(), collectionId, "inprogress","peoplepopulationandcommunity", "birthsdeathsandmarriages", "livebirths", "datasets", "babynamesenglandandwalesbabynamesstatisticsboys", "2022");
        Path datasetPath = Files.createDirectories(pathToCreate);
        Path versionFile = Paths.get(datasetPath.toString(), "data.json");
        Files.createFile(versionFile);

        try {
            Content contentApi = new Content();
            boolean result = contentApi.saveContent(request, new MockHttpServletResponse());
            Assert.assertTrue(result);
        } catch (Exception e) {
            Assert.fail(e.getMessage() + e.toString());
            e.printStackTrace();
            throw e;
        }

        byte[] bytes = IOUtils.toByteArray(EncryptionUtils.encryptionInputStream(versionFile, secretKey));

        Assert.assertEquals(expectedContentV1, new String(bytes));
    }

    private byte[] getV2RequestContent() {
        return expectedContentV1.getBytes();
    }

    private static final String expectedContentV2 =
            "{\"downloads\":[{\"url\":\"some/path/to/ac2be72c.xls\",\"version\":\"v2\"}]," +
                    "\"type\":\"dataset\"," +
                    "\"uri\":\"/peoplepopulationandcommunity/birthsdeathsandmarriages/livebirths/datasets/babynamesenglandandwalesbabynamesstatisticsboys/2014\"," +
                    "\"description\":" +
                    "{\"title\":\"Baby Names Statistics Boys\"," +
                    "\"summary\":\"Ranks and counts for boys' baby names in England and Wales and also by region and month.\"," +
                    "\"keywords\":[\"top,ten,boys,girls,most,popular\"]," +
                    "\"metaDescription\":\"Ranks and counts for boys' baby names in England and Wales and also by region and month.\"," +
                    "\"nationalStatistic\":true," +
                    "\"contact\":{\"email\":\"vsob@ons.gov.uk\",\"name\":\"Elizabeth McLaren\",\"telephone\":\"+44 (0)1329 444110\"}," + "\"releaseDate\":\"2015-08-16T23:00:00.000Z\"," +
                    "\"nextRelease\":\"August - September 16 (provisional date)\"," +
                    "\"edition\":\"2014\"," +
                    "\"datasetId\":\"\"," +
                    "\"unit\":\"\",\"preUnit\":\"\"," +
                    "\"source\":\"\"," +
                    "\"versionLabel\":\"Testing\"}," +
                    "\"versions\":[{\"correctionNotice\":\"\"," +
                    "\"updateDate\":\"2022-05-03T11:31:21.283Z\"," +
                    "\"uri\":\"/peoplepopulationandcommunity/birthsdeathsandmarriages/livebirths/datasets/babynamesenglandandwalesbabynamesstatisticsboys/2014/previous/v1\"," +
                    "\"label\":\"Testing\"}]" +
                    "}";
}
