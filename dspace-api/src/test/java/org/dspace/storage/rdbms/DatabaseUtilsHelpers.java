/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.storage.rdbms;

import java.sql.SQLException;

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
     * @throws SQLException
     */
    static public void updateDatabase()
            throws SQLException
    {
        DatabaseUtils.updateDatabase();
    }
}
