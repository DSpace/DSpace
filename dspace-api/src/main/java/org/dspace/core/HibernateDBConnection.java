/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.core;

import org.dspace.content.DSpaceObject;
import org.dspace.storage.rdbms.DatabaseConfigVO;
import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.proxy.HibernateProxyHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.orm.hibernate4.SessionFactoryUtils;

import javax.sql.DataSource;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

/**
 * Hibernate implementation of the DBConnection
 *
 * @author kevinvandevelde at atmire.com
 */
public class HibernateDBConnection implements DBConnection<Session> {

    @Autowired(required = true)
    @Qualifier("sessionFactory")
    private SessionFactory sessionFactory;

    private boolean batchModeEnabled = false;

    @Override
    public Session getSession() throws SQLException {
        if(!isTransActionAlive()){
            sessionFactory.getCurrentSession().beginTransaction();
            configureBatchMode();
        }
        return sessionFactory.getCurrentSession();
    }

    @Override
    public boolean isTransActionAlive() {
        Transaction transaction = getTransaction();
        return transaction != null && transaction.isActive();
    }

    protected Transaction getTransaction() {
        return sessionFactory.getCurrentSession().getTransaction();
    }

    @Override
    public boolean isSessionAlive() {
        return sessionFactory.getCurrentSession() != null && sessionFactory.getCurrentSession().getTransaction() != null && sessionFactory.getCurrentSession().getTransaction().isActive();
    }

    @Override
    public void rollback() throws SQLException {
        if(isTransActionAlive()){
            getTransaction().rollback();
        }
    }

    @Override
    public void closeDBConnection() throws SQLException {
        if(sessionFactory.getCurrentSession() != null && sessionFactory.getCurrentSession().isOpen())
        {
            sessionFactory.getCurrentSession().close();
        }
    }

    @Override
    public void commit() throws SQLException {
        if(isTransActionAlive() && !getTransaction().wasRolledBack())
        {
            getSession().flush();
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

    @Override
    @SuppressWarnings("unchecked")
    public <E extends ReloadableEntity> E reloadEntity(final E entity) throws SQLException {
        if(entity == null) {
            return null;
        } else if(getSession().contains(entity)) {
            return entity;
        } else {
            return (E) getSession().get(HibernateProxyHelper.getClassWithoutInitializingProxy(entity), entity.getID());
        }
    }

    @Override
    public void setOptimizedForBatchProcessing(final boolean batchOptimized) throws SQLException {
        this.batchModeEnabled = batchOptimized;
        configureBatchMode();
    }

    @Override
    public boolean isOptimizedForBatchProcessing() {
        return batchModeEnabled;
    }

    private void configureBatchMode() throws SQLException {
        if(batchModeEnabled) {
            getSession().setFlushMode(FlushMode.ALWAYS);
        } else {
            getSession().setFlushMode(FlushMode.AUTO);
        }
    }

    /**
     * Evict an entity from the hibernate cache. This is necessary when batch processing a large number of items.
     *
     * @param entity The entity to reload
     * @param <E> The class of the enity. The entity must implement the {@link ReloadableEntity} interface.
     * @throws SQLException When reloading the entity from the database fails.
     */
    @Override
    public <E extends ReloadableEntity> void uncacheEntity(E entity) throws SQLException {
        getSession().evict(entity);        
    }
}