/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.core;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import javax.sql.DataSource;

import org.dspace.authorize.ResourcePolicy;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.handle.Handle;
import org.dspace.storage.rdbms.DatabaseConfigVO;
import org.hibernate.FlushMode;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.proxy.HibernateProxyHelper;
import org.hibernate.resource.transaction.spi.TransactionStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.orm.hibernate5.SessionFactoryUtils;

/**
 * Hibernate implementation of the DBConnection.
 * <P>
 * NOTE: This class does NOT represent a single Hibernate database connection. Instead, it wraps
 * Hibernate's Session object to obtain access to a database connection in order to execute one or more
 * transactions.
 * <P>
 * Per DSpace's current Hibernate configuration ([dspace]/config/core-hibernate.xml), we use the one-session-per-thread
 * approach (ThreadLocalSessionContext). This means that Hibernate creates a single Session per thread (request), at the
 * time when getCurrentSession() is first called.
 * <P>
 * This Session may be reused for multiple Transactions, but if commit() is called, any objects (Entities) in
 * the Session become disconnected and MUST be reloaded into the Session (see reloadEntity() method below).
 * <P>
 * If an Error occurs, the Session itself is invalidated. No further Transactions can be run on that Session.
 * <P>
 * DSpace generally follows the "Session-per-request" transactional pattern described here:
 * https://docs.jboss.org/hibernate/orm/5.0/userguide/en-US/html/ch06.html#session-per-request
 *
 *
 * @author kevinvandevelde at atmire.com
 */
public class HibernateDBConnection implements DBConnection<Session> {

    @Autowired(required = true)
    @Qualifier("sessionFactory")
    private SessionFactory sessionFactory;

    private boolean batchModeEnabled = false;
    private boolean readOnlyEnabled = false;

    /**
     * Retrieves the current Session from Hibernate (per our settings, Hibernate is configured to create one Session
     * per thread). If Session doesn't yet exist, it is created. A Transaction is also initialized (or reinintialized)
     * in the Session if one doesn't exist, or was previously closed (e.g. if commit() was previously called)
     * @return Hibernate current Session object
     * @throws SQLException
     */
    @Override
    public Session getSession() throws SQLException {
        // If we don't yet have a live transaction, start a new one
        // NOTE: a Session cannot be used until a Transaction is started.
        if (!isTransActionAlive()) {
            sessionFactory.getCurrentSession().beginTransaction();
            configureDatabaseMode();
        }
        // Return the current Hibernate Session object (Hibernate will create one if it doesn't yet exist)
        return sessionFactory.getCurrentSession();
    }

    /**
     * Check if the connection has a currently active Transaction. A Transaction is active if it has not yet been
     * either committed or rolled back.
     * @return
     */
    @Override
    public boolean isTransActionAlive() {
        Transaction transaction = getTransaction();
        return transaction != null && transaction.isActive();
    }

    /**
     * Retrieve the current Hibernate Transaction object from our Hibernate Session.
     * @return current Transaction (may be active or inactive) or null
     */
    protected Transaction getTransaction() {
        return sessionFactory.getCurrentSession().getTransaction();
    }

    /**
     * Check if Hibernate Session is still "alive" / open. An open Session may or may not have an open Transaction
     * (so isTransactionAlive() may return false even if isSessionAlive() returns true). A Session may be reused for
     * multiple transactions (e.g. if commit() is called, the Session remains alive while the Transaction is closed)
     *
     * @return true if Session is alive, false otherwise
     */
    @Override
    public boolean isSessionAlive() {
        return sessionFactory.getCurrentSession() != null && sessionFactory.getCurrentSession().isOpen();
    }

    /**
     * Rollback any changes applied to the current Transaction. This also closes the Transaction. A new Transaction
     * may be opened the next time getSession() is called.
     * @throws SQLException
     */
    @Override
    public void rollback() throws SQLException {
        if (isTransActionAlive()) {
            getTransaction().rollback();
        }
    }

    /**
     * Close our current Database connection. This also closes & unbinds the Hibernate Session from our thread.
     * <P>
     * NOTE: Because DSpace configures Hibernate to automatically create a Session per thread, a Session may still
     * exist after this method is called (as Hibernate may automatically create a new Session for the current thread).
     * However, Hibernate will automatically clean up any existing Session when the thread closes.
     * @throws SQLException
     */
    @Override
    public void closeDBConnection() throws SQLException {
        if (sessionFactory.getCurrentSession() != null && sessionFactory.getCurrentSession().isOpen()) {
            sessionFactory.getCurrentSession().close();
        }
    }

    /**
     * Commits any current changes cached in the Hibernate Session to the database & closes the Transaction.
     * To open a new Transaction, you may call getSession().
     * <P>
     * WARNING: When commit() is called, while the Session is still "alive", all previously loaded objects (entities)
     * become disconnected from the Session. Therefore, if you continue to use the Session, you MUST reload any needed
     * objects (entities) using reloadEntity() method.
     *
     * @throws SQLException
     */
    @Override
    public void commit() throws SQLException {
        if (isTransActionAlive() && !getTransaction().getStatus().isOneOf(TransactionStatus.MARKED_ROLLBACK,
                                                                          TransactionStatus.ROLLING_BACK)) {
            // Flush synchronizes the database with in-memory objects in Session (and frees up that memory)
            getSession().flush();
            // Commit those results to the database & ends the Transaction
            getTransaction().commit();
        }
    }

    @Override
    public synchronized void shutdown() {
        sessionFactory.close();
    }

    @Override
    public String getType() {
        return ((SessionFactoryImplementor) sessionFactory).getDialect().toString();
    }

