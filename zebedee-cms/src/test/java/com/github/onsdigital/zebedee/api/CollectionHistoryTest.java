package com.github.onsdigital.zebedee.api;

import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.model.Permissions;
import com.github.onsdigital.zebedee.persistence.dao.CollectionHistoryDao;
import com.github.onsdigital.zebedee.persistence.model.CollectionHistoryEvent;
import com.github.onsdigital.zebedee.util.ZebedeeApiHelper;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static com.github.onsdigital.zebedee.persistence.dao.CollectionHistoryDao.getCollectionHistoryDao;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

/**
 * Created by dave on 6/3/16.
 */
public class CollectionHistoryTest extends ZebedeeAPIBaseTestCase {

    private CollectionHistory api;

    @Mock
    private ZebedeeApiHelper apiHelperMock;

    @Mock
    private Permissions permissionsMock;

    @Mock
    private CollectionHistoryDao mockDao;

    private List<CollectionHistoryEvent> eventList;

    @Override
    protected void customSetUp() throws Exception {
        this.api = new CollectionHistory();

        eventList = getCollectionHistoryDao().getCollectionEventHistory(COLLECTION_ID);

        when(apiHelperMock.getSession(mockRequest))
                .thenReturn(session);
        when(apiHelperMock.getPermissions())
                .thenReturn(permissionsMock);

        ReflectionTestUtils.setField(api, "apiHelper", apiHelperMock);
        ReflectionTestUtils.setField(api, "collectionHistoryDao", mockDao);
    }

    @Override
    protected Object getAPIName() {
        return CollectionHistory.class.getSimpleName();
    }

    /**
     * Test verifies success case behaviour.
     */
    @Test
    public void getCollectionHistorySuccess() throws Exception {
        when(permissionsMock.canEdit(session.email))
                .thenReturn(true);

        com.github.onsdigital.zebedee.model.collection.audit.CollectionHistory result
                = api.getCollectionEventHistory(mockRequest, mockResponse);

        com.github.onsdigital.zebedee.model.collection.audit.CollectionHistory expectedResult =
                new com.github.onsdigital.zebedee.model.collection.audit.CollectionHistory(eventList);

        assertThat(result, equalTo(expectedResult));

        verify(apiHelperMock, times(1)).getSession(mockRequest);
        verify(apiHelperMock, times(1)).getPermissions();
        verify(permissionsMock, times(1)).canEdit(session.email);
        //verify(mockDao, times(1)).getCollectionEventHistory(COLLECTION_ID);
    }

    /**
     * Test verifies the expected exception is thrown when a request with no session tries to request the collection
     * event history.
     *
     * @throws Exception expected.
     */
    @Test(expected = UnauthorizedException.class)
    public void testGetCollectionHistoryWhenNotLoggedIn() throws Exception {
        when(apiHelperMock.getSession(mockRequest))
                .thenReturn(null);
        try {
            api.getCollectionEventHistory(mockRequest, mockResponse);
        } catch (ZebedeeException zebEx) {
            verify(apiHelperMock, times(1)).getSession(mockRequest);
            verify(apiHelperMock, never()).getPermissions();
            verify(permissionsMock, never()).canEdit(session.email);
            verify(mockDao, never()).getCollectionEventHistory(COLLECTION_ID);
            throw zebEx;
        }
    }

    /**
     * Test verifies the expected exception is thrown when a user with a valid session tries to request the collection
     * event history when they do not have the necessary permissions.
     *
     * @throws Exception expected.
     */
    @Test(expected = UnauthorizedException.class)
    public void testGetCollectionHistoryIncorrectPermissions() throws Exception {
        when(permissionsMock.canEdit(session.email))
                .thenReturn(false);
        try {
            api.getCollectionEventHistory(mockRequest, mockResponse);
        } catch (ZebedeeException zebEx) {
            verify(apiHelperMock, times(1)).getSession(mockRequest);
            verify(apiHelperMock, times(1)).getPermissions();
            verify(permissionsMock, times(1)).canEdit(session.email);
            verify(mockDao, never()).getCollectionEventHistory(COLLECTION_ID);
            throw zebEx;
        }
    }
}
