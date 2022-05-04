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
import com.github.onsdigital.zebedee.util.versioning.VersionsServiceImpl;
import org.apache.commons.io.file.PathUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

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
    private SecretKey secretKey;

    @Mock
    private PermissionsService mockPermissionsService;

    @Mock
    UsersService mockUsersService;

    @Test
    public void It_Should_Write_Content_To_File() throws Exception {
        Path tempBasePath = Files.createTempDirectory("tempZebedee");

        System.setProperty("ENABLE_DATASET_IMPORT", "false");
        System.setProperty("zebedee_root", tempBasePath.toString());

        MockHttpServletRequest request = new MockHttpServletRequest();
        String collectionId = "aktesting";
        request.setPathInfo("/content/" + collectionId);
        String datasetURL = "/peoplepopulationandcommunity/birthsdeathsandmarriages/livebirths/datasets/babynamesenglandandwalesbabynamesstatisticsboys/2022/data.json";
        request.addParameter("uri", datasetURL);

        CollectionDescription collectionDescription = new CollectionDescription("AK Testing");
        EncryptionKeyFactory encryptionKeyFactory = new EncryptionKeyFactoryImpl();

        VersionsServiceImpl versionsService = new VersionsServiceImpl();
        com.github.onsdigital.zebedee.model.Content content = new com.github.onsdigital.zebedee.model.Content(tempBasePath);

        Collections collections = new Collections(tempBasePath, mockPermissionsService, versionsService, content);

        when(mockPermissionsService.canEdit(mockSession)).thenReturn(true);

        when(mockSessions.get()).thenReturn(mockSession);

        when(mockCollections.getPath()).thenReturn(tempBasePath);

        when(mockZebedee.getCollections()).thenReturn(collections);
        when(mockZebedee.getSessions()).thenReturn(mockSessions);
        when(mockZebedee.getEncryptionKeyFactory()).thenReturn(encryptionKeyFactory);
        when(mockZebedee.getPermissionsService()).thenReturn(mockPermissionsService);
        when(mockZebedee.getUsersService()).thenReturn(mockUsersService);
        when(mockZebedee.getCollectionKeyring()).thenReturn(mockCollectionKeyring);
        Root.zebedee = mockZebedee;

        Collection collection = Collection.create(collectionDescription, mockZebedee, mockSession);
        when(mockCollections.getCollection(collectionId)).thenReturn(collection); // Do we still need to do this given we have a real collection (TRY DELETING THIS LINE)
        when(mockCollectionKeyring.get(any(), any())).thenReturn(secretKey);

        MockHttpServletResponse response = new MockHttpServletResponse();

        // "/peoplepopulationandcommunity/birthsdeathsandmarriages/livebirths/datasets/babynamesenglandandwalesbabynamesstatisticsboys/2022/data.json";

//        Path a = Paths.get(tempBasePath.toString(), collectionId, "peoplepopulationandcommunity");
//        Files.createDirectory(a);
//        Path b = Paths.get(tempBasePath.toString(), collectionId, "peoplepopulationandcommunity", "birthsdeathsandmarriages");
//        Files.createDirectory(b);
        Path c = Paths.get(tempBasePath.toString(), collectionId, "inprogress","peoplepopulationandcommunity", "birthsdeathsandmarriages", "livebirths", "datasets", "babynamesenglandandwalesbabynamesstatisticsboys", "2022");
//        Files.createDirectory(c);

        Path d = Files.createDirectories(c);
        Path versionFile = Paths.get(d.toString(), "data.json");
        Files.createFile(versionFile);

        Version versionApi = new Version();
        versionApi.create(newVersionRequest, newVersionResponse);
        Page pageApi = new Page(false);
        pageApi.createPage(newPageRequest, newPageResponse);

        try {
            Content contentApi = new Content();
            boolean result = contentApi.saveContent(request, response);
        } catch (Exception e) {
            Assert.fail(e.getMessage() + e.toString());
            e.printStackTrace();
            throw e;
        }

    }
}
