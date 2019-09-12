package com.github.onsdigital.zebedee.service;

import com.github.onsdigital.zebedee.model.ServiceAccount;
import com.github.onsdigital.zebedee.util.serialiser.JSONSerialiser;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class ServiceStoreImplTest {

    static final String TEST_TOKEN = "a2d53b6425de4549964b5ece2678ef60";

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Mock
    private JSONSerialiser<ServiceAccount> jsonSerialiser;

    private ServiceStore serviceStore;
    private Path servicePath;

    @Before
    public void setUp() throws IOException {
        MockitoAnnotations.initMocks(this);

        servicePath = temporaryFolder.newFolder("service").toPath();

        serviceStore = new ServiceStoreImpl(servicePath, jsonSerialiser);
    }

    @Test
    public void testGet_invalidToken() throws Exception {
        ServiceAccount account = serviceStore.get("");
        assertThat(account, is(nullValue()));
    }

    @Test
    public void testGet_accountNotFound() throws Exception {
        ServiceAccount actual = serviceStore.get(TEST_TOKEN);

        assertThat(actual, is(nullValue()));
        verifyZeroInteractions(jsonSerialiser);
    }

    @Test(expected = IOException.class)
    public void testGet_getServiceAccountPathIllegalArgumentEx() throws Exception {
        serviceStore = new ServiceStoreImpl(null, jsonSerialiser);

        try {
            serviceStore.get(TEST_TOKEN);
        } catch (IOException ex) {
            assertThat(ex.getMessage(), equalTo("error getting service account path for token"));
            verifyZeroInteractions(jsonSerialiser);
            throw ex;
        }
    }

    @Test(expected = IOException.class)
    public void testGet_jsonDeserialiseError() throws Exception {
        Path serviceAccountPath = Files.createFile(servicePath.resolve(TEST_TOKEN + ".json"));

        when(jsonSerialiser.deserialiseQuietly(any(InputStream.class), any(Path.class)))
                .thenThrow(new RuntimeException("nargle!"));

        try {
            serviceStore.get(TEST_TOKEN);
        } catch (IOException ex) {
            assertThat(ex.getMessage(), equalTo("error deserialising service account json"));
            verify(jsonSerialiser, times(1)).deserialiseQuietly(any(InputStream.class), eq(serviceAccountPath));
            throw ex;
        }
    }

    @Test
    public void testGet_success() throws Exception {
        Path serviceAccountPath = Files.createFile(servicePath.resolve(TEST_TOKEN + ".json"));
        ServiceAccount serviceAccount = new ServiceAccount("Weyland-Yutani Corporation");

        when(jsonSerialiser.deserialiseQuietly(any(InputStream.class), any(Path.class)))
                .thenReturn(serviceAccount);

        ServiceAccount actual = serviceStore.get(TEST_TOKEN);
        assertThat(actual.getID(), equalTo(serviceAccount.getID()));
        verify(jsonSerialiser, times(1)).deserialiseQuietly(any(InputStream.class), eq(serviceAccountPath));
    }

    @Test(expected = IOException.class)
    public void testStore_serviceRootNull() throws Exception {
        serviceStore = new ServiceStoreImpl(null, jsonSerialiser);

        try {
            serviceStore.store(null, null);
        } catch (IOException ex) {
            assertThat(ex.getMessage(), equalTo("error getting service account path for token"));
            verifyZeroInteractions(jsonSerialiser);
            throw ex;
        }
    }

    @Test(expected = IOException.class)
    public void testStore_fileAlreadyExists() throws Exception {
        Path p = Files.write(servicePath.resolve(TEST_TOKEN + ".json"), "nargle".getBytes());

        InputStream inputStream = mock(InputStream.class);
        try {
            serviceStore.store(TEST_TOKEN, inputStream);
        } catch (FileAlreadyExistsException ex) {
            assertThat(ex.getMessage(), equalTo("The service token already exists : " + p));
            verifyZeroInteractions(jsonSerialiser);
            throw ex;
        }
    }

    @Test
    public void testStore_success() throws Exception {
        InputStream inputStream = mock(InputStream.class);
        Path filePath = servicePath.resolve(TEST_TOKEN + ".json");

        ServiceAccount account = new ServiceAccount("Weyland Yutani Corp");


        when(jsonSerialiser.deserialiseQuietly(inputStream, filePath))
                .thenReturn(account);

        ServiceAccount actual = serviceStore.store(TEST_TOKEN, inputStream);

        assertThat(actual, equalTo(account));
        verify(jsonSerialiser, times(1)).deserialiseQuietly(inputStream, filePath);
    }
}
