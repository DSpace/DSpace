/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.core;

import org.dspace.storage.rdbms.DatabaseConfigVO;

import javax.sql.DataSource;
import java.sql.SQLException;

/**
 * Interface representing a persistence provider "session".
 * Implementations will wrap something like a JPA EntityManager or a Hibernate
 * Session.  The terms "database" and "connection" are historic and have no
 * direct relationship to a JDBC Connection or even a connection pool.
 *
 * <p> This class should only be accessed by an enclosing {@link Context} object.
 *
 * <p> <em>Note</em> that the user's HTTPSession is an unrelated concept.
 *
 * @author kevinvandevelde at atmire.com
 * @param <T> type of the persistence provider's session object.
 */
public interface DBConnection<T> {

    /**
     * Access to the underlying persistence provider's session object.
     * @return the provider's session object for this connection.
     * @throws SQLException passed through.
     */
    public T getSession() throws SQLException;

    /**
     * @return true if this session has an uncommitted transaction.
     */
    public boolean isTransActionAlive();

    /**
     * @return true if the session is open, false if it has been closed.
     */
    public boolean isSessionAlive();

    /**
     * Commit the open transaction.
     * @throws SQLException passed through.
     */
    public void commit() throws SQLException;

    /**
     * Roll back the open transaction.
     * @throws SQLException passed through.
     */
    public void rollback() throws SQLException;

    /**
     * Close this session:  close DBMS connection(s) and clean up resources.
     * @throws SQLException passed through.
     */
    public void closeDBConnection() throws SQLException;

    /**
     * Close all sessions.  Release all associated resources (cache, DBMS
     * connections, etc.)  To be used only when exiting the application.
     */
    public void shutdown();

    /**
     * Some description of the DBMS used to persist entities.
     * @return Brand, version, dialect, etc.  Implementation specific.
     */
    public String getType();

    /**
     * The JDBC DataSource used by this session.  Think carefully before using.
     * @return the source of DBMS connections.
     */
    public DataSource getDataSource();

    /**
     * Identify certain characteristics of the DBMS being used to support persistence.
     * @return a collection of DBMS, database and connection information.
     * @throws SQLException passed through.
     */
    public DatabaseConfigVO getDatabaseConfig() throws SQLException;

    /**
     * Configure the connection for special uses.
     * @param batchOptimized if true, optimize for batch use.  Typically this
     *  means suppressing automatic flushing of updates, thus requiring manual
     *  flushing at appropriate points in the process.
     * @param readOnlyOptimized if true, optimize for read-only use.  Typically
     *  this suppresses all updating.
     * @throws SQLException
     */
    public void setConnectionMode(boolean batchOptimized, boolean readOnlyOptimized) throws SQLException;

    /**
     * Has this session been configured for large batches?  Typically this means
     * that automatic flushing of updates to the database is suppressed, and
     * thus one must take care to flush manually (or commit) at appropriate times.
     * @return true if configured for batch.
     */
    public boolean isOptimizedForBatchProcessing();

    /**
     * How many entities are cached in this session?
     * @return number of cached entities.
     * @throws SQLException passed through.
     */
    public long getCacheSize() throws SQLException;

    /**
     * Reload a DSpace object from the database. This will make sure the object
     * is valid and stored in the cache.  The returned object should be used
     * henceforth instead of the passed object.
     * @param <E> type of {@link entity}
     * @param entity The DSpace object to reload
     * @return the reloaded entity.
     * @throws java.sql.SQLException passed through.
     */
    public <E extends ReloadableEntity> E reloadEntity(E entity) throws SQLException;

    /**
     * Remove a DSpace object from the session cache when batch processing a
     * large number of objects.
     *
     * <p>Objects removed from cache are not saved in any way. Therefore, if you
     * have modified an object, you should be sure to {@link commit()} changes
     * before calling this method.
     * @param <E> Type of {@link entity}
     * @param entity The DSpace object to decache.
     * @throws java.sql.SQLException
     */
    public <E extends ReloadableEntity> void uncacheEntity(E entity) throws SQLException ;
}