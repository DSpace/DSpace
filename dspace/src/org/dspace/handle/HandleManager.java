/*
 * HandleManager.java
 *
 * Version: $Revision$
 *
 * Date: $Date$
 *
 * Copyright (c) 2002-2005, Hewlett-Packard Company and Massachusetts
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
package org.dspace.handle;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;

/**
 * Interface to the <a href="http://www.handle.net" target=_new>CNRI Handle
 * System </a>.
 * 
 * <p>
 * Currently, this class simply maps handles to local facilities; handles which
 * are owned by other sites (including other DSpaces) are treated as
 * non-existent.
 * </p>
 * 
 * @author Peter Breton
 * @version $Revision$
 */
public class HandleManager
{
    /** log4j category */
    private static Logger log = Logger.getLogger(HandleManager.class);

    /** Private Constructor */
    private HandleManager()
    {
    }

    /**
     * Return the local URL for handle, or null if handle cannot be found.
     * 
     * The returned URL is a (non-handle-based) location where a dissemination
     * of the object referred to by handle can be obtained.
     * 
     * @param context
     *            DSpace context
     * @param handle
     *            The handle
     * @return The local URL
     * @exception SQLException
     *                If a database error occurs
     */
    public static String resolveToURL(Context context, String handle)
            throws SQLException
    {
        TableRow dbhandle = findHandleInternal(context, handle);

        if (dbhandle == null)
        {
            return null;
        }

        String url = ConfigurationManager.getProperty("dspace.url")
                + "/handle/" + handle;

        if (log.isDebugEnabled())
        {
            log.debug("Resolved " + handle + " to " + url);
        }

        return url;
    }

    /**
     * Transforms handle into the canonical form <em>hdl:handle</em>.
     * 
     * No attempt is made to verify that handle is in fact valid.
     * 
     * @param handle
     *            The handle
     * @return The canonical form
     */
    public static String getCanonicalForm(String handle)
    {
        //        return "hdl:" + handle;
        return "http://hdl.handle.net/" + handle;
    }

    /**
     * Returns displayable string of the handle's 'temporary' URL
     * <em>http://hdl.handle.net/handle/em>.
     *
     * No attempt is made to verify that handle is in fact valid.
     *
     * @param handle The handle
     * @return The canonical form
     */

    //    public static String getURLForm(String handle)
    //    {
    //        return "http://hdl.handle.net/" + handle;
    //    }
    /**
     * Creates a new handle in the database.
     * 
     * @param context
     *            DSpace context
     * @param dso
     *            The DSpaceObject to create a handle for
     * @return The newly created handle
     * @exception SQLException
     *                If a database error occurs
     */
    public static String createHandle(Context context, DSpaceObject dso)
            throws SQLException
    {
        TableRow handle = DatabaseManager.create(context, "Handle");
        String handleId = createId(handle.getIntColumn("handle_id"));

        handle.setColumn("handle", handleId);
        handle.setColumn("resource_type_id", dso.getType());
        handle.setColumn("resource_id", dso.getID());
        DatabaseManager.update(context, handle);

        if (log.isDebugEnabled())
        {
            log.debug("Created new handle for "
                    + Constants.typeText[dso.getType()] + " " + handleId);
        }

        return handleId;
    }

    /**
     * Creates a handle entry, but with a handle supplied by the caller (new
     * Handle not generated)
     * 
     * @param context
     *            DSpace context
     * @param dso
     *            DSpaceObject
     * @param suppliedHandle
     *            existing handle value
     * @return the Handle
     */
    public static String createHandle(Context context, DSpaceObject dso,
            String suppliedHandle) throws SQLException
    {
        TableRow handle = DatabaseManager.create(context, "Handle");
        String handleId = suppliedHandle;

        handle.setColumn("handle", handleId);
        handle.setColumn("resource_type_id", dso.getType());
        handle.setColumn("resource_id", dso.getID());
        DatabaseManager.update(context, handle);

        if (log.isDebugEnabled())
        {
            log.debug("Created new handle for "
                    + Constants.typeText[dso.getType()] + " " + handleId);
        }

        return handleId;
    }

