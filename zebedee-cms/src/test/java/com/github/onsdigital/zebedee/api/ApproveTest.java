package com.github.onsdigital.zebedee.api;

import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.exceptions.ConflictException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.session.service.Sessions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ApproveTest {

    private static final String COLLECTION_ID = "123456789";

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private Zebedee zebedee;

    @Mock
    private Sessions sessions;

    @Mock
    private com.github.onsdigital.zebedee.model.Collections collections;

    @Mock
    private Collection collection;

    @Mock
    private Session session;

    private Approve approveEndpoint;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        approveEndpoint = new Approve();

        Root.zebedee = zebedee;
        when(zebedee.getSessions()).thenReturn(sessions);
        when(zebedee.getCollections()).thenReturn(collections);
        when(sessions.get()).thenReturn(session);
        when(session.getEmail()).thenReturn("test@ons.gov.uk");
        when(request.getPathInfo()).thenReturn("/approve/" + COLLECTION_ID);
        when(request.getRequestURI()).thenReturn("/approve/" + COLLECTION_ID);
    }

    @Test
    public void approveCollection_shouldCloseCollectionOnSuccess() throws Exception {
        when(collections.getCollection(COLLECTION_ID, true)).thenReturn(collection);

        boolean approved = approveEndpoint.approveCollection(request, response);

        assertThat(approved, is(true));
        verify(collections).approve(collection, session);
        verify(collection).close();
    }

    @Test
    public void approveCollection_shouldCloseCollectionWhenApproveThrows() throws Exception {
        when(collections.getCollection(COLLECTION_ID, true)).thenReturn(collection);
        doThrow(new ConflictException("approval failed")).when(collections).approve(collection, session);

        assertThrows(ConflictException.class, () -> approveEndpoint.approveCollection(request, response));

        verify(collection).close();
    }

    @Test
    public void approveCollection_withoutSession_shouldThrowUnauthorized() {
        when(sessions.get()).thenReturn(null);

        assertThrows(UnauthorizedException.class, () -> approveEndpoint.approveCollection(request, response));
    }
}
