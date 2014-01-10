/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;

/**
 * Class representing a particular bitstream format.
 * <P>
 * Changes to the bitstream format metadata are only written to the database
 * when <code>update</code> is called.
 * 
 * @author Robert Tansley
 * @version $Revision$
 */
public class BitstreamFormat
{
    /** log4j logger */
    private static Logger log = Logger.getLogger(BitstreamFormat.class);

    /**
     * The "unknown" support level - for bitstream formats that are unknown to
     * the system
     */
    public static final int UNKNOWN = 0;

    /**
     * The "known" support level - for bitstream formats that are known to the
     * system, but not fully supported
     */
    public static final int KNOWN = 1;

    /**
     * The "supported" support level - for bitstream formats known to the system
     * and fully supported.
     */
    public static final int SUPPORTED = 2;


    /** translate support-level ID to string.  MUST keep this table in sync
     *  with support level definitions above.
     */
    private static final String supportLevelText[] =
        { "UNKNOWN", "KNOWN", "SUPPORTED" };

    /** Our context */
    private Context bfContext;

    /** The row in the table representing this format */
    private TableRow bfRow;

    /** File extensions for this format */
    private List<String> extensions;

    /**
     * Class constructor for creating a BitstreamFormat object based on the
     * contents of a DB table row.
     * 
     * @param context
     *            the context this object exists in
     * @param row
     *            the corresponding row in the table
     * @throws SQLException
     */
    BitstreamFormat(Context context, TableRow row) throws SQLException
    {
        bfContext = context;
        bfRow = row;
        extensions = new ArrayList<String>();

        TableRowIterator tri = DatabaseManager.query(context,
                "SELECT * FROM fileextension WHERE bitstream_format_id= ? ",
                 getID());

        try
        {
            while (tri.hasNext())
            {
                extensions.add(tri.next().getStringColumn("extension"));
            }
        }
        finally
        {
            // close the TableRowIterator to free up resources
            if (tri != null)
            {
                tri.close();
            }
        }

        // Cache ourselves
        context.cache(this, row.getIntColumn("bitstream_format_id"));
    }

    /**
     * Get a bitstream format from the database.
     * 
     * @param context
     *            DSpace context object
     * @param id
     *            ID of the bitstream format
     * 
     * @return the bitstream format, or null if the ID is invalid.
     * @throws SQLException
     */
    public static BitstreamFormat find(Context context, int id)
            throws SQLException
    {
        // First check the cache
        BitstreamFormat fromCache = (BitstreamFormat) context.fromCache(
                BitstreamFormat.class, id);

        if (fromCache != null)
        {
            return fromCache;
        }

        TableRow row = DatabaseManager.find(context, "bitstreamformatregistry",
                id);

        if (row == null)
        {
            if (log.isDebugEnabled())
            {
                log.debug(LogManager.getHeader(context,
                        "find_bitstream_format",
                        "not_found,bitstream_format_id=" + id));
            }

            return null;
        }

        // not null, return format object
        if (log.isDebugEnabled())
        {
            log.debug(LogManager.getHeader(context, "find_bitstream_format",
                    "bitstream_format_id=" + id));
        }

        return new BitstreamFormat(context, row);
    }

    /**
     * Find a bitstream format by its (unique) MIME type.
     * If more than one bitstream format has the same MIME type, the
     * one returned is unpredictable.
     *
     * @param context
     *            DSpace context object
     * @param mimeType
     *            MIME type value
     *
     * @return the corresponding bitstream format, or <code>null</code> if
     *         there's no bitstream format with the given MIMEtype.
     * @throws SQLException
     */
    public static BitstreamFormat findByMIMEType(Context context,
            String mimeType) throws SQLException
    {
        // NOTE: Avoid internal formats since e.g. "License" also has
        // a MIMEtype of text/plain.
        TableRow formatRow = DatabaseManager.querySingle(context,
            "SELECT * FROM bitstreamformatregistry "+
            "WHERE mimetype LIKE ? AND internal = '0' ",
            mimeType);

        if (formatRow == null)
        {
            return null;
        }
        return findByFinish(context, formatRow);
    }

