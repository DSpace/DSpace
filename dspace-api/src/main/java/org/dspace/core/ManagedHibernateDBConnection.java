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
 * Implementing class for the MANAGED Context state for {@link HibernateDBConnection}
 */
public class ManagedHibernateDBConnection extends HibernateDBConnection {

    @Autowired(required = true)
    @Qualifier("managedSessionFactory")
    private SessionFactory sessionFactory;

    private Session session;
    private Transaction transaction;

    public SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    @Override
    public Session getSession() {
        if (session == null) {
            this.session = getSessionFactory().openSession();
        }

        return session;
    }

    @Override
    public Session getCurrentSession() {
        return getSession();
    }

    @Override
    public Transaction getTransaction() {
        if (transaction == null) {
            this.transaction = getSession().beginTransaction();
        }
        return this.transaction;
    }

    @Override
    public void commit() throws SQLException {
        super.commit();
        transaction = null;
    }
}