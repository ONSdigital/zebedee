package com.github.onsdigital.zebedee.api;

import com.github.onsdigital.zebedee.logging.click.event.ClickEventLogFactory;
import com.github.onsdigital.zebedee.model.ClickEvent;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.mock.web.DelegatingServletInputStream;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test verifies that {@link ClickEventLog} endpoint behaves correctly.
 */
public class ClickEventLogTest extends ZebedeeAPIBaseTestCase {

    private ClickEventLog endpoint;
    private List<String> elementClasses;
    private ClickEvent clickEvent;

    @Mock
    private ClickEventLogFactory clickEventLogFactoryMock;

    @Override
    protected void customSetUp() throws Exception {
        endpoint = new ClickEventLog();

        elementClasses = new ArrayList<>();
        elementClasses.add("btn-collection-work-on");

        ClickEvent.Trigger trigger = new ClickEvent.Trigger()
                .setElementId("123")
                .setElementClasses(elementClasses);

        ClickEvent.Collection collection = new ClickEvent.Collection()
                .setId("collectiontest1-0c4d242ddb74400842d942cfac067d48a8eb777e03534ca8229230f96ad50943")
                .setName("CollectionOne")
                .setType("manual");

        clickEvent = new ClickEvent()
                .setUser("Mr-meme@trollface.com")
                .setCollection(collection)
                .setTrigger(trigger);

        String clickEventJson = new ObjectMapper().writeValueAsString(clickEvent);

        // Convert the ClickEvent object to a JSON string and then convert that to an inputStream.
        InputStream inputStream = new ByteArrayInputStream(clickEventJson.getBytes(StandardCharsets.UTF_8));

        // Return this input stream from the request.
        when(mockRequest.getInputStream())
                .thenReturn(new DelegatingServletInputStream(inputStream));

        ReflectionTestUtils.setField(endpoint, "clickEventLogFactory", clickEventLogFactoryMock);
    }

    @Override
    protected Object getAPIName() {
        return ClickEventLog.class.getSimpleName();
    }

    @Test
    public void shouldLogClickEvent() throws Exception {
        endpoint.logEvent(mockRequest, mockResponse);

        verify(mockRequest, times(1)).getInputStream();
        verify(clickEventLogFactoryMock, times(1)).log(clickEvent);
    }
}