    /**
     * Find a bitstream format by its (unique) short description
     * 
     * @param context
     *            DSpace context object
     * @param desc
     *            the short description
     * 
     * @return the corresponding bitstream format, or <code>null</code> if
     *         there's no bitstream format with the given short description
     * @throws SQLException
     */
    public static BitstreamFormat findByShortDescription(Context context,
            String desc) throws SQLException
    {
        TableRow formatRow = DatabaseManager.findByUnique(context,
                "bitstreamformatregistry", "short_description", desc);

        if (formatRow == null)
        {
            return null;
        }

        return findByFinish(context, formatRow);
    }

    // shared final logic in findBy... methods;
    // use context's cache for object mapped from table row.
    private static BitstreamFormat findByFinish(Context context,
                                                TableRow formatRow)
        throws SQLException
    {
        // not null
        if (log.isDebugEnabled())
        {
            log.debug(LogManager.getHeader(context, "find_bitstream",
                    "bitstream_format_id="
                            + formatRow.getIntColumn("bitstream_format_id")));
        }

        // From cache?
        BitstreamFormat fromCache = (BitstreamFormat) context.fromCache(
                BitstreamFormat.class, formatRow
                        .getIntColumn("bitstream_format_id"));

        if (fromCache != null)
        {
            return fromCache;
        }

        return new BitstreamFormat(context, formatRow);
    }

    /**
     * Get the generic "unknown" bitstream format.
     * 
     * @param context
     *            DSpace context object
     * 
     * @return the "unknown" bitstream format.
     * @throws SQLException
     * 
     * @throws IllegalStateException
     *             if the "unknown" bitstream format couldn't be found
     */
    public static BitstreamFormat findUnknown(Context context)
            throws SQLException
    {
        BitstreamFormat bf = findByShortDescription(context, "Unknown");

        if (bf == null)
        {
            throw new IllegalStateException(
                    "No `Unknown' bitstream format in registry");
        }

        return bf;
    }

    /**
     * Retrieve all bitstream formats from the registry, ordered by ID
     * 
     * @param context
     *            DSpace context object
     * 
     * @return the bitstream formats.
     * @throws SQLException
     */
    public static BitstreamFormat[] findAll(Context context)
            throws SQLException
    {
        List<BitstreamFormat> formats = new ArrayList<BitstreamFormat>();

        TableRowIterator tri = DatabaseManager.queryTable(context, "bitstreamformatregistry",
                        "SELECT * FROM bitstreamformatregistry ORDER BY bitstream_format_id");

        try
        {
            while (tri.hasNext())
            {
                TableRow row = tri.next();

                // From cache?
                BitstreamFormat fromCache = (BitstreamFormat) context.fromCache(
                        BitstreamFormat.class, row
                                .getIntColumn("bitstream_format_id"));

                if (fromCache != null)
                {
                    formats.add(fromCache);
                }
                else
                {
                    formats.add(new BitstreamFormat(context, row));
                }
            }
        }
        finally
        {
            // close the TableRowIterator to free up resources
            if (tri != null)
            {
                tri.close();
            }
        }

        // Return the formats as an array
        BitstreamFormat[] formatArray = new BitstreamFormat[formats.size()];
        formatArray = (BitstreamFormat[]) formats.toArray(formatArray);

        return formatArray;
    }

    /**
     * Retrieve all non-internal bitstream formats from the registry. The
     * "unknown" format is not included, and the formats are ordered by support
     * level (highest first) first then short description.
     * 
     * @param context
     *            DSpace context object
     * 
     * @return the bitstream formats.
     * @throws SQLException
     */
    public static BitstreamFormat[] findNonInternal(Context context)
            throws SQLException
    {
        List<BitstreamFormat> formats = new ArrayList<BitstreamFormat>();

        String myQuery = "SELECT * FROM bitstreamformatregistry WHERE internal='0' "
                + "AND short_description NOT LIKE 'Unknown' "
                + "ORDER BY support_level DESC, short_description";

        TableRowIterator tri = DatabaseManager.queryTable(context,
                "bitstreamformatregistry", myQuery);

        try
        {
            while (tri.hasNext())
            {
                TableRow row = tri.next();

                // From cache?
                BitstreamFormat fromCache = (BitstreamFormat) context.fromCache(
                        BitstreamFormat.class, row
                                .getIntColumn("bitstream_format_id"));

                if (fromCache != null)
                {
                    formats.add(fromCache);
                }
                else
                {
                    formats.add(new BitstreamFormat(context, row));
                }
            }
        }
        finally
        {
            // close the TableRowIterator to free up resources
            if (tri != null)
            {
                tri.close();
            }
        }

        // Return the formats as an array
        BitstreamFormat[] formatArray = new BitstreamFormat[formats.size()];
        formatArray = (BitstreamFormat[]) formats.toArray(formatArray);

        return formatArray;
    }

