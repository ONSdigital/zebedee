package com.github.onsdigital.zebedee.persistence;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

/**
 * Created by dave on 5/26/16.
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
            return new Configuration().configure().buildSessionFactory();
        }
        catch (Throwable ex) {
            // Make sure you log the exception, as it might be swallowed
            System.err.println("Initial SessionFactory creation failed." + ex);
            throw new ExceptionInInitializerError(ex);
        }
    }

    public SessionFactory getSessionFactory() {
        if (sessionFactory == null) {
            sessionFactory = buildSessionFactory();
        }
        return sessionFactory;
    }
}
