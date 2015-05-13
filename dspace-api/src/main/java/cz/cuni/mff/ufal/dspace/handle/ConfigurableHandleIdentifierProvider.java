/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package cz.cuni.mff.ufal.dspace.handle;

import cz.cuni.mff.ufal.DSpaceApi;
import cz.cuni.mff.ufal.dspace.content.Handle;
import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.*;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.handle.HandleManager;
import org.dspace.identifier.*;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;
import org.dspace.utils.DSpace;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;


/**
 *
 * Using spring framework for exposing lindat handle functionality
 *
 * Usage after it has been specified in config/spring/api/identifier-service.xml
 *
 *	IdentifierService pid_service_abstract = new DSpace().getSingletonService(IdentifierService.class);
 *
 */
@Component
public class ConfigurableHandleIdentifierProvider extends IdentifierProvider {
    /** log4j category */
    private static Logger log = Logger.getLogger(ConfigurableHandleIdentifierProvider.class);

    protected String[] supportedPrefixes = new String[]{"info:hdl", "hdl", "http://"};

    // ===================================================
    // lindat specific
    // ===================================================

    static final String PREFIX_DELIMITER = "/";

    static final String SUBPREFIX_DELIMITER = "-";

    static public final String EXAMPLE_PREFIX = "1234567899";


    public boolean canDelete(Context context) throws AuthorizeException, SQLException {
        return AuthorizeManager.isAdmin(context);
    }

    public static String getPrefix()
    {
        String prefix = PIDConfiguration.getDefaultPrefix();
        if (null == prefix)
        {
            prefix = EXAMPLE_PREFIX; // XXX no good way to exit cleanly
            log.error("default handle prefix is not configured; using " + prefix);
        }
        return prefix;
    }
    /**
     * Factory method for handle creation
     *
     * @param id
     * @param pidCommunityConfiguration
     * @return
     */
    private static String createHandleId(int id,
                                         PIDCommunityConfiguration pidCommunityConfiguration)
    {
        String handleId = null;
        if (pidCommunityConfiguration.isEpic())
        {
            handleId = createEpicHandleId(id, pidCommunityConfiguration);
        }
        else if (pidCommunityConfiguration.isLocal())
        {
            handleId = createLocalHandleId(id, pidCommunityConfiguration);
        }
        else
        {
            throw new IllegalStateException("Unsupported PID type: "
                + pidCommunityConfiguration.getType());
        }
        return handleId;
    }

    /**
     * Formats handle suffix
     *
     * @param id Database handle ID (primary key in handle table)
     * @param pidCommunityConfiguration PID Community Configuration
     * @return formatted handle suffix
     */
    private static String formatSuffix(int id, PIDCommunityConfiguration pidCommunityConfiguration)
    {
        StringBuffer suffix = new StringBuffer();
        String handleSubprefix = pidCommunityConfiguration.getSubprefix();
        if(handleSubprefix != null && !handleSubprefix.isEmpty())
        {
            suffix.append(handleSubprefix + SUBPREFIX_DELIMITER);
        }
        suffix.append(id);
        return suffix.toString();
    }

    /**
     * Formats handle
     *
     * @param id Database handle ID (primary key in handle table)
     * @param pidCommunityConfiguration PID Community Configuration
     * @return formatted handle
     */
    private static String formatHandleID(int id, PIDCommunityConfiguration pidCommunityConfiguration)
    {
        StringBuffer handleId = new StringBuffer();
        String handlePrefix = pidCommunityConfiguration.getPrefix();
        handleId.append(handlePrefix);

        if(!handlePrefix.endsWith(PREFIX_DELIMITER))
        {
            handleId.append(PREFIX_DELIMITER);
        }

        String handleSuffix = formatSuffix(id, pidCommunityConfiguration);
        handleId.append(handleSuffix);
        return handleId.toString();
    }

