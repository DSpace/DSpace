/*
 * GlobalDAOPostgres.java
 *
 * Version: $Revision: 1727 $
 *
 * Date: $Date: 2007-01-19 10:52:10 +0000 (Fri, 19 Jan 2007) $
 *
 * Copyright (c) 2002-2005, Hewlett-Packard Company and Massachusetts
 * Institute of Technology.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * - Neither the name of the Hewlett-Packard Company nor the name of the
 * Massachusetts Institute of Technology nor the names of their
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 */
package org.dspace.dao.postgres;

import java.sql.Connection;
import java.sql.SQLException;

import org.dspace.dao.GlobalDAO;
import org.dspace.storage.rdbms.DatabaseManager;

/**
 * @author James Rutherford
 */
public class GlobalDAOPostgres extends GlobalDAO
{
    private Connection connection;

    // FIXME: This should be a GlobalDAOException
    public GlobalDAOPostgres() throws SQLException
    {
        startTransaction();
    }

    public boolean transactionOpen()
    {
        return (connection != null);
    }

    public void startTransaction() throws SQLException
    {
        // Obtain a non-auto-committing connection
        connection = DatabaseManager.getConnection();
        connection.setAutoCommit(false);
    }

    public void endTransaction() throws SQLException
    {
        try
        {
            // Commit any changes made as part of the transaction
            connection.commit();
        }
        finally
        {
            // Free the connection
            DatabaseManager.freeConnection(connection);
            connection = null;
        }
    }

    public void saveTransaction() throws SQLException
    {
        connection.commit();
    }

    public void abortTransaction()
    {
        try
        {
            connection.rollback();
        }
        catch (SQLException sqle)
        {
            log.error(sqle.getMessage());
        }
        finally
        {
            DatabaseManager.freeConnection(connection);
            connection = null;
        }
    }

    /**
     * This method will only exist until no-one calls the RDBMS-centric
     * Context.getDBConnection() any more.
     */
    @Deprecated
    public Connection getConnection()
    {
        return connection;
    }
}
