/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.identifier;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.*;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.sql.SQLException;

/**
 * The old DSpace handle identifier service, used to create handles or retrieve objects based on their handle
 *
 * @author Fabio Bolognesi (fabio at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 */
@Component
public class HandleIdentifierProvider extends IdentifierProvider {
    /** log4j category */
    private static Logger log = Logger.getLogger(HandleIdentifierProvider.class);

    /** Prefix registered to no one */
    protected static final String EXAMPLE_PREFIX = "123456789";

    protected String[] supportedPrefixes = new String[]{"info:hdl", "hdl", "http://"};

    @Override
    public boolean supports(Class<? extends Identifier> identifier) {
        return Handle.class.isAssignableFrom(identifier);
    }

    public boolean supports(String identifier)
    {
        for(String prefix : supportedPrefixes){
            if(identifier.startsWith(prefix))
            {
                return true;
            }
        }

        try {
            String outOfUrl = retrieveHandleOutOfUrl(identifier);
            if(outOfUrl != null)
            {
                return true;
            }
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
        }

        return false;
    }

    public String register(Context context, DSpaceObject dso) {
        try{
            String id = mint(context, dso);

            // move canonical to point the latest version
            if(dso instanceof Item)
            {
                Item item = (Item)dso;
                populateHandleMetadata(item, id);
            }

            return id;
        }catch (Exception e){
            log.error(LogManager.getHeader(context, "Error while attempting to create handle", "Item id: " + dso.getID()), e);
            throw new RuntimeException("Error while attempting to create identifier for Item id: " + dso.getID(), e);
        }
    }

    public void register(Context context, DSpaceObject dso, String identifier) {
        try{
            createNewIdentifier(context, dso, identifier);
            if(dso instanceof Item)
            {
                Item item = (Item)dso;
                populateHandleMetadata(item, identifier);
            }
        }catch (Exception e){
            log.error(LogManager.getHeader(context, "Error while attempting to create handle", "Item id: " + dso.getID()), e);
            throw new RuntimeException("Error while attempting to create identifier for Item id: " + dso.getID(), e);
        }
    }


    public void reserve(Context context, DSpaceObject dso, String identifier) {
        try{
            TableRow handle = DatabaseManager.create(context, "Handle");
            modifyHandleRecord(context, dso, handle, identifier);
        }catch(Exception e){
            log.error(LogManager.getHeader(context, "Error while attempting to create handle", "Item id: " + dso.getID()), e);
            throw new RuntimeException("Error while attempting to create identifier for Item id: " + dso.getID());
        }
    }


    /**
     * Creates a new handle in the database.
     *
     * @param context DSpace context
     * @param dso The DSpaceObject to create a handle for
     * @return The newly created handle
     * @exception java.sql.SQLException If a database error occurs
     */
    public String mint(Context context, DSpaceObject dso) {
        if(dso.getHandle() != null)
        {
            return dso.getHandle();
        }

        try{
            return createNewIdentifier(context, dso, null);
        }catch (Exception e){
            log.error(LogManager.getHeader(context, "Error while attempting to create handle", "Item id: " + dso.getID()), e);
            throw new RuntimeException("Error while attempting to create identifier for Item id: " + dso.getID());
        }
    }

