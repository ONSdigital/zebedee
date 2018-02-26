package com.github.onsdigital.zebedee.persistence.dao.impl;

import com.github.davidcarboni.cryptolite.Random;
import com.github.onsdigital.zebedee.exceptions.CollectionEventHistoryException;
import com.github.onsdigital.zebedee.json.CollectionDescription;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.persistence.CollectionEventType;
import com.github.onsdigital.zebedee.persistence.HibernateServiceImpl;
import com.github.onsdigital.zebedee.persistence.dao.CollectionHistoryDao;
import com.github.onsdigital.zebedee.persistence.model.CollectionEventMetaData;
import com.github.onsdigital.zebedee.persistence.model.CollectionHistoryEvent;
import org.hibernate.HibernateException;
import org.hibernate.SQLQuery;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

import static com.github.onsdigital.zebedee.persistence.dao.impl.CollectionHistoryDaoImpl.COLLECTION_ID;
import static com.github.onsdigital.zebedee.persistence.dao.impl.CollectionHistoryDaoImpl.SELECT_BY_COLLECTION_ID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests verifying {@link CollectionHistoryDaoImpl} behaves as expected in success and error scenarios.
 */
public class CollectionHistoryDaoImplTest {

    private static final String COLLECTION_NAME = "test-collection";
    private static final String TEST_COLLECTION_ID = COLLECTION_NAME + "-" + Random.id();

    @Mock
    private HibernateServiceImpl hibernateServiceMock;

    @Mock
    private SessionFactory sessionFactoryMock;

    @Mock
    private org.hibernate.Session hibernateSessionMock;

    @Mock
    private Transaction transactionMock;

    @Mock
    private SQLQuery sqlQueryMock;

    @Mock
    private Collection collectionMock;

    @Mock
    private ExecutorService threadPoolMock;

    private CollectionHistoryDaoImpl dao;
    private CollectionHistoryEvent event;
    private CollectionEventType eventType;
    private CollectionDescription collectionDescription;
    private Session session;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        session = new Session();
        session.setEmail("tyrionLannister@test.com");

        dao = new CollectionHistoryDaoImpl(hibernateServiceMock);
        eventType = randomEvent();
        event = new CollectionHistoryEvent(TEST_COLLECTION_ID, COLLECTION_NAME, session, eventType);
        collectionDescription = new CollectionDescription(COLLECTION_NAME);
        collectionDescription.setId(TEST_COLLECTION_ID);

