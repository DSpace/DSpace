/*
 * Copyright (c) 2004-2005, Hewlett-Packard Company and Massachusetts
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
        ;
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
            LOG.error("Problem selecting checker result description. "
                    + e.getMessage(), e);
            throw new RuntimeException("selecting checker result description. "
                    + e.getMessage(), e);
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
    public List listAllCodes()
    {
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        List codes = new ArrayList();
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
            LOG.error("Problem listing checksum results codes: "
                    + e.getMessage(), e);
            throw new RuntimeException(
                    "Problem listing checksum results codes: " + e.getMessage(),
                    e);
        }
        finally
        {
            cleanup(stmt, conn, rs);
        }
    }
}
