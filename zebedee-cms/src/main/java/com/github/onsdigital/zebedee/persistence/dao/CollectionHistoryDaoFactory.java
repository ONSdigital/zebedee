package com.github.onsdigital.zebedee.persistence.dao;

import com.github.onsdigital.zebedee.api.CollectionHistory;
import com.github.onsdigital.zebedee.configuration.Configuration;
import com.github.onsdigital.zebedee.persistence.HibernateServiceImpl;
import com.github.onsdigital.zebedee.persistence.dao.impl.CollectionHistoryDaoImpl;
import com.github.onsdigital.zebedee.persistence.dao.impl.CollectionHistoryDaoStub;

import static com.github.onsdigital.zebedee.configuration.Configuration.getAuditDBURL;
import static com.github.onsdigital.zebedee.configuration.Configuration.isAuditDatabaseEnabled;
import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logError;
import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logInfo;
import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logWarn;

/**
 * Factory implementation manages the creation and access to a {@link CollectionHistoryDao} implmentation.
 */
public class CollectionHistoryDaoFactory {

    private static final String STUB_DETAILS_MSG = "Application not connected to AUDIT database. "
            + CollectionHistoryDaoStub.class.getSimpleName() + " has been enabled. Events will only be written to the " +
            "application logs and " + CollectionHistory.class.getSimpleName() + " API will return an empty result set.";

    private static final String ERROR_MSG = "Error while attempting to connect to AUDIT database";
    private static final String AUDIT_DB_DISABLED_MSG = "Collection AUDIT database is DISABLED";
    private static final String AUDIT_DB_ENABLED_MSG = "Collection AUDIT database is ENABLED. Attempting connection";
    private static final String AUDIT_DB_CONNECTION_SUCCESS_MSG = "Successfully connected to AUDIT database";

    private static CollectionHistoryDao collectionHistoryDao = null;

    private CollectionHistoryDaoFactory() {
        // Util class hide constructor.
    }

    public static CollectionHistoryDao initialise() {
        return getCollectionHistoryDao();
    }

    /**
     * @return Implmentation of {@link CollectionHistoryDao}.
     * <ul>
     * <li>If {@link Configuration#isAuditDatabaseEnabled()} an attempt is made to connect to the audit database.
     * If successful the impl will allow read/write of history events to DB.</li>
     * <li>If there is any failure connecting to the database the exception is caught and logged and  the {@link CollectionHistoryDaoStub}
     * impl is returned allowing the application to continue running writing all collection history events to the
     * application logs.</li>
     * <li>If {@link Configuration#isAuditDatabaseEnabled()} is false then the {@link CollectionHistoryDaoStub} will
     * be returned - same as above
     * . </li></ul>
     */
    public static CollectionHistoryDao getCollectionHistoryDao() {
        if (collectionHistoryDao == null) {
            if (isAuditDatabaseEnabled()) {
                logInfo(AUDIT_DB_ENABLED_MSG).log();
                try {
                    CollectionHistoryDaoFactory.collectionHistoryDao = new CollectionHistoryDaoImpl
                            (HibernateServiceImpl.getInstance());
                    logInfo(AUDIT_DB_CONNECTION_SUCCESS_MSG).addParameter("databaseURL", getAuditDBURL()).log();
                } catch (Exception ex) {
                    logError(ex, ERROR_MSG).log();
                    setStubbedImpl();
                }
            } else {
                logWarn(AUDIT_DB_DISABLED_MSG).log();
                setStubbedImpl();

            }
        }
        return collectionHistoryDao;
    }

    private static void setStubbedImpl() {
        logWarn(STUB_DETAILS_MSG).log();
        CollectionHistoryDaoFactory.collectionHistoryDao = new CollectionHistoryDaoStub();
    }

    public static void setCollectionHistoryDao(CollectionHistoryDao collectionHistoryDao) {
        CollectionHistoryDaoFactory.collectionHistoryDao = collectionHistoryDao;
    }
}