        when(hibernateServiceMock.getSessionFactory())
                .thenReturn(sessionFactoryMock);
        when(sessionFactoryMock.getCurrentSession())
                .thenReturn(hibernateSessionMock);
        when(hibernateSessionMock.getTransaction())
                .thenReturn(transactionMock);
        when(collectionMock.getDescription())
                .thenReturn(collectionDescription);
    }

    /**
     * Success test case for {@link CollectionHistoryDao#saveCollectionHistoryEvent(CollectionHistoryEvent)}
     */
    @Test
    public void shouldSaveEventSuccessfully() throws Exception {
        dao.saveCollectionHistoryEvent(event).get();
        saveEventSuccessVerifications();
    }



    /**
     * Exception test case for {@link CollectionHistoryDao#saveCollectionHistoryEvent(CollectionHistoryEvent)}
     */
    @Test(expected = ExecutionException.class)
    public void shouldLogAndThrowZebedeeExceptionOnSaveWithError() throws Exception {
        when(sessionFactoryMock.getCurrentSession())
                .thenThrow(new HibernateException("Whoops!"));

        try {
            dao.saveCollectionHistoryEvent(event).get();
        } catch (Exception ex) {
            saveEventHistoryFailureVerifications();
            throw ex;
        }
    }

    /**
     * Success test case for {@link CollectionHistoryDao#saveCollectionHistoryEvent(String, String, Session,
     * CollectionEventType, CollectionEventMetaData...)}
     */
    @Test
    public void shouldSaveEventSuccessfullyFromIdAndName() throws Exception {
        dao.saveCollectionHistoryEvent(TEST_COLLECTION_ID, COLLECTION_NAME, session, eventType).get();
        saveEventSuccessVerifications();
    }

    /**
     * Exception test case for {@link CollectionHistoryDao#saveCollectionHistoryEvent(String, String, Session,
     * CollectionEventType, CollectionEventMetaData...)}
     */
    @Test(expected = ExecutionException.class)
    public void shouldLogAndThrowExceptionForSaveEventFromIdAndNameWithError() throws Exception {
        when(sessionFactoryMock.getCurrentSession())
                .thenThrow(new HibernateException("Whoops!"));

        try {
            dao.saveCollectionHistoryEvent(TEST_COLLECTION_ID, COLLECTION_NAME, session, eventType).get();
        } catch (Exception ex) {
            saveEventHistoryFailureVerifications();
            throw ex;
        }
    }

    /**
     * Success test case for {@link CollectionHistoryDao#saveCollectionHistoryEvent(Collection, Session,
     * CollectionEventType, CollectionEventMetaData...)}
     */
    @Test
    public void shouldSaveEventSuccessfullyFromCollectionAndSession() throws Exception {
        dao.saveCollectionHistoryEvent(collectionMock, session, event.getEventType()).get();
        saveEventSuccessVerifications();
    }

    /**
     * Exception test case for {@link CollectionHistoryDao#saveCollectionHistoryEvent(Collection, Session,
     * CollectionEventType, CollectionEventMetaData...)}
     */
    @Test(expected = ExecutionException.class)
    public void shouldLogAndThrowExceptionForSaveEventFromCollectionDescriptionWithError() throws Exception {
        when(sessionFactoryMock.getCurrentSession())
                .thenThrow(new HibernateException("Whoops!"));

        try {
            dao.saveCollectionHistoryEvent(collectionMock, session, event.getEventType()).get();
        } catch (ExecutionException ex) {
            saveEventHistoryFailureVerifications();
            throw ex;
        }
    }

    /**
     * Success test case for {@link CollectionHistoryDao#getCollectionEventHistory(String)}
     */
    @Test
    public void shouldGetCollectionHistory() throws Exception {
        List<CollectionHistoryEvent> expectedResult = new ArrayList<>();
        expectedResult.add(event);

        when(hibernateSessionMock.createSQLQuery(SELECT_BY_COLLECTION_ID))
                .thenReturn(sqlQueryMock);
        when(sqlQueryMock.addEntity(CollectionHistoryEvent.class))
                .thenReturn(sqlQueryMock);
        when(sqlQueryMock.setString(COLLECTION_ID, TEST_COLLECTION_ID))
                .thenReturn(sqlQueryMock);
        when(sqlQueryMock.list())
                .thenReturn(expectedResult);

        // run the test.
        assertThat(dao.getCollectionEventHistory(TEST_COLLECTION_ID), equalTo(expectedResult));

        verify(hibernateServiceMock, times(1)).getSessionFactory();
        verify(sessionFactoryMock, times(1)).getCurrentSession();
        verify(hibernateSessionMock, times(1)).beginTransaction();
        verify(hibernateSessionMock, times(1)).createSQLQuery(SELECT_BY_COLLECTION_ID);
        verify(sqlQueryMock, times(1)).addEntity(CollectionHistoryEvent.class);
        verify(sqlQueryMock, times(1)).setString(COLLECTION_ID, TEST_COLLECTION_ID);
        verify(sqlQueryMock, times(1)).list();
        verify(hibernateSessionMock, times(1)).getTransaction();
        verify(transactionMock, times(1)).commit();
    }

    /**
     * Failure test case for {@link CollectionHistoryDao#getCollectionEventHistory(String)}.
     */
    @Test(expected = CollectionEventHistoryException.class)
    public void ShouldLogAndThrowExceptionForErrorsGettingHistory() throws Exception {
        when(sessionFactoryMock.getCurrentSession())
                .thenThrow(new HibernateException("Whoops!"));

        try {
            dao.getCollectionEventHistory(TEST_COLLECTION_ID);
        } catch (HibernateException ex) {
            verify(hibernateServiceMock, times(1)).getSessionFactory();
            verify(sessionFactoryMock, times(1)).getCurrentSession();

            verify(hibernateSessionMock, never()).beginTransaction();
            verify(hibernateSessionMock, never()).createSQLQuery(anyString());
            verify(sqlQueryMock, never()).addEntity(CollectionHistoryEvent.class);
            verify(sqlQueryMock, never()).setString(any(), any());
            verify(sqlQueryMock, never()).list();
            verify(hibernateSessionMock, never()).getTransaction();
            verify(transactionMock, never()).commit();
            throw ex;
        }
    }

    private void saveEventHistoryFailureVerifications() {
        verify(hibernateServiceMock, times(1)).getSessionFactory();
        verify(sessionFactoryMock, times(1)).getCurrentSession();
        verify(hibernateSessionMock, never()).beginTransaction();
        verify(hibernateSessionMock, never()).save(event);
        verify(hibernateSessionMock, never()).getTransaction();
        verify(transactionMock, never()).commit();
    }

    private void saveEventSuccessVerifications() {
        verify(hibernateServiceMock, times(1)).getSessionFactory();
        verify(sessionFactoryMock, times(1)).getCurrentSession();
        verify(hibernateSessionMock, times(1)).beginTransaction();
        verify(hibernateSessionMock, times(1)).save(event);
        verify(hibernateSessionMock, times(1)).getTransaction();
        verify(transactionMock, times(1)).commit();
    }

    private CollectionEventType randomEvent() {
        List<CollectionEventType> types = Arrays.asList(CollectionEventType.values());
        Collections.shuffle(types);
        return types.get(0);
    }
}
