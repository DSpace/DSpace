/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.storage.rdbms;

import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;

/**
 * A dirty cheat to make protected DatabaseUtils methods available for testing.
 *
 * @author mwood
 */
public class DatabaseUtilsHelpers
{
    /**
     * Get Flyway to set up the database with tables and such.
     *
     * @param ds a data source for the update.
     * @param cnx a database connection for the update.
     * @throws SQLException passed through.
     */
    static public void updateDatabase(DataSource ds, Connection cnx)
            throws SQLException
    {
        DatabaseUtils.updateDatabase(ds, cnx);
    }
}