    /**
     * Create a new bitstream format
     * 
     * @param context
     *            DSpace context object
     * @return the newly created BitstreamFormat
     * @throws SQLException
     * @throws AuthorizeException
     */
    public static BitstreamFormat create(Context context) throws SQLException,
            AuthorizeException
    {
        // Check authorisation - only administrators can create new formats
        if (!AuthorizeManager.isAdmin(context))
        {
            throw new AuthorizeException(
                    "Only administrators can create bitstream formats");
        }

        // Create a table row
        TableRow row = DatabaseManager.create(context,
                "bitstreamformatregistry");

        log.info(LogManager.getHeader(context, "create_bitstream_format",
                "bitstream_format_id="
                        + row.getIntColumn("bitstream_format_id")));

        return new BitstreamFormat(context, row);
    }

    /**
     * Get the internal identifier of this bitstream format
     * 
     * @return the internal identifier
     */
    public final int getID()
    {
        return bfRow.getIntColumn("bitstream_format_id");
    }

    /**
     * Get a short (one or two word) description of this bitstream format
     * 
     * @return the short description
     */
    public final String getShortDescription()
    {
        return bfRow.getStringColumn("short_description");
    }

    /**
     * Set the short description of the bitstream format
     * 
     * @param s
     *            the new short description
     */
    public final void setShortDescription(String s)
       throws SQLException
    {
        // You can not reset the unknown's registry's name
        BitstreamFormat unknown = null;
		try {
			unknown = findUnknown(bfContext);
		} catch (IllegalStateException e) {
			// No short_description='Unknown' found in bitstreamformatregistry
			// table. On first load of registries this is expected because it
			// hasn't been inserted yet! So, catch but ignore this runtime 
			// exception thrown by method findUnknown.
		}
		
		// If the exception was thrown, unknown will == null so goahead and 
		// load s. If not, check that the unknown's registry's name is not
		// being reset.
		if (unknown == null || unknown.getID() != getID()) {
            bfRow.setColumn("short_description", s);
		}
    }

    /**
     * Get a description of this bitstream format, including full application or
     * format name
     * 
     * @return the description
     */
    public final String getDescription()
    {
        return bfRow.getStringColumn("description");
    }

    /**
     * Set the description of the bitstream format
     * 
     * @param s
     *            the new description
     */
    public final void setDescription(String s)
    {
        bfRow.setColumn("description", s);
    }

    /**
     * Get the MIME type of this bitstream format, for example
     * <code>text/plain</code>
     * 
     * @return the MIME type
     */
    public final String getMIMEType()
    {
        return bfRow.getStringColumn("mimetype");
    }

    /**
     * Set the MIME type of the bitstream format
     * 
     * @param s
     *            the new MIME type
     */
    public final void setMIMEType(String s)
    {
        bfRow.setColumn("mimetype", s);
    }

    /**
     * Get the support level for this bitstream format - one of
     * <code>UNKNOWN</code>,<code>KNOWN</code> or <code>SUPPORTED</code>.
     * 
     * @return the support level
     */
    public final int getSupportLevel()
    {
        return bfRow.getIntColumn("support_level");
    }

    /**
     * Get the support level text for this bitstream format - one of
     * <code>UNKNOWN</code>,<code>KNOWN</code> or <code>SUPPORTED</code>.
     *
     * @return the support level
     */
    public String getSupportLevelText() {
        return supportLevelText[getSupportLevel()];
    }

    /**
     * Set the support level for this bitstream format - one of
     * <code>UNKNOWN</code>,<code>KNOWN</code> or <code>SUPPORTED</code>.
     * 
     * @param sl
     *            the new support level
     */
    public final void setSupportLevel(int sl)
    {
        // Sanity check
        if ((sl < 0) || (sl > 2))
        {
            throw new IllegalArgumentException("Invalid support level");
        }

        bfRow.setColumn("support_level", sl);
    }

