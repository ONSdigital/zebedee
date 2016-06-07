package com.github.onsdigital.zebedee.persistence;

import org.hibernate.SessionFactory;

/**
 * Created by dave on 6/7/16.
 */
public interface HibernateService {

    SessionFactory getSessionFactory();
}
