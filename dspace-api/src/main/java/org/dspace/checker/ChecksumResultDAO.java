/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.checker;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.dspace.storage.rdbms.DatabaseManager;

/**
 * <p>
 * Database Access for the checksum results information.
 * </p>
 * 
 * @author Jim Downing
 * @author Grace Carpenter
 * @author Nathan Sarr
 * 
 */
public final class ChecksumResultDAO extends DAOSupport
{
    /**
     * Find a specified description.
     */
    private static final String FIND_CHECK_STRING = "select result_description "
            + "from checksum_results where result_code = ?";

    /**
     * Usual Log4J Logger.
     */
    private static final Logger LOG = Logger.getLogger(ChecksumResultDAO.class);

    /**
     * Default constructor
     */
    public ChecksumResultDAO()
    {
    }

    /**
     * Get the result description for the given result code
     * 
     * @param code
     *            to get the description for.
     * @return the found description.
     */
    public String getChecksumCheckStr(String code)
    {
        String description = null;
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try
        {
            conn = DatabaseManager.getConnection();
            stmt = conn.prepareStatement(FIND_CHECK_STRING);
            stmt.setString(1, code);

            rs = stmt.executeQuery();

            if (rs.next())
            {
                description = rs.getString(1);
            }
        }
        catch (SQLException e)
        {
            LOG.error("Problem selecting checker result description. " + e.getMessage(), e);
            throw new IllegalStateException("selecting checker result description. " + e.getMessage(), e);
        }
        finally
        {
            cleanup(stmt, conn, rs);
        }

        return description;
    }

    /**
     * Get a list of all the possible result codes.
     * 
     * @return a list of all the result codes
     */
    public List<String> listAllCodes()
    {
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        List<String> codes = new ArrayList<String>();
        try
        {
            conn = DatabaseManager.getConnection();
            stmt = conn.createStatement();

            rs = stmt.executeQuery("SELECT result_code FROM checksum_results");
            while (rs.next())
            {
                String code = rs.getString("result_code");
                codes.add(code);
            }
            return codes;
        }
        catch (SQLException e)
        {
            LOG.error("Problem listing checksum results codes: " + e.getMessage(), e);
            throw new IllegalStateException("Problem listing checksum results codes: " + e.getMessage(), e);
        }
        finally
        {
            cleanup(stmt, conn, rs);
        }
    }
}
