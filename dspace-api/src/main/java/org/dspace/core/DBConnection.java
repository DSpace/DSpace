/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.core;

import java.sql.SQLException;

/**
 * Interface representing a Database connection, this class should only be access by the context object.
 *
 * @author kevinvandevelde at atmire.com
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
}