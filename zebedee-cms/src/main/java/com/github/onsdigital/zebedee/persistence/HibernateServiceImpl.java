package com.github.onsdigital.zebedee.persistence;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logError;

/**
 * Provides access to {@link SessionFactory}.
 */
public class HibernateServiceImpl implements HibernateService {

    private static HibernateServiceImpl instance = null;
    private SessionFactory sessionFactory = null;

    private HibernateServiceImpl() {
        this.sessionFactory = buildSessionFactory();
    }

    public static HibernateServiceImpl getInstance() {
        if (instance == null) {
            instance = new HibernateServiceImpl();
        }
        return instance;
    }

    private static SessionFactory buildSessionFactory() {
        try {
            // Create the SessionFactory from hibernate.cfg.xml
            Configuration config = new Configuration();
            config.configure();

            config.setProperty("hibernate.connection.url", com.github.onsdigital.zebedee.configuration.Configuration.getAuditDBURL());
            config.setProperty("hibernate.connection.username", com.github.onsdigital.zebedee.configuration.Configuration.getAuditDBUsername());
            config.setProperty("hibernate.connection.password", com.github.onsdigital.zebedee.configuration.Configuration.getAuditDBPassword());

            return config.buildSessionFactory();
        }
        catch (Throwable ex) {
            // Make sure you log the exception, as it might be swallowed
            logError(ex, "Hibernate Error: Initial SessionFactory creation failed.")
                    .throwUnchecked(ex);
            return null;
        }
    }

    public SessionFactory getSessionFactory() {
        if (sessionFactory == null) {
            sessionFactory = buildSessionFactory();
        }
        return sessionFactory;
    }
}