    @Override
    public DataSource getDataSource() {
        return SessionFactoryUtils.getDataSource(sessionFactory);
    }

    @Override
    public DatabaseConfigVO getDatabaseConfig() throws SQLException {
        DatabaseConfigVO databaseConfigVO = new DatabaseConfigVO();

        try (Connection connection = getDataSource().getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            databaseConfigVO.setDatabaseDriver(metaData.getDriverName());
            databaseConfigVO.setDatabaseUrl(metaData.getURL());
            databaseConfigVO.setSchema(metaData.getSchemaTerm());
            databaseConfigVO.setMaxConnections(metaData.getMaxConnections());
            databaseConfigVO.setUserName(metaData.getUserName());
        }
        return databaseConfigVO;
    }


    @Override
    public long getCacheSize() throws SQLException {
        return getSession().getStatistics().getEntityCount();
    }

    /**
     * Reload an entity into the Hibernate cache. This can be called after a call to commit() to re-cache an object
     * in the Hibernate Session (see commit()). Failing to reload objects into the cache may result in a Hibernate
     * throwing a "LazyInitializationException" if you attempt to use an object that has been disconnected from the
     * Session cache.
     * @param entity The DSpace object to reload
     * @param <E> The class of the entity. The entity must implement the {@link ReloadableEntity} interface.
     * @return the newly cached object.
     * @throws SQLException
     */
    @Override
    @SuppressWarnings("unchecked")
    public <E extends ReloadableEntity> E reloadEntity(final E entity) throws SQLException {
        if (entity == null) {
            return null;
        } else if (getSession().contains(entity)) {
            return entity;
        } else {
            return (E) getSession().get(HibernateProxyHelper.getClassWithoutInitializingProxy(entity), entity.getID());
        }
    }

    @Override
    public void setConnectionMode(final boolean batchOptimized, final boolean readOnlyOptimized) throws SQLException {
        this.batchModeEnabled = batchOptimized;
        this.readOnlyEnabled = readOnlyOptimized;
        configureDatabaseMode();
    }

    @Override
    public boolean isOptimizedForBatchProcessing() {
        return batchModeEnabled;
    }

    private void configureDatabaseMode() throws SQLException {
        if (batchModeEnabled) {
            getSession().setHibernateFlushMode(FlushMode.ALWAYS);
        } else if (readOnlyEnabled) {
            getSession().setHibernateFlushMode(FlushMode.MANUAL);
        } else {
            getSession().setHibernateFlushMode(FlushMode.AUTO);
        }
    }

    /**
     * Evict an entity from the hibernate cache.
     * <P>
     * When an entity is evicted, it frees up the memory used by that entity in the cache. This is often
     * necessary when batch processing a large number of objects (to avoid out-of-memory exceptions).
     *
     * @param entity The entity to evict
     * @param <E>    The class of the entity. The entity must implement the {@link ReloadableEntity} interface.
     * @throws SQLException When reloading the entity from the database fails.
     */
    @Override
    public <E extends ReloadableEntity> void uncacheEntity(E entity) throws SQLException {
        if (entity != null) {
            if (entity instanceof DSpaceObject) {
                DSpaceObject dso = (DSpaceObject) entity;

                // The metadatavalue relation has CascadeType.ALL, so they are evicted automatically
                // and we don' need to uncache the values explicitly.

                if (Hibernate.isInitialized(dso.getHandles())) {
                    for (Handle handle : Utils.emptyIfNull(dso.getHandles())) {
                        uncacheEntity(handle);
                    }
                }

                if (Hibernate.isInitialized(dso.getResourcePolicies())) {
                    for (ResourcePolicy policy : Utils.emptyIfNull(dso.getResourcePolicies())) {
                        uncacheEntity(policy);
                    }
                }
            }

            // ITEM
            if (entity instanceof Item) {
                Item item = (Item) entity;

                //DO NOT uncache the submitter. This could be the current eperson. Uncaching could lead to
                //LazyInitializationExceptions (see DS-3648)

                if (Hibernate.isInitialized(item.getBundles())) {
                    for (Bundle bundle : Utils.emptyIfNull(item.getBundles())) {
                        uncacheEntity(bundle);
                    }
                }
                // BUNDLE
            } else if (entity instanceof Bundle) {
                Bundle bundle = (Bundle) entity;

                if (Hibernate.isInitialized(bundle.getBitstreams())) {
                    for (Bitstream bitstream : Utils.emptyIfNull(bundle.getBitstreams())) {
                        uncacheEntity(bitstream);
                    }
                }
                // BITSTREAM
                // No specific child entities to decache

                // COMMUNITY
            } else if (entity instanceof Community) {
                Community community = (Community) entity;

                // We don't uncache groups as they might still be referenced from the Context object

                if (Hibernate.isInitialized(community.getLogo())) {
                    uncacheEntity(community.getLogo());
                }

                // COLLECTION
            } else if (entity instanceof Collection) {
                Collection collection = (Collection) entity;

                //We don't uncache groups as they might still be referenced from the Context object

                if (Hibernate.isInitialized(collection.getLogo())) {
                    uncacheEntity(collection.getLogo());
                }
                if (Hibernate.isInitialized(collection.getTemplateItem())) {
                    uncacheEntity(collection.getTemplateItem());
                }
            }

            // Unless this object exists in the session, we won't do anything
            if (getSession().contains(entity)) {

                // If our Session has unsaved changes (dirty) and not READ-ONLY
                if (!readOnlyEnabled && getSession().isDirty()) {
                    // write changes to database (don't worry if transaction fails, flushed changes will be rolled back)
                    getSession().flush();
                }

                // Remove object from Session
                getSession().evict(entity);
            }
        }
    }
}
