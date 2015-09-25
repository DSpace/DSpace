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
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
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
public class QueryBuilder
{
    private static final Logger LOG = Logger.getLogger(QueryBuilder.class);

    static final String THIS_BITSTREAM = "BITSTREAM_ID = ?";

    static final String BITSTREAMS_IN_ITEM = "BITSTREAM_ID in \n" +
            "(SELECT BITSTREAM_ID FROM BUNDLE2BITSTREAM WHERE BUNDLE_ID in\n" +
            "   (SELECT BUNDLE_ID FROM ITEM2BUNDLE WHERE ITEM_ID = ?))";

    static final String BITSTREAMS_IN_COLLECTION = "BITSTREAM_ID in \n" +
            "(SELECT BITSTREAM_ID FROM BUNDLE2BITSTREAM WHERE BUNDLE_ID in\n" +
            "  (SELECT BUNDLE_ID FROM ITEM2BUNDLE WHERE ITEM_ID  in\n" +
            "     (SELECT ITEM_ID FROM COLLECTION2ITEM WHERE COLLECTION_ID = ?)))";

    static final String BITSTREAMS_IN_COMMUNITY = "BITSTREAM_ID in \n" +
            "(SELECT BITSTREAM_ID FROM BUNDLE2BITSTREAM WHERE BUNDLE_ID in\n" +
            "   (SELECT BUNDLE_ID FROM ITEM2BUNDLE WHERE ITEM_ID  in\n" +
            "      (SELECT ITEM_ID  FROM COLLECTION2ITEM WHERE COLLECTION_ID in \n" +
            "         (SELECT COLLECTION_ID FROM COMMUNITY2COLLECTION WHERE COMMUNITY_ID =  ?))))";


    private Context context;
    private String with_result;
    private String[] without_result;
    private DSpaceObject root;
    private ResultSet rs;

    protected QueryBuilder(Context ctxt,
                        String has_result, String[] has_not_results,
                        DSpaceObject inside)
    {
        context = ctxt;
        root = inside;
        with_result = has_result;
        without_result = has_not_results;
        rs = null;
    }

    protected String where_clause(String operator, String tableName) {
        String stmt = " ";
        if (with_result != null)
        {
            stmt = stmt + operator + " " + tableName + ".RESULT = ?";
            operator = " AND ";
        }
        if (without_result != null)
        {
            for (int i = 0; i < without_result.length; i++)
            {
                stmt = stmt + operator + " " + tableName + ".RESULT != ?";
                operator = " AND ";
            }
        }
        if (root != null)
        {
            String bitstream_ids = "";
            switch (root.getType())
            {
                case Constants.ITEM:
                    bitstream_ids = tableName + "." + BITSTREAMS_IN_ITEM;
                    break;
                case Constants.COLLECTION:
                    bitstream_ids = tableName + "." + BITSTREAMS_IN_COLLECTION;
                    break;
                case Constants.COMMUNITY:
                    bitstream_ids = tableName + "." + BITSTREAMS_IN_COMMUNITY;
                    break;
                case Constants.BITSTREAM:
                    bitstream_ids = tableName + "." + THIS_BITSTREAM;
                    break;
                default:
                    throw new RuntimeException(root.toString() + " does not contains Bitstreams");
            }
            stmt = stmt + operator + bitstream_ids;
        }
        return stmt;
    }

    protected PreparedStatement prepare(int pos, PreparedStatement sqlStmt) throws SQLException
    {
        if (with_result != null)
        {
            sqlStmt.setString(pos, with_result);
            if (LOG.isDebugEnabled())
                LOG.debug("pos " + pos + " " + with_result);
            pos++;
        }
        if (without_result != null)
        {
            for (int i = 0; i < without_result.length; i++)
            {
                sqlStmt.setString(pos, without_result[i]);
                if (LOG.isDebugEnabled())
                    LOG.debug("pos " + pos +  without_result[i]);
                pos++;
            }
        }
        if (root != null)
        {
            sqlStmt.setInt(pos, root.getID());
            if (LOG.isDebugEnabled())
                LOG.debug("pos " + pos + " " + root);
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
        if (! me.isEmpty())
            me = me.substring(sep.length());
        return me;
    }

    protected PreparedStatement prepareStatement(String stmt) throws SQLException
    {
        if (LOG.isDebugEnabled())
        {
            String prt = stmt.replaceAll("\n", " ").toLowerCase().replaceAll("\\([^*]", "(\n\t").replaceAll("\\)", ")\n\t");
            LOG.debug(prt);
        }
        return context.getDBConnection().prepareStatement(stmt);
    }

}
