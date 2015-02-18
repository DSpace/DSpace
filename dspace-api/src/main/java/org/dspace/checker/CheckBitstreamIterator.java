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

    private PreparedStatement prepStmt;
    private ResultSet rs;

    public CheckBitstreamIterator(Context ctxt,
                                  String has_result, String[] has_not_results,
                                  Date before_date, Date after_date,
                                  DSpaceObject inside)
    {
        super(ctxt, has_result, has_not_results, before_date, after_date, inside);
        prepStmt = null;
        rs = null;
    }

    // TODO return BitstreamInfo instance or null
    public boolean next() throws SQLException
    {
        if (prepStmt == null)
        {
            prepStmt = select();
            rs = prepStmt.executeQuery();
        }
        return rs.next();
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

    private PreparedStatement select() throws SQLException
    {
        String stmt = "SELECT MRC.bitstream_id, MRC.result, MRC.last_process_end_date, MRC.current_checksum, \n" +
                "BITSTREAM.DELETED, BITSTREAM.internal_id, BITSTREAM.checksum_algorithm, BITSTREAM.checksum \n" +
                "FROM MOST_RECENT_CHECKSUM MRC JOIN BITSTREAM ON BITSTREAM.BITSTREAM_ID = MRC.BITSTREAM_ID ";
        stmt = stmt + where_clause("MRC");
        stmt = stmt + " ORDER BY MRC.LAST_PROCESS_END_DATE";
        return prepare(stmt);
    }

    public int count() throws SQLException
    {
        String stmt = "SELECT COUNT(*) AS N FROM MOST_RECENT_CHECKSUM MRC ";
        stmt = stmt + where_clause("MRC");
        LOG.debug(stmt);
        ResultSet rs = prepare(stmt).executeQuery();
        rs.next();
        return rs.getInt(1);
    }


}
