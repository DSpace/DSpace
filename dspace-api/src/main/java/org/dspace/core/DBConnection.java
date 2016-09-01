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
 * Interface representing a Database connection, this class should only be access by the context object.
 *
 * @author kevinvandevelde at atmire.com
 * @param <T> class type
 */
public interface DBConnection<T> {

    public T getSession() throws SQLException;

    public boolean isTransActionAlive();

    public boolean isSessionAlive();

    public void rollback() throws SQLException;

    public void closeDBConnection() throws SQLException;

    public void commit() throws SQLException;

    public void shutdown();

    public String getType();

    public DataSource getDataSource();

    public DatabaseConfigVO getDatabaseConfig() throws SQLException;
    
    public void setOptimizedForBatchProcessing(boolean batchOptimized) throws SQLException;

    public boolean isOptimizedForBatchProcessing();

    public long getCacheSize() throws SQLException;

    /**
     * Reload a DSpace object from the database. This will make sure the object is valid and stored in the cache.
     * @param entity The DSpace object to reload
     */
    public <E extends ReloadableEntity> E reloadEntity(E entity) throws SQLException;

    /**
     * Remove a DSpace object from the cache when batch processing a large number of objects.
     * @param entity The DSpace object to reload
     */
    public <E extends ReloadableEntity> void uncacheEntity(E entity) throws SQLException ;
}