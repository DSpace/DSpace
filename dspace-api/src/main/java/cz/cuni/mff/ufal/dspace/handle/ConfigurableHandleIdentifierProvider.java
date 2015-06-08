/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package cz.cuni.mff.ufal.dspace.handle;

import cz.cuni.mff.ufal.DSpaceApi;
import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.*;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.handle.HandleManager;
import org.dspace.identifier.IdentifierNotFoundException;
import org.dspace.identifier.IdentifierNotResolvableException;
import org.dspace.identifier.IdentifierProvider;
import org.dspace.identifier.Identifier;
import org.dspace.identifier.IdentifierException;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;
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
    // IdentifierProvider api
    // ===================================================

    @Override
    public boolean supports(Class<? extends Identifier> identifier) {
        return Handle.class.isAssignableFrom(identifier);
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

    @Override
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

    @Override
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

    @Override
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

    @Override
    public void reserve(Context context, DSpaceObject dso, String identifier) {
        try{
            Handle handle = Handle.create( context, dso, identifier );
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
    @Override
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

    @Override
    public DSpaceObject resolve(Context context, String identifier, String... attributes) {
        // We can do nothing with this, return null
        try
        {
            Handle handle = Handle.findByHandle(context, identifier);

            if (handle == null) {
                // Check for an url
                identifier = retrieveHandleOutOfUrl(identifier);
                if(identifier != null) {
                    handle = Handle.findByHandle(context, identifier);
                }

                if (handle== null) {
                    return null;
                }
            }

            int handle_type_id = handle.getResourceTypeID();
            int resource_id = handle.getResourceID();

            if ( 0 > handle_type_id || 0 > resource_id ) {
                throw new IllegalStateException("No associated resource type");
            }

            // What are we looking at here?

            if (handle_type_id == Constants.ITEM)
            {
                Item item = Item.find(context, resource_id);

                if (log.isDebugEnabled()) {
                    log.debug("Resolved handle " + identifier + " to item "
                        + ((item == null) ? (-1) : item.getID()));
                }

                return item;
            }
            else if (handle_type_id == Constants.COLLECTION)
            {
                Collection collection = Collection.find(context, resource_id);

                if (log.isDebugEnabled()) {
                    log.debug("Resolved handle " + identifier + " to collection "
                        + ((collection == null) ? (-1) : collection.getID()));
                }

                return collection;
            }
            else if (handle_type_id == Constants.COMMUNITY)
            {
                Community community = Community.find(context, resource_id);

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

        return null;
    }

    @Override
    public void delete(Context context, DSpaceObject dso) throws IdentifierException {
        try{
            TableRow row = getHandleInternal(context, dso.getType(), dso.getID());
            if (row != null) {
                //Only set the "resource_id" column to null when unbinding a handle.
                // We want to keep around the "resource_type_id" value, so that we
                // can verify during a restore whether the same *type* of resource
                // is reusing this handle!
                Handle handle = new Handle( context, row );
                handle.setResourceID(-1);
                handle.update();

                if(log.isDebugEnabled()) {
                    log.debug("Unbound Handle " + handle.getHandle() + " from object " + Constants.typeText[dso.getType()] + " id=" + dso.getID());
                }

            }
            else
            {
                log.warn("Cannot find Handle entry to unbind for object " + Constants.typeText[dso.getType()] + " id=" + dso.getID());
            }
        }catch(SQLException | AuthorizeException sqe)
        {
            throw new IdentifierException(sqe.getMessage(),sqe);
        }

    }

    // ===================================================
    // lindat api
    // ===================================================

    public static String modifyHandle(Context context,
                                      Handle handle,
                                      String handle_str,
                                      int resourceTypeID,
                                      int resourceID,
                                      String url,
                                      boolean archive)
        throws SQLException, AuthorizeException
    {
        if ( null == handle ) {
            log.warn("Could not find handle record for " + handle_str);
            return null;
        }

        String old_handle_str = handle.getHandle();
        Item item = null;
        if (handle.isInternalResource())
        {
            // Try resolving handle to Item
            try
            {
                DSpaceObject dso = new ConfigurableHandleIdentifierProvider().resolve(
                    context, old_handle_str);
                if (dso != null && dso.getType() == Constants.ITEM) {
                    item = (Item) dso;
                }
            }
            catch (IllegalStateException e) {
                item = null;
            }

            // Update Item's metadata
            if ( null != item  ) {

                // Handle resolved to Item
                if (archive) {
                    // Archive metadata
                    Metadatum[] dcUri = item.getMetadata("dc", "identifier", "uri", Item.ANY);
                    List<String> values = new ArrayList<>();
                    for (Metadatum aDcUri : dcUri) {
                        values.add(aDcUri.value);
                    }
                    item.addMetadata("dc", "identifier", "other", Item.ANY,
                        values.toArray(new String[values.size()]));
                }

                // Clear dc.identifier.uri
                item.clearMetadata("dc", "identifier", "uri", Item.ANY);

                // Update dc.identifier.uri
                if (handle_str != null && !handle_str.isEmpty()) {
                    String newUri = HandleManager.getCanonicalForm(handle_str);
                    item.addMetadata("dc", "identifier", "uri", Item.ANY, newUri);
                }

                // Update the metadata
                item.update();
            }
        }

        handle.setHandle(handle_str);;
        handle.setURL(url);
        handle.setResourceTypeID(resourceTypeID);
        handle.setResourceID(resourceID);
        handle.update();

        // Archive handle
        if ( archive ) {
            if (!handle_str.equals(old_handle_str)) {
                String newUrl = HandleManager.resolveToURL(context, handle_str);
                Handle archivedHandle = Handle.create(context, null, old_handle_str);
                archivedHandle.setURL(newUrl);
                archivedHandle.update();
            }
        }


        if (log.isDebugEnabled()) {
            log.debug("Created new handle for "
                + Constants.typeText[resourceTypeID] + " " + handle_str);
        }
        return handle_str;
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
                Handle h = Handle.findByHandle(context, oldHandle);
                modifyHandle(context, h, newHandle, h.getType(), h.getID(), h.getURL(), archiveOldHandles);
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

    public static void permanent_remove(Context context, int handleID )
        throws SQLException, AuthorizeException, IOException
    {
        Handle handleDeleted = Handle.find(context, handleID);
        handleDeleted.delete();
        context.commit();
    }

    public static Handle resolveToHandle(Context context, int handleID ) throws SQLException {
        return Handle.find( context, handleID );
    }

    public static Handle resolveToHandle(Context context, String handle_str ) throws SQLException {
        return Handle.findByHandle(context, handle_str);
    }

    public static List<Handle> findAll( Context context ) throws SQLException {
        return Handle.findAll(context);
    }

    // ===================================================
    // lindat specific
    // ===================================================

    static final String PREFIX_DELIMITER = "/";

    static final String SUBPREFIX_DELIMITER = "-";

    static public final String EXAMPLE_PREFIX = "1234567899";


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
     * Returns prefix/suffix or null/null.
     *
     * @param handle Prefix of the handle
     */
    public static String[] splitHandle(String handle)
    {
        if(handle != null) {
            return handle.split(PREFIX_DELIMITER);
        }
        return new String[] { null, null };
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

    protected String createNewIdentifier(Context context, DSpaceObject dso, String handle_str) throws SQLException, AuthorizeException {
        Handle handle=null;
        if (handle_str != null) {
            handle = Handle.findByHandle(context, handle_str);
            if ( null == handle ) {
                handle = Handle.create(context, dso, handle_str);
            }
        }

        if (handle!=null && -1 != handle.getResourceID())
        {
            // Check if this handle is already linked up to this specified DSpace Object
            if(null != dso &&
                handle.getResourceID()==dso.getID() &&
                    handle.getResourceTypeID()==dso.getType())
            {
                // This handle already links to this DSpace Object -- so, there's nothing else we need to do
                return handle_str;
            }
            else
            {
                //handle found in DB table & already in use by another existing resource
                throw new IllegalStateException("Attempted to create a handle which is already " +
                    "in use: handle_id=" + handle_str);
            }
        }

        if (handle!=null && -1 != handle.getResourceTypeID())
        {
            //If there is a 'resource_type_id' (but 'resource_id' is empty), then the object using
            // this handle was previously unbound (see unbindHandle() method) -- likely because object was deleted
            int previousType = handle.getResourceTypeID();

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
            handle = Handle.create(context, dso);
            // if user did not supply a handle
            if ( null == handle_str ) {
                PIDCommunityConfiguration pidCommunityConfiguration = PIDConfiguration
                    .getPIDCommunityConfiguration(dso);
                try{
                    handle_str = createHandleId(handle.getID(), pidCommunityConfiguration);
                }catch(IllegalStateException e){
                    try
                    {
                        //Failed to register the handle (eg with epic), clean up
                        handle.delete();
                        handle = null;
                    }
                    catch (AuthorizeException|IOException|SQLException  e1)
                    {
                        log.error("Failed handle cleanup." + e);
                    }
                    throw e;
                }
            }
        }

        try {
            if ( null == dso ) {
                modifyHandle(context, handle, handle_str, -1, -1, null, false);
            }else {
                modifyHandle(context, handle, handle_str, dso.getType(), dso.getID(), null, false);
            }
        } catch (AuthorizeException e) {
            log.error( "Someone tried to create a handle without correct permissions" );
        }
        return handle_str;
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

    protected void populateHandleMetadata(Item item, String handle)
            throws SQLException, IOException, AuthorizeException
    {
        String handleref = HandleManager.getCanonicalForm(handle);

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
        List<String> prefixes = new ArrayList<>();
        String sql = "select distinct regexp_replace(handle,'^([^/]*).*$','\\1') as prefix from handle where handle like '%/%' order by prefix";

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

    // ===================================================
    // formatting of handles
    // ===================================================

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

    // ===================================================
    // different handle providers
    // ===================================================

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

} // class
