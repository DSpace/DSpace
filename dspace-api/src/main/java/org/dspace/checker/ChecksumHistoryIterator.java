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
import java.util.Date;
import org.apache.log4j.Logger;
import org.dspace.core.Context;

/**
 * <p>
 * Use to iterate over bitstreams selected based on information stored in MOST_RECENT_CHECKSUM
 * </p>
 *
 * @author Monika Mevenkamp
 */
public final class ChecksumHistoryIterator extends DAOSupport
{
    private static final Logger LOG = Logger.getLogger(ChecksumHistoryIterator.class);

    /** Query that inserts results of recent check into the history table. */
    private static final String SELECT_HISTORY = "select bitstream_id, process_start_date, "
            + " process_end_date, checksum_expected, checksum_calculated, result  "
            + " from checksum_history where bitstream_id = ? "
            + " order by process_end_date desc";

    private Context context;
    private int bitstream_id;
    private ResultSet rs;

    public ChecksumHistoryIterator(Context ctxt, int bitstream_id)
    {
        super(ctxt.getDBConnection());
        context = ctxt;
        this.bitstream_id = bitstream_id;
        rs = null;
    }

    public ChecksumHistory next() throws SQLException
    {
        if (rs == null) {
            String stmt = SELECT_HISTORY;
            LOG.debug(stmt);
            PreparedStatement sqlStmt = prepareStatement(stmt);
            sqlStmt.setInt(1, bitstream_id);
            rs = sqlStmt.executeQuery();
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
            hist =  new ChecksumHistory(bitId, startDate, endDate, expected, computed, "", resultCode);
        }
        return hist;
    }

    public String toString()
    {
        return getClass().getCanonicalName() + "(bitstrame_id=" + bitstream_id + ")";
    }

}
