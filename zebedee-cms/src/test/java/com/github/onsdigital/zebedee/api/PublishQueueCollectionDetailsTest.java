package com.github.onsdigital.zebedee.api;

import com.github.onsdigital.zebedee.exceptions.CollectionNotFoundException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.json.ApprovalStatus;
import com.github.onsdigital.zebedee.json.CollectionDescription;
import com.github.onsdigital.zebedee.json.CollectionDetail;
import com.github.onsdigital.zebedee.json.CollectionType;
import com.github.onsdigital.zebedee.permissions.service.PermissionsService;
import com.github.onsdigital.zebedee.util.ZebedeeCmsService;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Date;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PublishQueueCollectionDetailsTest extends ZebedeeAPIBaseTestCase {

    private PublishingQueueCollectionDetails api;
    private CollectionDescription collectionDescription;

    @Mock
    private ZebedeeCmsService zebedeeCmsServiceMock;

    @Mock
    private PermissionsService permissionsServiceMock;

    @Mock
    private com.github.onsdigital.zebedee.model.Collection collectionMock;

    @Override
    protected void customSetUp() throws Exception {

        collectionDescription = new CollectionDescription();
        collectionDescription.setId("123");
        collectionDescription.setName("collectionName");
        collectionDescription.setType(CollectionType.scheduled);
        collectionDescription.setPublishDate(new Date());
        collectionDescription.setReleaseUri("releaseURI");
        collectionDescription.setApprovalStatus(ApprovalStatus.IN_PROGRESS);

        when(collectionMock.getDescription()).thenReturn(collectionDescription);
        when(zebedeeCmsServiceMock.getSession(mockRequest))
                .thenReturn(session);
        when(zebedeeCmsServiceMock.getPermissions())
                .thenReturn(permissionsServiceMock);
        when(zebedeeCmsServiceMock.getCollection(mockRequest))
                .thenReturn(collectionMock);
        when(permissionsServiceMock.canView(session.getEmail(), collectionDescription))
                .thenReturn(true);

        api = new PublishingQueueCollectionDetails(zebedeeCmsServiceMock);
    }

    @Override
    protected Object getAPIName() {
        return CollectionHistory.class.getSimpleName();
    }

    /**
     * Test verifies success case behaviour.
     */
    @Test
    public void shouldGet() throws Exception {
        when(permissionsServiceMock.canEdit(session.getEmail()))
                .thenReturn(true);

        CollectionDetail actual = api.get(mockRequest, mockResponse);

        assertThat(actual, notNullValue());

        assertThat(actual.getId(), equalTo(collectionDescription.getId()));
        assertThat(actual.getName(), equalTo(collectionDescription.getName()));
        assertThat(actual.getType(), equalTo(collectionDescription.getType()));
        assertThat(actual.getPublishDate(), equalTo(collectionDescription.getPublishDate()));
        assertThat(actual.getReleaseUri(), equalTo(collectionDescription.getReleaseUri()));
        assertThat(actual.approvalStatus, equalTo(collectionDescription.approvalStatus));

        verify(zebedeeCmsServiceMock, times(1)).getCollection(mockRequest);
        verify(zebedeeCmsServiceMock, times(1)).getSession(mockRequest);
        verify(zebedeeCmsServiceMock, times(1)).getPermissions();
        verify(permissionsServiceMock, times(1)).canView(session.getEmail(), collectionDescription);
    }

    /**
     * Test verifies the expected exception is thrown for a request with a non existent collection
     *
     * @throws Exception expected.
     */
    @Test(expected = CollectionNotFoundException.class)
    public void shouldThrowWhenCollectionNotFound() throws Exception {
        when(zebedeeCmsServiceMock.getCollection(mockRequest))
                .thenReturn(null);
        try {
            api.get(mockRequest, mockResponse);
        } catch (ZebedeeException zebEx) {
            verify(zebedeeCmsServiceMock, times(1)).getCollection(mockRequest);
            verify(zebedeeCmsServiceMock, never()).getSession(mockRequest);
            verify(zebedeeCmsServiceMock, never()).getPermissions();
            verify(permissionsServiceMock, never()).canView(session.getEmail(), collectionDescription);
            throw zebEx;
        }
    }

    /**
     * Test verifies the expected exception is thrown for a request with no session
     *
     * @throws Exception expected.
     */
    @Test(expected = UnauthorizedException.class)
    public void shouldThrowWhenNotLoggedIn() throws Exception {
        when(zebedeeCmsServiceMock.getSession(mockRequest))
                .thenReturn(null);
        try {
            api.get(mockRequest, mockResponse);
        } catch (ZebedeeException zebEx) {
            verify(zebedeeCmsServiceMock, times(1)).getCollection(mockRequest);
            verify(zebedeeCmsServiceMock, times(1)).getSession(mockRequest);
            verify(zebedeeCmsServiceMock, never()).getPermissions();
            verify(permissionsServiceMock, never()).canView(session.getEmail(), collectionDescription);
            throw zebEx;
        }
    }

    /**
     * Test verifies the expected exception is thrown for a request for a user without permission
     *
     * @throws Exception expected.
     */
    @Test(expected = UnauthorizedException.class)
    public void shouldThrowWithIncorrectPermissions() throws Exception {
        when(permissionsServiceMock.canView(session.getEmail(), collectionDescription))
                .thenReturn(false);
        try {
            api.get(mockRequest, mockResponse);
        } catch (ZebedeeException zebEx) {
            verify(zebedeeCmsServiceMock, times(1)).getCollection(mockRequest);
            verify(zebedeeCmsServiceMock, times(1)).getSession(mockRequest);
            verify(zebedeeCmsServiceMock, times(1)).getPermissions();
            verify(permissionsServiceMock, times(1)).canView(session.getEmail(), collectionDescription);
            throw zebEx;
        }
    }
}
