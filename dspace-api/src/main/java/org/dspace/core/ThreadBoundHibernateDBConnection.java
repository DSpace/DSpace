/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.core;

import java.sql.SQLException;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * Implementing class for the regular Context states for {@link HibernateDBConnection}
 */
public class ThreadBoundHibernateDBConnection extends HibernateDBConnection {

    @Autowired(required = true)
    @Qualifier("sessionFactory")
    private SessionFactory sessionFactory;

    public SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    @Override
    public Session getSession() throws SQLException {
        if (!isTransActionAlive()) {
            getSessionFactory().getCurrentSession().beginTransaction();
            configureDatabaseMode();
        }
        return getSessionFactory().getCurrentSession();
    }

    @Override
    public Session getCurrentSession() {
        return getSessionFactory().getCurrentSession();
    }

    @Override
    protected Transaction getTransaction() {
        return getCurrentSession().getTransaction();
    }

}