    /**
     * Creates new handle locally
     *
     * @param id
     *            Database handle ID (primary key in handle table)
     * @param pidCommunityConfiguration
     *            PID Community Configuration
     * @return
     */
    private static String createLocalHandleId(int id,
                                              PIDCommunityConfiguration pidCommunityConfiguration)
    {
        return formatHandleID(id, pidCommunityConfiguration);
    }

    /**
     * Creates new handle by calling EPIC service
     *
     * @param id
     *            Database handle ID (primary key in handle table)
     * @param pidCommunityConfiguration
     *            PIC Community Configuration
     * @return
     */
    private static String createEpicHandleId(int id,
                                             PIDCommunityConfiguration pidCommunityConfiguration)
    {
        String handleId;

        String suffix = formatSuffix(id, pidCommunityConfiguration);
        String prefix = pidCommunityConfiguration.getPrefix();

        try
        {
            handleId = DSpaceApi.handle_HandleManager_createId(log, id, prefix, suffix);
            // if the handle created successfully register the final handle
            DSpaceApi
                .handle_HandleManager_registerFinalHandleURL(log, handleId);
        }
        catch (IOException e)
        {
            DSpaceApi
                .getFunctionalityManager()
                .setErrorMessage(
                    "PID Service is not working. Please contact the administrator.");
            throw new IllegalStateException(
                "External PID service is not working. Please contact the administrator. "
                    + "Internal message: [" + e.toString() + "]");
        }
        return handleId;
    }

