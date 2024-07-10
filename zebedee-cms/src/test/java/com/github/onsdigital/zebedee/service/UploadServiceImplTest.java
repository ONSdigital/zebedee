package com.github.onsdigital.zebedee.service;

import com.github.onsdigital.dp.uploadservice.api.Client;
import junit.framework.TestCase;
import org.apache.hc.core5.http.NameValuePair;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.File;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

@RunWith(MockitoJUnitRunner.class)
public class UploadServiceImplTest extends TestCase {

    @Mock
    Client mockApiClient;

    @Mock
    List<NameValuePair> mockParams;

    @Mock
    File mockFile;

    @Test(expected = IllegalArgumentException.class)
    public void testWhenFileIsNullIllegalArgumentIsThrown() throws Exception {
        UploadService uploadService = new UploadServiceImpl(mockApiClient);
        uploadService.uploadResumableFile(null, mockParams);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWhenParamsIsNullIllegalArgumentIsThrown() throws Exception {
        UploadService uploadService = new UploadServiceImpl(mockApiClient);
        uploadService.uploadResumableFile(mockFile, null);
    }

    @Test
    public void testWhenArgumentsAreCorrect() throws Exception {
        UploadService uploadService = new UploadServiceImpl(mockApiClient);
        uploadService.uploadResumableFile(mockFile, mockParams);

        verify(mockApiClient, times(1)).uploadResumableFile(mockFile, mockParams);
    }
}
