/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.checker;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import org.apache.log4j.Logger;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;

/**
 * <p>
 * Use to iterate over bitstreams selected based on information stored in MOST_RECENT_CHECKSUM
 * </p>
 *
 * @author Monika Mevenkamp
 */
public final class CheckBitstreamIterator extends QueryBuilder
{
    private static final Logger LOG = Logger.getLogger(CheckBitstreamIterator.class);

    private Date before = null, after = null;
    private PreparedStatement sqlStmt = null;
    private ResultSet rs = null;

    public CheckBitstreamIterator(Context ctxt,
                                   String has_result, String[] has_not_results,
                                   Date before_date, Date after_date,
                                   DSpaceObject inside)
    {
        super(ctxt, has_result, has_not_results, inside);
        after = after_date;
        before = before_date;
        rs = null;
    }

    // TODO return BitstreamInfo instance or null
    public boolean next() throws SQLException
    {
        if (rs == null)
        {
            String stmt = "SELECT MRC.bitstream_id, MRC.result, MRC.last_process_end_date, MRC.current_checksum, \n" +
                    "BITSTREAM.DELETED, BITSTREAM.internal_id, BITSTREAM.checksum_algorithm, BITSTREAM.checksum \n" +
                    "FROM MOST_RECENT_CHECKSUM MRC JOIN BITSTREAM ON BITSTREAM.BITSTREAM_ID = MRC.BITSTREAM_ID ";
            sqlStmt = prepare(stmt);
            rs = sqlStmt.executeQuery();
        }
        boolean nxt = rs.next();
        if (! nxt)
        {
            sqlStmt.close();
        }
        return nxt;
    }

    public void close() throws SQLException
    {
        if (sqlStmt != null) {
            sqlStmt.close();
        }
    }

    public int bitstream_id() throws SQLException
    {
        return rs.getInt(1);
    }

    public String result() throws SQLException
    {
        return rs.getString(2);
    }

    public Timestamp last_process_end_date() throws SQLException
    {
        return rs.getTimestamp(3);
    }

    public String checksum() throws SQLException
    {
        return rs.getString(4);
    }

    public boolean deleted() throws SQLException
    {
        return rs.getBoolean(5);
    }

    public String internalId() throws SQLException
    {
        return rs.getString(6);
    }

    public String checksumAlgo() throws SQLException
    {
        return rs.getString(7);
    }

    public String storedChecksum() throws SQLException
    {
        return rs.getString(8);
    }



    public int count() throws SQLException
    {
        String stmt = "SELECT COUNT(*) AS N FROM MOST_RECENT_CHECKSUM MRC ";
        LOG.debug(stmt);
        ResultSet rs = prepare(stmt).executeQuery();
        rs.next();
        return rs.getInt(1);
    }

    private PreparedStatement prepare(String stmt) throws SQLException {
        String operator = " WHERE ";
        if (before != null)
        {
            stmt = stmt + operator + " MRC.LAST_PROCESS_END_DATE < ? ";
            operator = " AND ";
        }
        if (after != null)
        {
            stmt = stmt + operator + " MRC.LAST_PROCESS_END_DATE >= ?";
            operator = " AND ";
        }
        stmt = stmt + where_clause(operator, "MRC");
        stmt = stmt + " ORDER BY LAST_PROCESS_END_DATE";

        PreparedStatement sqlStmt = prepareStatement(stmt);
        int pos = 1;
        if (before != null)
        {
            sqlStmt.setTimestamp(pos, new Timestamp(before.getTime()));
            if (LOG.isDebugEnabled())
                LOG.debug("pos " + pos + " " + before);
            pos++;
        }
        if (after != null)
        {
            sqlStmt.setTimestamp(pos, new Timestamp(after.getTime()));
            if (LOG.isDebugEnabled())
                LOG.debug("pos " + pos + " " + after);
            pos++;
        }
        return  prepare(pos, sqlStmt);
    }

    public String toString()
    {
        return getClass().getCanonicalName() + "(" + propertyString(", ") + ")";
    }

    public String propertyString(String sep)
    {
        String me = super.propertyString(sep);
        String  s = (me.isEmpty()) ? "" : sep;
        if (after != null) {
            me = me + s + "after=" + after.toString();
            s = sep;
        }
        if (before != null) {
            me = me + s + "before=" + before.toString();
        }
        return me;
    }
}
