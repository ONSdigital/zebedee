package com.github.onsdigital.zebedee.util.mertics.service.client;

import com.github.onsdigital.zebedee.util.mertics.AbstractMetricsTest;
import com.splunk.Service;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Created by dave on 8/16/16.
 */
public class SplunkClientTest extends AbstractMetricsTest {

    @Mock
    private Service splunkServiceMock;

    private SplunkClient client;

    @Override
    public void before() throws Exception {
        MockitoAnnotations.initMocks(this);
        client = new SplunkClient(getTestServiceArgs());
        client.setSplunkService(splunkServiceMock);
    }

    @Override
    public void after() throws Exception {
        //
    }
}
