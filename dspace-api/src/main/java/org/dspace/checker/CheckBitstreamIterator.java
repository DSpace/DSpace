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
import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.content.Bitstream;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Constants;
import org.dspace.core.Context;

/**
 * <p>
 * Use to iterate over bitstreams selected based on information stored in MOST_RECENT_CHECKSUM
 * </p>
 *
 * @author Monika Mevenkamp
 */
public final class CheckBitstreamIterator extends DAOSupport
{
    private static final Logger LOG = Logger.getLogger(CheckBitstreamIterator.class);

    static final String THIS_BITSTREAM = "MRC.BITSTREAM_ID = ?";

    static final String BITSTREAMS_IN_ITEM = "MRC.BITSTREAM_ID in \n" +
            "(SELECT BITSTREAM_ID FROM BUNDLE2BITSTREAM WHERE BUNDLE_ID in\n" +
            "   (SELECT BUNDLE_ID FROM ITEM2BUNDLE WHERE ITEM_ID = ?))";

    static final String BITSTREAMS_IN_COLLECTION = "MRC.BITSTREAM_ID in \n" +
            "(SELECT BITSTREAM_ID FROM BUNDLE2BITSTREAM WHERE BUNDLE_ID in\n" +
            "  (SELECT BUNDLE_ID FROM ITEM2BUNDLE WHERE ITEM_ID  in\n" +
            "     (SELECT ITEM_ID FROM COLLECTION2ITEM WHERE COLLECTION_ID = ?)))";

    static final String BITSTREAMS_IN_COMMUNITY = "MRC.BITSTREAM_ID in \n" +
            "(SELECT BITSTREAM_ID FROM BUNDLE2BITSTREAM WHERE BUNDLE_ID in\n" +
            "   (SELECT BUNDLE_ID FROM ITEM2BUNDLE WHERE ITEM_ID  in\n" +
            "      (SELECT ITEM_ID  FROM COLLECTION2ITEM WHERE COLLECTION_ID in \n" +
            "         (SELECT COLLECTION_ID FROM COMMUNITY2COLLECTION WHERE COMMUNITY_ID =  ?))))";


    private Context context;
    private String with_result;
    private String[] without_result;
    private Date before, after;
    private DSpaceObject root;
    private ResultSet rs;
    private Bitstream rs_bitstream;

    public CheckBitstreamIterator(Context ctxt,
                                   String has_result, String[] has_not_results,
                                   Date before_date, Date after_date,
                                   DSpaceObject inside)
    {
        super(ctxt.getDBConnection());
        context = ctxt;
        after = after_date;
        before = before_date;
        root = inside;
        with_result = has_result;
        without_result = has_not_results;
        rs = null;
        rs_bitstream = null;
    }

    // TODO return BitstreamInfo instance or null
    public boolean next() throws SQLException
    {
        if (rs == null) {
            rs = select();
        }
        return rs.next();
    }

    public int bitstream_id() throws SQLException
    {
        return rs.getInt(1);
    }

    public Bitstream bitstream()
    {
        try
        {
            if (rs_bitstream == null) {
                rs_bitstream = Bitstream.find(context, bitstream_id());
            }
            return rs_bitstream;
        } catch (SQLException e)
        {
            return null;
        }
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

    private ResultSet select() throws SQLException
    {
        String stmt = "SELECT MRC.bitstream_id, MRC.result, MRC.last_process_end_date, MRC.current_checksum, \n" +
                "BITSTREAM.DELETED, BITSTREAM.internal_id \n" +
                "FROM MOST_RECENT_CHECKSUM MRC JOIN BITSTREAM ON BITSTREAM.BITSTREAM_ID = MRC.BITSTREAM_ID ";
        stmt = stmt + where_clause();
        stmt = stmt + " ORDER BY MRC.LAST_PROCESS_END_DATE";
        return prepare(stmt).executeQuery();
    }

    public int count() throws SQLException
    {
        String stmt = "SELECT COUNT(*) AS N FROM MOST_RECENT_CHECKSUM MRC ";
        stmt = stmt + where_clause();
        LOG.debug(stmt);
        ResultSet rs = prepare(stmt).executeQuery();
        rs.next();
        return rs.getInt(1);
    }

    private String where_clause() {
        String stmt = "";
        String operator = " WHERE ";
        if (after != null)
        {
            stmt = stmt + operator + " MRC.LAST_PROCESS_END_DATE >= ?";
            LOG.debug(operator + "last_check after " + after);
            operator = " AND ";
        }
        if (before != null)
        {
            stmt = stmt + operator + " MRC.LAST_PROCESS_END_DATE < ? ";
            LOG.debug("last_check before " + before);
            operator = " AND ";
        }
        if (with_result != null)
        {
            stmt = stmt + operator + " MRC.RESULT = ?";
            LOG.debug("last_check result = " + with_result);
            operator = " AND ";
        }
        if (without_result != null)
        {
            for (int i = 0; i < without_result.length; i++)
            {
                stmt = stmt + operator + " MRC.RESULT != ?";
                LOG.debug("last_check result != " + without_result[i]);
                operator = " AND ";
            }
        }
        if (root != null)
        {
            String bitstream_ids = "";
            LOG.debug("bitstream in  = " + root);
            switch (root.getType())
            {
                case Constants.ITEM:
                    bitstream_ids = BITSTREAMS_IN_ITEM;
                    break;
                case Constants.COLLECTION:
                    bitstream_ids = BITSTREAMS_IN_COLLECTION;
                    break;
                case Constants.COMMUNITY:
                    bitstream_ids = BITSTREAMS_IN_COMMUNITY;
                    break;
                case Constants.BITSTREAM:
                    bitstream_ids = THIS_BITSTREAM;
                    break;
                default:
                    throw new RuntimeException(root.toString() + " does not contains Bitstreams");
            }
            stmt = stmt + operator + bitstream_ids;
            operator = " AND ";
        }
        return stmt;
    }

    private PreparedStatement prepare(String stmt) throws SQLException
    {
        PreparedStatement sqlStmt = prepareStatement(stmt);
        int pos = 1;
        if (before != null)
        {
            sqlStmt.setTimestamp(pos, new Timestamp(before.getTime()));
            pos++;
        }
        if (after != null)
        {
            sqlStmt.setTimestamp(pos, new Timestamp(after.getTime()));
            pos++;
        }
        if (with_result != null)
        {
            sqlStmt.setString(pos, with_result);
            pos++;
        }
        if (without_result != null)
        {
            for (int i = 0; i < without_result.length; i++)
            {
                sqlStmt.setString(pos, without_result[i]);
                pos++;
            }
        }
        if (root != null)
        {
            sqlStmt.setInt(pos, root.getID());
            pos++;
        }
        return  sqlStmt;
    }

    public String toString()
    {
        return getClass().getCanonicalName() + "(" + propertyString(", ") + ")";
    }

    public String propertyString(String sep)
    {
        DateFormat df = DateFormat.getDateInstance(DateFormat.SHORT, Locale.US);
        String me = "";
        if (root != null) {
            me = me + sep + "root=" + root;
        }
        if (with_result != null) {
            me = me + sep + "with_result=" + with_result;
        }
        if (without_result != null) {
            me = me + sep + "without_result=[" + StringUtils.join(without_result, ',') + "]";
        }
        if (after != null) {
            me = me + sep + "after=" + df.format(after);
        }
        if (before != null) {
            me = me + sep + "before=" + df.format(before);
        }
        if (! me.isEmpty())
            me = me.substring(sep.length());
        return me;
    }
}
