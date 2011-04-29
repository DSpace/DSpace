/*
 * Copyright (c) 2002-2009, The DSpace Foundation.  All rights reserved.
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
 * - Neither the name of the DSpace Foundation nor the names of its
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

package org.dspace.checker;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.log4j.Logger;
import org.dspace.storage.rdbms.DatabaseManager;

/**
 * Database Helper Class to cleanup database resources
 * 
 * @author Jim Downing
 * @author Grace Carpenter
 * @author Nathan Sarr
 * 
 */
public class DAOSupport
{

    private static final Logger LOG = Logger.getLogger(DAOSupport.class);

    /**
     * Utility method that cleans up the statement and connection.
     * 
     * @param stmt
     *            A prepared statement to close.
     * @param conn
     *            Corresponding connection to close.
     */
    protected void cleanup(Statement stmt, Connection conn)
    {
        cleanup(stmt);
        if (conn != null)
        {
            DatabaseManager.freeConnection(conn);
        }
    }

    /**
     * Utility method that cleans up the statement and connection.
     * 
     * @param stmt
     *            A prepared statement to close.
     * @param conn
     *            Corresponding connection to close.
     * @param rs
     *            Result set to close
     */
    protected void cleanup(Statement stmt, Connection conn, ResultSet rs)
    {
        if (rs != null)
        {
            try
            {
                rs.close();
            }
            catch (SQLException e)
            {
                LOG.warn("Problem closing result set. " + e.getMessage(), e);
            }
        }
        cleanup(stmt);

        if (conn != null)
        {
            DatabaseManager.freeConnection(conn);
        }
    }

    protected void cleanup(Statement stmt)
    {
        if (stmt != null)
        {
            try
            {
                stmt.close();
            }
            catch (SQLException e)
            {
                LOG.warn("Problem closing prepared statement. "
                        + e.getMessage(), e);
            }
        }
    }

    protected void cleanup(Connection conn)
    {
        if (conn != null)
        {
            try
            {
                conn.close();
            }
            catch (SQLException e)
            {
                LOG.warn(e);
            }
        }
    }

}