    public DSpaceObject resolve(Context context, String identifier, String... attributes) {
        // We can do nothing with this, return null
        try
        {
            TableRow dbhandle = findHandleInternal(context, identifier);

            if (dbhandle == null)
            {
                //Check for an url
                identifier = retrieveHandleOutOfUrl(identifier);
                if(identifier != null)
                {
                    dbhandle = findHandleInternal(context, identifier);
                }

                if(dbhandle == null)
                {
                    return null;
                }
            }

            if ((dbhandle.isColumnNull("resource_type_id")) || (dbhandle.isColumnNull("resource_id")))
            {
                throw new IllegalStateException("No associated resource type");
            }

            // What are we looking at here?
            int handletypeid = dbhandle.getIntColumn("resource_type_id");
            int resourceID = dbhandle.getIntColumn("resource_id");

            if (handletypeid == Constants.ITEM)
            {
                Item item = Item.find(context, resourceID);

                if (log.isDebugEnabled()) {
                    log.debug("Resolved handle " + identifier + " to item "
                            + ((item == null) ? (-1) : item.getID()));
                }

                return item;
            }
            else if (handletypeid == Constants.COLLECTION)
            {
                Collection collection = Collection.find(context, resourceID);

                if (log.isDebugEnabled()) {
                    log.debug("Resolved handle " + identifier + " to collection "
                            + ((collection == null) ? (-1) : collection.getID()));
                }

                return collection;
            }
            else if (handletypeid == Constants.COMMUNITY)
            {
                Community community = Community.find(context, resourceID);

                if (log.isDebugEnabled())
                {
                    log.debug("Resolved handle " + identifier + " to community "
                            + ((community == null) ? (-1) : community.getID()));
                }

                return community;
            }
        }catch (Exception e){
            log.error(LogManager.getHeader(context, "Error while resolving handle to item", "handle: " + identifier), e);
        }
//        throw new IllegalStateException("Unsupported Handle Type "
//                + Constants.typeText[handletypeid]);
        return null;
    }

    @Override
    public String lookup(Context context, DSpaceObject dso) throws IdentifierNotFoundException, IdentifierNotResolvableException {

        try
        {
            TableRow row = getHandleInternal(context, dso.getType(), dso.getID());
            if (row == null)
            {
                if (dso.getType() == Constants.SITE)
                {
                    return Site.getSiteHandle();
                }
                else
                {
                    return null;
                }
            }
            else
            {
                return row.getStringColumn("handle");
            }
        }catch(SQLException sqe){
            throw new IdentifierNotResolvableException(sqe.getMessage(),sqe);
        }
    }

    @Override
    public void delete(Context context, DSpaceObject dso, String identifier) throws IdentifierException {
        delete(context, dso);
    }

    public void delete(Context context, DSpaceObject dso) throws IdentifierException {
        try{
        TableRow row = getHandleInternal(context, dso.getType(), dso.getID());
        if (row != null)
        {
            //Only set the "resouce_id" column to null when unbinding a handle.
            // We want to keep around the "resource_type_id" value, so that we
            // can verify during a restore whether the same *type* of resource
            // is reusing this handle!
            row.setColumnNull("resource_id");
            DatabaseManager.update(context, row);

            if(log.isDebugEnabled())
            {
                log.debug("Unbound Handle " + row.getStringColumn("handle") + " from object " + Constants.typeText[dso.getType()] + " id=" + dso.getID());
            }

        }
        else
        {
            log.warn("Cannot find Handle entry to unbind for object " + Constants.typeText[dso.getType()] + " id=" + dso.getID());
        }
        }catch(SQLException sqe)
        {
            throw new IdentifierException(sqe.getMessage(),sqe);
        }

    }

    public static String retrieveHandleOutOfUrl(String url)
            throws SQLException {
        // We can do nothing with this, return null
        if (!url.contains("/"))
        {
            return null;
        }

        String[] splitUrl = url.split("/");

        return splitUrl[splitUrl.length - 2] + "/" + splitUrl[splitUrl.length - 1];
    }

    /**
     * Get the configured Handle prefix string, or a default
     * @return configured prefix or "123456789"
     */
    public static String getPrefix()
    {
        String prefix = ConfigurationManager.getProperty("handle.prefix");
        if (null == prefix)
        {
            prefix = EXAMPLE_PREFIX; // XXX no good way to exit cleanly
            log.error("handle.prefix is not configured; using " + prefix);
        }
        return prefix;
    }

    protected static String getCanonicalForm(String handle)
    {

        // Let the admin define a new prefix, if not then we'll use the
        // CNRI default. This allows the admin to use "hdl:" if they want to or
        // use a locally branded prefix handle.myuni.edu.
        String handlePrefix = ConfigurationManager.getProperty("handle.canonical.prefix");
        if (handlePrefix == null || handlePrefix.length() == 0)
        {
            handlePrefix = "http://hdl.handle.net/";
        }

        return handlePrefix + handle;
    }

