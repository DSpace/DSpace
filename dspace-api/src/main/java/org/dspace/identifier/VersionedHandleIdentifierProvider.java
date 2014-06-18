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
import org.dspace.utils.DSpace;
import org.dspace.versioning.*;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Date;

/**
 *
 *
 * @author Fabio Bolognesi (fabio at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 */
@Component
public class VersionedHandleIdentifierProvider extends IdentifierProvider {
    /** log4j category */
    private static Logger log = Logger.getLogger(VersionedHandleIdentifierProvider.class);

    /** Prefix registered to no one */
    static final String EXAMPLE_PREFIX = "123456789";

    private static final char DOT = '.';

    private String[] supportedPrefixes = new String[]{"info:hdl", "hdl", "http://"};

    private VersionDAO versionDAO;
    private VersionHistoryDAO versionHistoryDAO;

    @Override
    public boolean supports(Class<? extends Identifier> identifier)
    {
        return Handle.class.isAssignableFrom(identifier);
    }

    public boolean supports(String identifier)
    {
        for(String prefix : supportedPrefixes)
        {
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

    public String register(Context context, DSpaceObject dso)
    {
        try
        {
            String id = mint(context, dso);

            // move canonical to point the latest version
            if(dso != null && dso.getType() == Constants.ITEM)
            {
                Item item = (Item)dso;
                VersionHistory history = retrieveVersionHistory(context, (Item)dso);
                if(history!=null)
                {
                    String canonical = getCanonical(item);
                    // Modify Canonical: 12345/100 will point to the new item
                    TableRow canonicalRecord = findHandleInternal(context, canonical);
                    modifyHandleRecord(context, dso, canonicalRecord, canonical);

                    // in case of first version we have to modify the previous metadata to be xxxx.1
                    Version version = history.getVersion(item);
                    Version previous = history.getPrevious(version);
                    if (history.isFirstVersion(previous))
                    {
                        modifyHandleMetadata(previous.getItem(), (canonical + DOT + 1));
                    }
                    // Check if our previous item hasn't got a handle anymore.
                    // This only occurs when a switch has been made from the standard handle identifier provider
                    // to the versioned one, in this case no "versioned handle" is reserved so we need to create one
                    if(previous != null && getHandleInternal(context, Constants.ITEM, previous.getItemID()) == null){
                        makeIdentifierBasedOnHistory(context, previous.getItem(), canonical, history);

                    }
                }
                populateHandleMetadata(item);
            }

            return id;
        }catch (Exception e){
            log.error(LogManager.getHeader(context, "Error while attempting to create handle", "Item id: " + (dso != null ? dso.getID() : "")), e);
            throw new RuntimeException("Error while attempting to create identifier for Item id: " + (dso != null ? dso.getID() : ""));
        }
    }

    public void register(Context context, DSpaceObject dso, String identifier)
    {
        try
        {

            Item item = (Item) dso;

            // if for this identifier is already present a record in the Handle table and the corresponding item
            // has an history someone is trying to restore the latest version for the item. When
            // trying to restore the latest version the identifier in input doesn't have the for 1234/123.latestVersion
            // it is the canonical 1234/123
            VersionHistory itemHistory = getHistory(context, identifier);
            if(!identifier.matches(".*/.*\\.\\d+") && itemHistory!=null){

                int newVersionNumber = itemHistory.getLatestVersion().getVersionNumber()+1;
                String canonical = identifier;
                identifier = identifier.concat(".").concat("" + newVersionNumber);
                restoreItAsVersion(context, dso, identifier, item, canonical, itemHistory);
            }
            // if identifier == 1234.5/100.4 reinstate the version 4 in the version table if absent
            else if(identifier.matches(".*/.*\\.\\d+"))
            {
                // if it is a version of an item is needed to put back the record
                // in the versionitem table
                String canonical = getCanonical(identifier);
                DSpaceObject canonicalItem = this.resolve(context, canonical);
                if(canonicalItem==null){
                    restoreItAsCanonical(context, dso, identifier, item, canonical);
                }
                else{
                    VersionHistory history = retrieveVersionHistory(context, (Item)canonicalItem);
                    if(history==null){
                        restoreItAsCanonical(context, dso, identifier, item, canonical);
                    }
                    else
                    {
                        restoreItAsVersion(context, dso, identifier, item, canonical, history);

                    }
                }
            }
            else
            {
                //A regular handle
                createNewIdentifier(context, dso, identifier);
                if(dso instanceof Item)
                {
                    populateHandleMetadata(item);
                }
            }
        }catch (Exception e){
            log.error(LogManager.getHeader(context, "Error while attempting to create handle", "Item id: " + dso.getID()), e);
            throw new RuntimeException("Error while attempting to create identifier for Item id: " + dso.getID(), e);
        }
    }

    private VersionHistory getHistory(Context context, String identifier)
    {
        DSpaceObject item = this.resolve(context, identifier);
        if(item!=null){
            VersionHistory history = retrieveVersionHistory(context, (Item)item);
            return history;
        }
        return null;
    }

    private void restoreItAsVersion(Context context, DSpaceObject dso, String identifier, Item item, String canonical, VersionHistory history) throws SQLException, IOException, AuthorizeException
    {
        createNewIdentifier(context, dso, identifier);
        populateHandleMetadata(item);

        int versionNumber = Integer.parseInt(identifier.substring(identifier.lastIndexOf(".") + 1));
        createVersion(context, history, item, "Restoring from AIP Service", new Date(), versionNumber);
        Version latest = history.getLatestVersion();


        // if restoring the lastest version: needed to move the canonical
        if(latest.getVersionNumber() < versionNumber){
            TableRow canonicalRecord = findHandleInternal(context, canonical);
            modifyHandleRecord(context, dso, canonicalRecord, canonical);
        }
    }

    private void restoreItAsCanonical(Context context, DSpaceObject dso, String identifier, Item item, String canonical) throws SQLException, IOException, AuthorizeException
    {
        createNewIdentifier(context, dso, identifier);
        populateHandleMetadata(item);

        int versionNumber = Integer.parseInt(identifier.substring(identifier.lastIndexOf(".")+1));
        VersionHistory history=versionHistoryDAO.create(context);
        createVersion(context, history, item, "Restoring from AIP Service", new Date(), versionNumber);

        TableRow canonicalRecord = findHandleInternal(context, canonical);
        modifyHandleRecord(context, dso, canonicalRecord, canonical);

    }


    public void reserve(Context context, DSpaceObject dso, String identifier)
    {
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
     */
    public String mint(Context context, DSpaceObject dso)
    {
        if(dso.getHandle() != null)
        {
            return dso.getHandle();
        }

        try{
            String handleId = null;
            VersionHistory history = null;
            if(dso instanceof Item)
            {
                history = retrieveVersionHistory(context, (Item)dso);
            }

            if(history!=null)
            {
                handleId = makeIdentifierBasedOnHistory(context, dso, handleId, history);
            }else{
                handleId = createNewIdentifier(context, dso, null);
            }
            return handleId;
        }catch (Exception e){
            log.error(LogManager.getHeader(context, "Error while attempting to create handle", "Item id: " + dso.getID()), e);
            throw new RuntimeException("Error while attempting to create identifier for Item id: " + dso.getID());
        }
    }

    public DSpaceObject resolve(Context context, String identifier, String... attributes)
    {
        // We can do nothing with this, return null
        try{
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

                if (log.isDebugEnabled()) {
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

        try {
            if (dso instanceof Item)
            {
                Item item = (Item) dso;

                // If it is the most current version occurs to move the canonical to the previous version
                VersionHistory history = retrieveVersionHistory(context, item);
                if(history!=null && history.getLatestVersion().getItem().equals(item) && history.size() > 1)
                {
                    Item previous = history.getPrevious(history.getLatestVersion()).getItem();

                    // Modify Canonical: 12345/100 will point to the new item
                    String canonical = getCanonical(previous);
                    TableRow canonicalRecord = findHandleInternal(context, canonical);
                    modifyHandleRecord(context, previous, canonicalRecord, canonical);
                }
            }
        } catch (Exception e) {
            log.error(LogManager.getHeader(context, "Error while attempting to register doi", "Item id: " + dso.getID()), e);
            throw new IdentifierException("Error while moving doi identifier", e);
        }


    }

    public static String retrieveHandleOutOfUrl(String url) throws SQLException
    {
        // We can do nothing with this, return null
        if (!url.contains("/")) return null;

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
                int resourceID = handle.getIntColumn("resource_id");
                int resourceType = handle.getIntColumn("resource_type_id");

                if(resourceID==dso.getID() && resourceType ==dso.getType())
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
        }

        if(handleId==null)
            handleId = createId(handle.getIntColumn("handle_id"));

        modifyHandleRecord(context, dso, handle, handleId);

        return handleId;
    }

    protected String makeIdentifierBasedOnHistory(Context context, DSpaceObject dso, String handleId, VersionHistory history) throws AuthorizeException, SQLException
    {
        Item item = (Item)dso;

        // FIRST time a VERSION is created 2 identifiers will be minted  and the canonical will be updated to point to the newer URL:
        //  - id.1-->old URL
        //  - id.2-->new URL
        Version version = history.getVersion(item);
        Version previous = history.getPrevious(version);
        String canonical = getCanonical(previous.getItem());
        if (history.isFirstVersion(previous))
        {
            // add a new Identifier for previous item: 12345/100.1
            String identifierPreviousItem=canonical + DOT + 1;
            //Make sure that this hasn't happened already
            if(findHandleInternal(context, identifierPreviousItem) == null)
            {
                TableRow handle = DatabaseManager.create(context, "Handle");
                modifyHandleRecord(context, previous.getItem(), handle, identifierPreviousItem);
            }
        }


        // add a new Identifier for this item: 12345/100.x
        String idNew = canonical + DOT + version.getVersionNumber();
        //Make sure we don't have an old handle hanging around (if our previous version was deleted in the workspace)
        TableRow handleRow = findHandleInternal(context, idNew);
        if(handleRow == null)
        {
            handleRow = DatabaseManager.create(context, "Handle");
        }
        modifyHandleRecord(context, dso, handleRow, idNew);

        return handleId;
    }


    protected String modifyHandleRecord(Context context, DSpaceObject dso, TableRow handle, String handleId) throws SQLException
    {
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

    protected String getCanonical(Item item)
    {
        String canonical = item.getHandle();
        if( canonical.matches(".*/.*\\.\\d+") && canonical.lastIndexOf(DOT)!=-1)
        {
            canonical =  canonical.substring(0, canonical.lastIndexOf(DOT));
        }

        return canonical;
    }

    protected String getCanonical(String identifier)
    {
        String canonical = identifier;
        if( canonical.matches(".*/.*\\.\\d+") && canonical.lastIndexOf(DOT)!=-1)
        {
            canonical =  canonical.substring(0, canonical.lastIndexOf(DOT));
        }

        return canonical;
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
        String sql = "SELECT * FROM Handle WHERE resource_type_id = ? AND resource_id = ?";

        return DatabaseManager.querySingleTable(context, "Handle", sql, type, id);
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


    protected VersionHistory retrieveVersionHistory(Context c, Item item)
    {
        VersioningService versioningService = new DSpace().getSingletonService(VersioningService.class);
        return versioningService.findVersionHistory(c, item.getID());
    }

    protected void populateHandleMetadata(Item item)
            throws SQLException, IOException, AuthorizeException
    {
        String handleref = getCanonicalForm(getCanonical(item));

        // Add handle as identifier.uri DC value.
        // First check that identifier doesn't already exist.
        boolean identifierExists = false;
        DCValue[] identifiers = item.getDC("identifier", "uri", Item.ANY);
        for (DCValue identifier : identifiers)
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

    protected void modifyHandleMetadata(Item item, String handle)
            throws SQLException, IOException, AuthorizeException
    {
        String handleref = getCanonicalForm(handle);
        item.clearMetadata("dc", "identifier", "uri", Item.ANY);
        item.addDC("identifier", "uri", null, handleref);
        item.update();
    }


    protected VersionImpl createVersion(Context c, VersionHistory vh, Item item, String summary, Date date, int versionNumber) {
        try {
            VersionImpl version = versionDAO.create(c);

            // check if an equals versionNumber is already present in the DB (at this point it should never happen).
            if(vh!=null && vh.getVersions()!=null){
                for(Version v : vh.getVersions()){
                    if(v.getVersionNumber()==versionNumber){
                        throw new RuntimeException("A Version for this versionNumber is already present. Impossible complete the operation.");
                    }
                }
            }

            version.setVersionNumber(versionNumber);
            version.setVersionDate(date);
            version.setEperson(item.getSubmitter());
            version.setItemID(item.getID());
            version.setSummary(summary);
            version.setVersionHistory(vh.getVersionHistoryId());
            versionDAO.update(version);
            return version;
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    protected int getNextVersionNumer(Version latest){
        if(latest==null) return 1;

        return latest.getVersionNumber()+1;
    }

    public void setVersionDAO(VersionDAO versionDAO)
    {
        this.versionDAO = versionDAO;
    }

    public void setVersionHistoryDAO(VersionHistoryDAO versionHistoryDAO)
    {
        this.versionHistoryDAO = versionHistoryDAO;
    }
}
