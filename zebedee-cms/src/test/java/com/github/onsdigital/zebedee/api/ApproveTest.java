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

import static com.github.onsdigital.zebedee.api.Approve.OVERRIDE_KEY_PARAM;
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

    private Approve api;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        api = new Approve();

        Root.zebedee = zebedee;
        when(zebedee.getSessions()).thenReturn(sessions);
        when(zebedee.getCollections()).thenReturn(collections);
        when(sessions.get()).thenReturn(session);
        when(session.getEmail()).thenReturn("test@ons.gov.uk");
        when(request.getPathInfo()).thenReturn("/approve/" + COLLECTION_ID);
        when(request.getRequestURI()).thenReturn("/approve/" + COLLECTION_ID);
    }

    @Test
    public void getOverrideKey_paramNull_shouldReturnNull() {
        when(request.getParameter(OVERRIDE_KEY_PARAM))
                .thenReturn(null);

        Long key = api.getOverrideKey(request);
        assertThat(key, is(nullValue()));
    }

    @Test
    public void getOverrideKey_paramEmpty_shouldReturnNull() {
        when(request.getParameter(OVERRIDE_KEY_PARAM))
                .thenReturn("");

        Long key = api.getOverrideKey(request);
        assertThat(key, is(nullValue()));
    }

    @Test
    public void getOverrideKey_nonNumericValue_shouldReturnNull() {
        when(request.getParameter(OVERRIDE_KEY_PARAM))
                .thenReturn("abc");

        Long key = api.getOverrideKey(request);
        assertThat(key, is(nullValue()));
    }

    @Test
    public void getOverrideKey_numericValue_shouldReturnValueAsLong() {
        when(request.getParameter(OVERRIDE_KEY_PARAM))
                .thenReturn("666");

        Long key = api.getOverrideKey(request);
        assertThat(key, equalTo(666L));
    }

    @Test
    public void approveCollection_shouldCloseCollectionOnSuccess() throws Exception {
        when(collections.getCollection(COLLECTION_ID, true)).thenReturn(collection);

        boolean approved = api.approveCollection(request, response);

        assertThat(approved, is(true));
        verify(collections).approve(collection, session, null);
        verify(collection).close();
    }

    @Test
    public void approveCollection_shouldCloseCollectionWhenApproveThrows() throws Exception {
        when(collections.getCollection(COLLECTION_ID, true)).thenReturn(collection);
        doThrow(new ConflictException("approval failed")).when(collections).approve(collection, session, null);

        assertThrows(ConflictException.class, () -> api.approveCollection(request, response));

        verify(collection).close();
    }

    @Test
    public void approveCollection_withoutSession_shouldThrowUnauthorized() {
        when(sessions.get()).thenReturn(null);

        assertThrows(UnauthorizedException.class, () -> api.approveCollection(request, response));
    }
}
