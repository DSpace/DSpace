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
public final class ChecksumHistoryIterator
{
    private static final Logger LOG = Logger.getLogger(ChecksumHistoryIterator.class);

    private static final String SELECT_HISTORY = "select CSH.bitstream_id, CSH.process_start_date, "
            + " CSH.process_end_date, CSH.checksum_expected, CSH.checksum_calculated, CSH.result  "
            + " from checksum_history CSH ";

    private static final String COUNT_HISTORY = "select COUNT(*) "
            + " from checksum_history CSH ";

    private static final String BITSTREAM_WHERE = " where CSH.bitstream_id = ? ";
    private static final String ORDER_BY = " order by CSH.process_end_date desc ";

    private static final int UNDEFINED = -1;
    private Context context;
    private int bitstream_id;
    private Date after, before;
    private QueryBuilder builder;
    private ResultSet rs;

    /**
     * create iterator to loop over checksum_history entries for given bitstream
     *
     * @param ctxt
     * @param bitstream_id
     */
    public ChecksumHistoryIterator(Context ctxt, int bitstream_id)
    {
        context = ctxt;
        this.bitstream_id = bitstream_id;
        rs = null;
    }

    public ChecksumHistoryIterator(Context ctxt,
                                   String has_result, String[] has_not_results,
                                   Date before_date, Date after_date,
                                   DSpaceObject inside)
    {
        context = ctxt;
        bitstream_id = UNDEFINED;
        builder = new QueryBuilder(ctxt, has_result, has_not_results, inside);
        after = after_date;
        before = before_date;
        rs = null;
    }

    /**
     * return the number of checksum_history entries in this iterator
     */
    public int count() throws SQLException
    {
        ResultSet rs = buildResultSet(COUNT_HISTORY);
        rs.next();
        return rs.getInt(1);
    }

    /**
     * return null or next checksum_history entry
     */
    public ChecksumHistory next() throws SQLException
    {
        if (rs == null)
        {
            rs = buildResultSet(SELECT_HISTORY);
        }
        ChecksumHistory hist = null;
        if (rs.next())
        {
            int bitId = rs.getInt(1);
            Date startDate = rs.getDate(2);
            Date endDate = rs.getDate(3);
            String expected = rs.getString(4);
            String computed = rs.getString(5);
            String resultCode = rs.getString(6);
            hist = new ChecksumHistory(bitId, startDate, endDate, expected, computed, "", resultCode);
        }
        return hist;
    }

    private ResultSet buildResultSet(String stmt) throws SQLException
    {
        PreparedStatement sqlStmt = null;
        if (bitstream_id != UNDEFINED)
        {
            stmt = stmt + BITSTREAM_WHERE + ORDER_BY;
            LOG.debug(stmt);
            sqlStmt = context.getDBConnection().prepareStatement(stmt);
            sqlStmt.setInt(1, bitstream_id);
        } else
        {
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
            stmt = stmt + builder.where_clause(operator, "CSH");
            stmt = stmt + ORDER_BY;

            sqlStmt = builder.prepareStatement(stmt);
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
            sqlStmt = builder.prepare(pos, sqlStmt);
        }
        return sqlStmt.executeQuery();
    }


    public String toString()
    {
        String me = getClass().getCanonicalName() + "(";
        if (
                bitstream_id != UNDEFINED)
        {
            me = me + "bitstrame_id=" + bitstream_id;
        } else
        {
            String sep = ", ";
            me = builder.propertyString(sep);

            String s = (me.isEmpty()) ? "" : sep;
            if (after != null)
            {
                me = me + s + "after=" + after.toString();
                s = sep;
            }
            if (before != null)
            {
                me = me + s + "before=" + before.toString();
            }
            return me;
        }
        return me + ")";
    }

}
