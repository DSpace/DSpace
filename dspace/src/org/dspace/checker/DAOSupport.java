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