    /**
     * Get a list of all handles in the system.
     *
     * @param context
     *            DSpace context object
     *
     * @return the handles in the system
     */
    public static List<Handle> findAll(Context context) throws SQLException {
        TableRowIterator tri = DatabaseManager.queryTable(context, "handle",
            "SELECT * FROM handle");

        List<Handle> handles = new ArrayList<Handle>();

        try
        {
            while (tri.hasNext())
            {
                TableRow row = tri.next();

                // First check the cache
                Handle fromCache = (Handle) context.fromCache(
                    Handle.class, row.getIntColumn("handle_id"));

                if (fromCache != null)
                {
                    handles.add(fromCache);
                }
                else
                {
                    handles.add(new Handle(context, row));
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

        return handles;
    }

    /**
     * Changes the specified handle
     *
     * @param context
     *            DSpace context
     * @param oldHandle
     *            Old handle
     * @param newHandle
     *            New handle
     * @param archiveOldHandle
     *            Flag indicating whether the old handle should be stored in
     *            Item metadata
     * @throws Exception
     */
    public static void changeHandle(Context context,
                                    String oldHandle,
                                    String newHandle,
                                    boolean archiveOldHandle) throws SQLException, AuthorizeException
    {
        // Ignore invalid handles
        if(oldHandle == null)
        {
            return;
        }

        // Find handle
        Handle h = Handle.findByHandle(context, oldHandle);

        if (h == null)
        {
            throw new IllegalStateException("Handle " + oldHandle
                + "not found ");
        }

        Item item = null;

        if (h.isInternalResource())
        {
            // Try resolving handle to Item
            try
            {
                IdentifierService pid_service =
                    new DSpace().getSingletonService(IdentifierService.class);

                DSpaceObject dso = pid_service.resolve(context, oldHandle);
                if (dso != null && dso.getType() == Constants.ITEM)
                {
                    item = (Item) dso;
                }
            }
            catch (IllegalStateException e) {
                item = null;
            } catch (IdentifierNotResolvableException e) {
                e.printStackTrace();
            } catch (IdentifierNotFoundException e) {
                e.printStackTrace();
            }

            if (item != null)
            {
                // Handle resolved to Item
                if (archiveOldHandle)
                {
                    // Archive metadata
                    Metadatum[] dcUri = item.getMetadata("dc", "identifier",
                        "uri", Item.ANY);

                    List<String> values = new ArrayList<String>();

                    for (int i = 0; i < dcUri.length; i++)
                    {
                        values.add(dcUri[i].value);
                    }

                    item.addMetadata("dc", "identifier", "other", Item.ANY,
                        values.toArray(new String[values.size()]));
                }

                item.clearMetadata("dc", "identifier", "uri", Item.ANY);

                if (newHandle != null && !newHandle.isEmpty())
                {
                    // Update dc.identifier.uri
                    String newUri = getCanonicalForm(newHandle);
                    item.addMetadata("dc", "identifier", "uri", Item.ANY,
                        newUri);
                }

                // Update the metadata
                item.update();
            }
        }

        // Update the handle itself
        // - needs to be done before archiving handle to avoid unique constraint
        // violation
        h.setHandle(newHandle);
        h.update();

        if (archiveOldHandle)
        {
            if (!newHandle.equals(oldHandle))
            {
                // Archive handle
                String newUrl = HandleManager.resolveToURL(context, newHandle);
                Handle archiveHandle = Handle.create(context, null, oldHandle);
                archiveHandle.setURL(newUrl);
                archiveHandle.update();
            }
        }

    }


    /**
     * Changes handle prefix
     *
     * @param context
     *            DSpace context
     * @param oldPrefix
     *            Old handle prefix
     * @param newPrefix
     *            New handle prefix
     * @param archiveOldHandles
     *            Flag indicating whether the old handle should be stored in
     *            Item metadata
     * @throws Exception
     */
    public static void changePrefix( Context context,
                                     String oldPrefix,
                                     String newPrefix,
                                     boolean archiveOldHandles) throws Exception
    {
        // Iterates over the list of
        String sql = "select handle as old_handle, regexp_replace(handle,'^([^/]*)(/.*)$', ? || '\\2') as new_handle from handle"
            + " where handle like ? || '/%' order by handle_id";

        TableRowIterator iterator = DatabaseManager.query(context, sql,
            newPrefix, oldPrefix);

        try
        {
            while (iterator.hasNext())
            {
                TableRow row = (TableRow) iterator.next();
                String oldHandle = row.getStringColumn("old_handle");
                String newHandle = row.getStringColumn("new_handle");
                changeHandle(context, oldHandle, newHandle, archiveOldHandles);
            }
        }
        finally
        {
            // close the TableRowIterator to free up resources
            if (iterator != null)
            {
                iterator.close();
            }
        }
    }

    /**
     * Returns prefix of a given handle
     *
     * @param handle Prefix of the handle
     */
    public static String extractHandlePrefix(String handle)
    {
        String prefix = null;
        if(handle != null)
        {
            int pos = handle.indexOf(PREFIX_DELIMITER);
            if(pos >= 0)
            {
                prefix = handle.substring(0, pos);
            }
        }
        return prefix;
    }

    /**
     * Returns suffix of a given handle
     *
     * @param handle Suffix of the handle
     */
    public static String extractHandleSuffix(String handle)
    {
        String suffix = null;
        if(handle != null)
        {
            int pos = handle.indexOf(PREFIX_DELIMITER);
            if(pos >= 0)
            {
                suffix = handle.substring(pos+1);
            }
        }
        return suffix;
    }

    public static boolean isSupportedPrefix(String prefix)
    {
        Set<String> supportedPrefixes = PIDConfiguration.getSupportedPrefixes();
        return supportedPrefixes.contains(prefix);
    }

    /**
     * Returns complete handle made from prefix and suffix
     */
    public static String completeHandle(String prefix, String suffix)
    {
        return prefix + PREFIX_DELIMITER + suffix;
    }

    public static DSpaceObject resolveToObject(Context context, int handle_id) throws SQLException
    {
        TableRow handle = DatabaseManager.find(context, "handle", handle_id);
        if (handle == null) {
            if (log.isDebugEnabled()) {
                log.error(LogManager.getHeader(context, "No object for specified handle_id", "Handle id: " + handle_id) );
            }
            return null;
        } else {
            IdentifierService pid_service =
                new DSpace().getSingletonService(IdentifierService.class);
            try {
                return pid_service.resolve(context, handle.getStringColumn("handle"));
            } catch (IdentifierNotFoundException e) {
            } catch (IdentifierNotResolvableException e) {
            }
            return null;
        }
    }

    public static String modifyHandleRecord(Context context, TableRow handle, String handleId, int resourceTypeID, int resourceID) throws SQLException {
        if ( null == handle) {
            handle = findHandleInternal(context, handleId);
        }
        if ( null == handle ) {
            log.warn("Could not find handle record for " + handleId);
            return null;
        }
        handle.setColumn("handle", handleId);
        handle.setColumn("resource_type_id", resourceTypeID);
        handle.setColumn("resource_id", resourceID);
        DatabaseManager.update(context, handle);

        if (log.isDebugEnabled()) {
            log.debug("Created new handle for "
                + Constants.typeText[resourceTypeID] + " " + handleId);
        }
        return handleId;
    }



    // ===================================================


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
        try {
            String id = mint(context, dso);

            // move canonical to point the latest version
            if (dso instanceof Item) {
                Item item = (Item) dso;
                populateHandleMetadata(item, id);
            }
            return id;
        } catch (Exception e) {
            log.error(LogManager.getHeader(context, "Error while attempting to create handle", "Item id: " + dso.getID()), e);
            throw new RuntimeException("Error while attempting to create identifier for Item id: " + dso.getID(), e);
        }
    }

    public void register(Context context, DSpaceObject dso, String identifier) {
        try{
            createNewIdentifier(context, dso, identifier);
            if (dso instanceof Item) {
                Item item = (Item) dso;
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

                if(dbhandle == null) {
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
        if (row != null) {
            //Only set the "resouce_id" column to null when unbinding a handle.
            // We want to keep around the "resource_type_id" value, so that we
            // can verify during a restore whether the same *type* of resource
            // is reusing this handle!
            if(canDelete(context)) {
                row.setColumnNull("resource_id");
                DatabaseManager.update(context, row);
            }

            if(log.isDebugEnabled()) {
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
        } catch (AuthorizeException e) {
            throw new IdentifierException(e.getMessage(), e);
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
                if(null != dso &&
                    handle.getIntColumn("resource_id")==dso.getID() &&
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
            // if not user supplied handle
            if ( null == handleId ) {
                PIDCommunityConfiguration pidCommunityConfiguration = PIDConfiguration
                    .getPIDCommunityConfiguration(dso);
                handleId = createHandleId(
                    handle.getIntColumn("handle_id"), pidCommunityConfiguration);
            }
        }

        modifyHandleRecord(context, dso, handle, handleId);
        return handleId;
    }

    protected String modifyHandleRecord(Context context, DSpaceObject dso, TableRow handle, String handleId) throws SQLException {
        return modifyHandleRecord(
            context,
            handle,
            handleId,
            null == dso ? -1 : dso.getType(),
            null == dso ? -1 : dso.getID()
        );
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

    /**
     * List all distinct prefixes stored in Handle table.
     *
     * @param context
     *            DSpace context
     * @return Alphabetically sorted list of handle prefixes
     * @exception SQLException
     *                If a database error occurs
     */
    public static List<String> getPrefixes(Context context) throws SQLException
    {
        List<String> prefixes = new ArrayList<String>();
        String sql = "select distinct regexp_replace(handle,'^([^/]*).*$','\\\\1') as prefix from handle where handle	 like '%/%' order by prefix";

        TableRowIterator iterator = DatabaseManager.query(context, sql);

        try
        {
            while (iterator.hasNext())
            {
                TableRow row = (TableRow) iterator.next();
                prefixes.add(row.getStringColumn("prefix"));
            }
        }
        finally
        {
            // close the TableRowIterator to free up resources
            if (iterator != null)
            {
                iterator.close();
            }
        }

        return prefixes;
    }

} // class
