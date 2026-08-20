/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.storage.rdbms;

import java.sql.SQLException;

import org.dspace.core.HibernateDBConnection;
import org.dspace.utils.DSpace;
import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
/**
 * A Hibernate database connection bound to an object rather than a thread, suitable for short-lived isolated
 * database operations that need to run alongside an existing thread-bound connection.
 */
public class ObjectBoundHibernateDBConnection extends HibernateDBConnection {

    private final SessionFactory sessionFactory;
    private Session session;
    private Transaction transaction;

    public ObjectBoundHibernateDBConnection() {
        this.sessionFactory = new DSpace().getServiceManager()
                                         .getServiceByName("sessionFactory", SessionFactory.class);
    }

    @Override
    public Session getSession() throws SQLException {
        if (session == null || !session.isOpen()) {
            session = sessionFactory.openSession();
        }

        if (transaction == null || !transaction.isActive()) {
            transaction = session.beginTransaction();
            configureDatabaseMode();
        }

        return session;
    }

    @Override
    public boolean isSessionAlive() {
        return session != null && session.isOpen();
    }

    @Override
    public boolean isTransActionAlive() {
        return transaction != null && transaction.isActive();
    }

    @Override
    protected Transaction getTransaction() {
        return transaction;
    }

    @Override
    public void commit() throws SQLException {
        if (isTransActionAlive()) {
            transaction.commit();
        }
    }

    @Override
    public void rollback() throws SQLException {
        if (isTransActionAlive()) {
            transaction.rollback();
        }
    }

    @Override
    public void closeDBConnection() throws SQLException {
        try {
            if (isTransActionAlive()) {
                transaction.rollback();
            }
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
            session = null;
            transaction = null;
        }
    }

    private void configureDatabaseMode() {
        if (isOptimizedForBatchProcessing()) {
            session.setHibernateFlushMode(FlushMode.ALWAYS);
        } else {
            session.setHibernateFlushMode(FlushMode.AUTO);
        }
    }
}