    /**
     * Find out if the bitstream format is an internal format - that is, one
     * that is used to store system information, rather than the content of
     * items in the system
     * 
     * @return <code>true</code> if the bitstream format is an internal type
     */
    public final boolean isInternal()
    {
        return bfRow.getBooleanColumn("internal");
    }

    /**
     * Set whether the bitstream format is an internal format
     * 
     * @param b
     *            pass in <code>true</code> if the bitstream format is an
     *            internal type
     */
    public final void setInternal(boolean b)
    {
        bfRow.setColumn("internal", b);
    }

    /**
     * Update the bitstream format metadata
     * 
     * @throws SQLException
     * @throws AuthorizeException
     */
    public void update() throws SQLException, AuthorizeException
    {
        // Check authorisation - only administrators can change formats
        if (!AuthorizeManager.isAdmin(bfContext))
        {
            throw new AuthorizeException(
                    "Only administrators can modify bitstream formats");
        }

        log.info(LogManager.getHeader(bfContext, "update_bitstream_format",
                "bitstream_format_id=" + getID()));

        // Delete extensions
        DatabaseManager.updateQuery(bfContext,
                "DELETE FROM fileextension WHERE bitstream_format_id= ? ",
                getID());

        // Rewrite extensions
        for (int i = 0; i < extensions.size(); i++)
        {
            String s = extensions.get(i);
            TableRow r = DatabaseManager.row("fileextension");
            r.setColumn("bitstream_format_id", getID());
            r.setColumn("extension", s);
            DatabaseManager.insert(bfContext, r);
        }

        DatabaseManager.update(bfContext, bfRow);
    }

    /**
     * Delete this bitstream format. This converts the types of any bitstreams
     * that may have this type to "unknown". Use this with care!
     * 
     * @throws SQLException
     * @throws AuthorizeException
     */
    public void delete() throws SQLException, AuthorizeException
    {
        // Check authorisation - only administrators can delete formats
        if (!AuthorizeManager.isAdmin(bfContext))
        {
            throw new AuthorizeException(
                    "Only administrators can delete bitstream formats");
        }

        // Find "unknown" type
        BitstreamFormat unknown = findUnknown(bfContext);

        if (unknown.getID() == getID())
        {
            throw new IllegalArgumentException("The Unknown bitstream format may not be deleted.");
        }

        // Remove from cache
        bfContext.removeCached(this, getID());

        // Set bitstreams with this format to "unknown"
        int numberChanged = DatabaseManager.updateQuery(bfContext,
                "UPDATE bitstream SET bitstream_format_id= ? " + 
                " WHERE bitstream_format_id= ? ", 
                unknown.getID(),getID());

        // Delete extensions
        DatabaseManager.updateQuery(bfContext,
                "DELETE FROM fileextension WHERE bitstream_format_id= ? ",
                getID());

        // Delete this format from database
        DatabaseManager.delete(bfContext, bfRow);

        log.info(LogManager.getHeader(bfContext, "delete_bitstream_format",
                "bitstream_format_id=" + getID() + ",bitstreams_changed="
                        + numberChanged));
    }

    /**
     * Get the filename extensions associated with this format
     * 
     * @return the extensions
     */
    public String[] getExtensions()
    {
        String[] exts = new String[extensions.size()];
        exts = (String[]) extensions.toArray(exts);

        return exts;
    }

    /**
     * Set the filename extensions associated with this format
     * 
     * @param exts
     *            String [] array of extensions
     */
    public void setExtensions(String[] exts)
    {
        extensions = new ArrayList<String>();

        for (int i = 0; i < exts.length; i++)
        {
            extensions.add(exts[i]);
        }
    }

    /**
     * If you know the support level string, look up the corresponding type ID
     * constant.
     *
     * @param slevel
     *            String with the name of the action (must be exact match)
     *
     * @return the corresponding action ID, or <code>-1</code> if the action
     *         string is unknown
     */
    public static int getSupportLevelID(String slevel)
    {
        for (int i = 0; i < supportLevelText.length; i++)
        {
            if (supportLevelText[i].equals(slevel))
            {
                return i;
            }
        }

        return -1;
    }
}
