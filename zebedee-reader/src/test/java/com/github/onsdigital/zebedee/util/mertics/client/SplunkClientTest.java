package com.github.onsdigital.zebedee.util.mertics.client;

import com.github.onsdigital.zebedee.util.mertics.AbstractMetricsTest;
import com.splunk.ResponseMessage;
import com.splunk.Service;
import org.apache.http.HttpStatus;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Consumer;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests verify the behaviour of the {@link SplunkClient}
 */
public class SplunkClientTest extends AbstractMetricsTest {

    @Mock
    private Service splunkServiceMock;

    @Mock
    private SplunkRequest splunkRequestMock;

    @Mock
    private ResponseMessage responseMessageMock;

    @Mock
    private Consumer<ResponseMessage> splunkErrorHandler;

    private SplunkClient client;

    @Override
    public void before() throws Exception {
        MockitoAnnotations.initMocks(this);
        client = new SplunkClient(getTestServiceArgs());
        client.setSplunkService(splunkServiceMock);
        client.setErrorResponseHandler(splunkErrorHandler);
    }

    @Override
    public void after() throws Exception {
        //
    }

    /**
     * Verify behaviour for success response.
     */
    @Test
    public void shouldSendMessageSuccessfully() throws ExecutionException, InterruptedException {
        when(splunkServiceMock.send(SPLUNK_HEC_URI, splunkRequestMock))
                .thenReturn(responseMessageMock);

        when(responseMessageMock.getStatus())
                .thenReturn(HttpStatus.SC_OK);

        Future future = client.send(SPLUNK_HEC_URI, splunkRequestMock);
        future.get();

        verify(splunkServiceMock, times(1)).send(SPLUNK_HEC_URI, splunkRequestMock);
        verify(splunkErrorHandler, never()).accept(responseMessageMock);
    }

    /**
     * Verify behaviour for error response.
     */
    @Test
    public void shouldHandleErrorResponse() throws ExecutionException, InterruptedException {
        when(splunkServiceMock.send(SPLUNK_HEC_URI, splunkRequestMock))
                .thenReturn(responseMessageMock);

        when(responseMessageMock.getStatus())
                .thenReturn(HttpStatus.SC_INTERNAL_SERVER_ERROR);

        Future future = client.send(SPLUNK_HEC_URI, splunkRequestMock);
        future.get();

        verify(splunkServiceMock, times(1)).send(SPLUNK_HEC_URI, splunkRequestMock);
        verify(splunkErrorHandler, times(1)).accept(responseMessageMock);
    }

    /**
     * Verify behaviour for no response.
     */
    @Test
    public void shouldHandleNullResponse() throws ExecutionException, InterruptedException {
        when(splunkServiceMock.send(SPLUNK_HEC_URI, splunkRequestMock))
                .thenReturn(null);

        Future future = client.send(SPLUNK_HEC_URI, splunkRequestMock);
        future.get();

        verify(splunkServiceMock, times(1)).send(SPLUNK_HEC_URI, splunkRequestMock);
        verify(splunkErrorHandler, times(1)).accept(null);
    }
}
