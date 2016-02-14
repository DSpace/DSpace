/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.core;

import org.dspace.storage.rdbms.DatabaseConfigVO;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.orm.hibernate4.SessionFactoryUtils;

import javax.sql.DataSource;
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

    @Override
    public Session getSession() throws SQLException {
        if(!isTransActionAlive()){
            sessionFactory.getCurrentSession().beginTransaction();
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
	public void clearCache() throws SQLException {
		this.getSession().clear();
	}
}