    /**
     * Return the object which handle maps to, or null. This is the object
     * itself, not a URL which points to it.
     * 
     * @param context
     *            DSpace context
     * @param handle
     *            The handle to resolve
     * @return The object which handle maps to, or null if handle is not mapped
     *         to any object.
     * @exception SQLException
     *                If a database error occurs
     */
    public static DSpaceObject resolveToObject(Context context, String handle)
            throws SQLException
    {
        TableRow dbhandle = findHandleInternal(context, handle);

        if (dbhandle == null)
        {
            return null;
        }

        if ((dbhandle.isColumnNull("resource_type_id"))
                || (dbhandle.isColumnNull("resource_id")))
        {
            throw new IllegalStateException("No associated resource type");
        }

        // What are we looking at here?
        int handletypeid = dbhandle.getIntColumn("resource_type_id");
        int resourceID = dbhandle.getIntColumn("resource_id");

        if (handletypeid == Constants.ITEM)
        {
            Item item = Item.find(context, resourceID);

            if (log.isDebugEnabled())
            {
                log.debug("Resolved handle " + handle + " to item "
                        + ((item == null) ? (-1) : item.getID()));
            }

            return item;
        }
        else if (handletypeid == Constants.COLLECTION)
        {
            Collection collection = Collection.find(context, resourceID);

            if (log.isDebugEnabled())
            {
                log.debug("Resolved handle " + handle + " to collection "
                        + ((collection == null) ? (-1) : collection.getID()));
            }

            return collection;
        }
        else if (handletypeid == Constants.COMMUNITY)
        {
            Community community = Community.find(context, resourceID);

            if (log.isDebugEnabled())
            {
                log.debug("Resolved handle " + handle + " to community "
                        + ((community == null) ? (-1) : community.getID()));
            }

            return community;
        }

        throw new IllegalStateException("Unsupported Handle Type "
                + Constants.typeText[handletypeid]);
    }

    /**
     * Return the handle for an Object, or null if the Object has no handle.
     * 
     * @param context
     *            DSpace context
     * @param dso
     *            The object to obtain a handle for
     * @return The handle for object, or null if the object has no handle.
     * @exception SQLException
     *                If a database error occurs
     */
    public static String findHandle(Context context, DSpaceObject dso)
            throws SQLException
    {
        //        if (!(obj instanceof Item))
        //            return null;
        //        Item item = (Item) obj;
        return getHandleInternal(context, dso.getType(), dso.getID());
    }

    /**
     * Return all the handles which start with prefix.
     * 
     * @param context
     *            DSpace context
     * @param prefix
     *            The handle prefix
     * @return A list of the handles starting with prefix. The list is
     *         guaranteed to be non-null. Each element of the list is a String.
     * @exception SQLException
     *                If a database error occurs
     */
    static List getHandlesForPrefix(Context context, String prefix)
            throws SQLException
    {
        String sql = "SELECT handle FROM handle WHERE handle LIKE ? ";
        TableRowIterator iterator = DatabaseManager.queryTable(context, null, sql, prefix+"%");
        List results = new ArrayList();

        while (iterator.hasNext())
        {
            TableRow row = (TableRow) iterator.next();
            results.add(row.getStringColumn("handle"));
        }
        
        iterator.close();

        return results;
    }

    ////////////////////////////////////////
    // Internal methods
    ////////////////////////////////////////

    /**
     * Return the handle for an Object, or null if the Object has no handle.
     * 
     * @param context
     *            DSpace context
     * @param type
     *            The type of object
     * @param id
     *            The id of object
     * @return The handle for object, or null if the object has no handle.
     * @exception SQLException
     *                If a database error occurs
     */
    private static String getHandleInternal(Context context, int type, int id)
            throws SQLException
    {   	
      	String sql = "SELECT handle FROM Handle WHERE resource_type_id = ? " + 
      				 "AND resource_id = ?";
		
		TableRow row = DatabaseManager.querySingle(context, sql,type,id);

        return (row == null) ? null : row.getStringColumn("handle");
    }

    /**
     * Find the database row corresponding to handle.
     * 
     * @param context
     *            DSpace context
     * @param handle
     *            The handle to resolve
     * @return The database row corresponding to the handle
     * @exception SQLException
     *                If a database error occurs
     */
    private static TableRow findHandleInternal(Context context, String handle)
            throws SQLException
    {
        if (handle == null)
        {
            throw new IllegalArgumentException("Handle is null");
        }

        return DatabaseManager
                .findByUnique(context, "Handle", "handle", handle);
    }

    /**
     * Create a new handle id. The implementation uses the PK of the RDBMS
     * Handle table.
     * 
     * @return A new handle id
     * @exception SQLException
     *                If a database error occurs
     */
    private static String createId(int id) throws SQLException
    {
        String handlePrefix = ConfigurationManager.getProperty("handle.prefix");

        return new StringBuffer().append(handlePrefix).append(
                handlePrefix.endsWith("/") ? "" : "/").append(id).toString();
    }
}