    protected String createNewIdentifier(Context context, DSpaceObject dso, String handleId) throws SQLException {
        TableRow handle=null;
        if(handleId != null)
        {
            handle = findHandleInternal(context, handleId);


            if(handle!=null && !handle.isColumnNull("resource_id"))
            {
                //Check if this handle is already linked up to this specified DSpace Object
                if(handle.getIntColumn("resource_id")==dso.getID() &&
                        handle.getIntColumn("resource_type_id")==dso.getType())
                {
                    //This handle already links to this DSpace Object -- so, there's nothing else we need to do
                    return handleId;
                }
                else
                {
                    //handle found in DB table & already in use by another existing resource
                    throw new IllegalStateException("Attempted to create a handle which is already in use: " + handleId);
                }
            }

        }
        else if(handle!=null && !handle.isColumnNull("resource_type_id"))
        {
            //If there is a 'resource_type_id' (but 'resource_id' is empty), then the object using
            // this handle was previously unbound (see unbindHandle() method) -- likely because object was deleted
            int previousType = handle.getIntColumn("resource_type_id");

            //Since we are restoring an object to a pre-existing handle, double check we are restoring the same *type* of object
            // (e.g. we will not allow an Item to be restored to a handle previously used by a Collection)
            if(previousType != dso.getType())
            {
                throw new IllegalStateException("Attempted to reuse a handle previously used by a " +
                        Constants.typeText[previousType] + " for a new " +
                        Constants.typeText[dso.getType()]);
            }
        }

        if(handle==null){
            handle = DatabaseManager.create(context, "Handle");
            handleId = createId(handle.getIntColumn("handle_id"));
        }

        modifyHandleRecord(context, dso, handle, handleId);
        return handleId;
    }

    protected String modifyHandleRecord(Context context, DSpaceObject dso, TableRow handle, String handleId) throws SQLException {
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
     * Return the handle for an Object, or null if the Object has no handle.
     *
     * @param context
     *            DSpace context
     * @param type
     *            The type of object
     * @param id
     *            The id of object
     * @return The handle for object, or null if the object has no handle.
     * @exception java.sql.SQLException
     *                If a database error occurs
     */
    protected static TableRow getHandleInternal(Context context, int type, int id)
            throws SQLException
    {
        String sql = "SELECT * FROM Handle WHERE resource_type_id = ? " +
                "AND resource_id = ?";

        return DatabaseManager.querySingleTable(context, "Handle", sql, type, id);
    }

    /**
     * Find the database row corresponding to handle.
     *
     * @param context DSpace context
     * @param handle The handle to resolve
     * @return The database row corresponding to the handle
     * @exception java.sql.SQLException If a database error occurs
     */
    protected static TableRow findHandleInternal(Context context, String handle)
            throws SQLException {
        if (handle == null)
        {
            throw new IllegalArgumentException("Handle is null");
        }
        return DatabaseManager.findByUnique(context, "Handle", "handle", handle);
    }

    /**
     * Create a new handle id. The implementation uses the PK of the RDBMS
     * Handle table.
     *
     * @return A new handle id
     * @exception java.sql.SQLException
     *                If a database error occurs
     */
    protected static String createId(int id) throws SQLException
    {
        String handlePrefix = getPrefix();

        return handlePrefix + (handlePrefix.endsWith("/") ? "" : "/") + id;
    }


    protected void populateHandleMetadata(Item item, String handle)
            throws SQLException, IOException, AuthorizeException
    {
        String handleref = getCanonicalForm(handle);

        // Add handle as identifier.uri DC value.
        // First check that identifier doesn't already exist.
        boolean identifierExists = false;
        Metadatum[] identifiers = item.getDC("identifier", "uri", Item.ANY);
        for (Metadatum identifier : identifiers)
        {
            if (handleref.equals(identifier.value))
            {
                identifierExists = true;
            }
        }
        if (!identifierExists)
        {
            item.addDC("identifier", "uri", null, handleref);
        }
    }
}
