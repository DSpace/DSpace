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
import org.dspace.content.service.ItemService;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.handle.service.HandleService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.versioning.*;
import org.dspace.versioning.service.VersionHistoryService;
import org.dspace.versioning.service.VersioningService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;

/**
 *
 *
 * @author Fabio Bolognesi (fabio at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Pascal-Nicolas Becker (dspace at pascal dash becker dot de)
 */
@Component
public class VersionedHandleIdentifierProvider extends IdentifierProvider {
    /** log4j category */
    private static Logger log = Logger.getLogger(VersionedHandleIdentifierProvider.class);

    /** Prefix registered to no one */
    static final String EXAMPLE_PREFIX = "123456789";

    private static final char DOT = '.';

    @Autowired(required = true)
    private VersioningService versionService;

    @Autowired(required = true)
    private VersionHistoryService versionHistoryService;

    @Autowired(required = true)
    private HandleService handleService;

    @Autowired(required = true)
    private ItemService itemService;

    @Override
    public boolean supports(Class<? extends Identifier> identifier)
    {
        return Handle.class.isAssignableFrom(identifier);
    }

    @Override
    public boolean supports(String identifier)
    {
    	String prefix = handleService.getPrefix();
        String canonicalPrefix = DSpaceServicesFactory.getInstance().getConfigurationService().getProperty("handle.canonical.prefix");
        if (identifier == null)
        {
            return false;
        }
        // return true if handle has valid starting pattern
        if (identifier.startsWith(prefix + "/")
                || identifier.startsWith(canonicalPrefix)
                || identifier.startsWith("hdl:")
                || identifier.startsWith("info:hdl")
                || identifier.matches("^https?://hdl\\.handle\\.net/.*")
                || identifier.matches("^https?://.+/handle/.*"))
        {
            return true;
        }

        //Check additional prefixes supported in the config file
        String[] additionalPrefixes = DSpaceServicesFactory.getInstance().getConfigurationService().getArrayProperty("handle.additional.prefixes");
        for(String additionalPrefix: additionalPrefixes) {
            if (identifier.startsWith(additionalPrefix + "/")) {
                return true;
            }
        }

        // otherwise, assume invalid handle
        return false;
    }

    @Override
    public String register(Context context, DSpaceObject dso)
    {
        String id = mint(context, dso);
        try
        {
            if (dso instanceof Item)
            {
                populateHandleMetadata(context, (Item) dso, id);
            }
        }catch (Exception e){
            log.error(LogManager.getHeader(context, "Error while attempting to create handle", "Item id: " + (dso != null ? dso.getID() : "")), e);
            throw new RuntimeException("Error while attempting to create identifier for Item id: " + (dso != null ? dso.getID() : ""));
        }
        return id;
    }

    @Override
    public void register(Context context, DSpaceObject dso, String identifier)
            throws IdentifierException
    {
        if (dso instanceof Item && identifier != null)
        {
            Item item = (Item) dso;

            // if identifier == 1234.5/100.4 reinstate the version 4 in the 
            // version table if absent
            
            
            Matcher versionHandleMatcher = Pattern.compile("^.*/.*\\.(\\d+)$").matcher(identifier);
            // do we have to register a versioned handle?
            if(versionHandleMatcher.matches())
            {
                // parse the version number from the handle
                int versionNumber = -1;
                try {
                    versionNumber = Integer.valueOf(versionHandleMatcher.group(1));
                } catch (NumberFormatException ex) {
                    throw new IllegalStateException("Cannot detect the interger value of a digit.", ex);
                }
                
                // get history
                VersionHistory history = null;
                try {
                    history = versionHistoryService.findByItem(context, item);
                } catch (SQLException ex) {
                    throw new RuntimeException("Unable to create handle '" 
                        + identifier + "' for " 
                        + Constants.typeText[dso.getType()] + " " + dso.getID() 
                        + " in cause of a problem with the database: ", ex);
                }
                
                // do we have a version history?
                if (history != null)
                {
                    // get the version
                    Version version = null;
                    try {
                        versionHistoryService.getVersion(context, history, item);
                    } catch (SQLException ex) {
                        throw new RuntimeException("Problem with the database connection occurd.", ex);
                    }
                    
                    // did we found a version?
                    if (version != null)
                    {
                        // do the version's number and the handle versionnumber match?
                        if (version.getVersionNumber() != versionNumber)
                        {
                            throw new IdentifierException("Trying to register a handle without matching its item's version number.");
                        }
                        
                        // create the handle
                        try {
                            handleService.createHandle(context, dso, identifier);
                            populateHandleMetadata(context, item, identifier);
                            return;
                        } catch (AuthorizeException ex) {
                            throw new IdentifierException("Current user does not "
                                    + "have the privileges to add the handle " 
                                    + identifier + " to the item's (" 
                                    + dso.getID() + ") metadata.", ex);
                        } catch (SQLException | IOException ex) {
                            throw new RuntimeException("Unable to create handle '"
                            + identifier + "' for "
                            + Constants.typeText[dso.getType()] + " " + dso.getID()
                            + ".", ex);
                        }
                    }
                } else {
                    try {
                        // either no VersionHistory or no Version exists.
                        // Restore item with the appropriate version number.
                        restoreItAsVersion(context, item, identifier, versionNumber);
                    } catch (SQLException | IOException ex) {
                        throw new RuntimeException("Unable to restore a versioned "
                                + "handle as there was a problem in creating a "
                                + "neccessary item version: ", ex);
                    } catch (AuthorizeException ex) {
                        throw new RuntimeException("Unable to restore a versioned "
                                + "handle as the current user was not allowed to "
                                + "create a neccessary item version: ", ex);
                    }
                    return;
                }
            }
        }
        try {
            // either we have a DSO not of type item or the handle was not a
            // versioned (e.g. 123456789/100) one
            // just register it.
            createNewIdentifier(context, dso, identifier);
            if (dso instanceof Item) {
                    populateHandleMetadata(context, (Item) dso, identifier);
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Unable to create handle '" 
                            + identifier + "' for " 
                            + Constants.typeText[dso.getType()] + " " + dso.getID() 
                            + " in cause of a problem with the database: ", ex);
        } catch (AuthorizeException ex) {
                    throw new IdentifierException("Current user does not "
                            + "have the privileges to add the handle " 
                            + identifier + " to the item's (" 
                            + dso.getID() + ") metadata.", ex);
        } catch (IOException ex) {
            throw new RuntimeException("Unable add the handle '"
            + identifier + "' for "
            + Constants.typeText[dso.getType()] + " " + dso.getID()
            + " in the object's metadata.", ex);
        }
    }

    // get VersionHistory by handle
    protected VersionHistory getHistory(Context context, String identifier) throws SQLException {
        DSpaceObject item = this.resolve(context, identifier);
        if(item!=null){
            VersionHistory history = versionHistoryService.findByItem(context, (Item) item);
            return history;
        }
        return null;
    }

    protected void restoreItAsVersion(Context context, Item item, String identifier, int versionNumber)
            throws SQLException, AuthorizeException, IOException
    {
        createNewIdentifier(context, item, identifier);
        populateHandleMetadata(context, item, identifier);
        
        VersionHistory vh = versionHistoryService.findByItem(context, item);
        if (vh == null)
        {
            vh = versionHistoryService.create(context);
        }
        Version version = versionHistoryService.getVersion(context, vh, item);
        if (version == null)
        {
            version = versionService.createNewVersion(context, vh, item, "Restoring from AIP Service", new Date(), versionNumber);
        }
        versionHistoryService.update(context, vh);
    }

    @Override
    public void reserve(Context context, DSpaceObject dso, String identifier)
    {
        try{
            handleService.createHandle(context, dso, identifier);
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
    @Override
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
                history = versionHistoryService.findByItem(context, (Item) dso);
            }

            if(history!=null)
            {
                handleId = makeIdentifierBasedOnHistory(context, dso, history);
            }else{
                handleId = createNewIdentifier(context, dso, null);
            }
            return handleId;
        }catch (Exception e){
            log.error(LogManager.getHeader(context, "Error while attempting to create handle", "Item id: " + dso.getID()), e);
            throw new RuntimeException("Error while attempting to create identifier for Item id: " + dso.getID());
        }
    }

    @Override
    public DSpaceObject resolve(Context context, String identifier, String... attributes)
    {
        // We can do nothing with this, return null
        try{
            return handleService.resolveToObject(context, identifier);
        }catch (Exception e){
            log.error(LogManager.getHeader(context, "Error while resolving handle to item", "handle: " + identifier), e);
        }
        return null;
    }

    @Override
    public String lookup(Context context, DSpaceObject dso) throws IdentifierNotFoundException, IdentifierNotResolvableException {

        try
        {
            return handleService.findHandle(context, dso);
        }catch(SQLException sqe){
            throw new IdentifierNotResolvableException(sqe.getMessage(),sqe);
        }
    }

    @Override
    public void delete(Context context, DSpaceObject dso, String identifier) throws IdentifierException {
        delete(context, dso);
    }

    @Override
    public void delete(Context context, DSpaceObject dso) throws IdentifierException {
        try{
            handleService.unbindHandle(context, dso);
        } catch(SQLException sqe) {
            throw new RuntimeException(sqe.getMessage(),sqe);
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
        if(handleId == null)
        {
            return handleService.createHandle(context, dso);
        }else{
            return handleService.createHandle(context,  dso, handleId);
        }
    }

    protected String makeIdentifierBasedOnHistory(Context context, DSpaceObject dso, VersionHistory history) throws AuthorizeException, SQLException
    {
        if (!(dso instanceof Item))
        {
            throw new IllegalStateException("Cannot create versioned handle for "
                    + "objects other then item: Currently versioning supports "
                    + "items only.");
        }
        Item item = (Item)dso;
        
        // The first version will have a handle like 12345/100 to be backward compatible
        // to DSpace installation that started without versioning.
        // Mint foreach new VERSION an identifier like: 12345/100.versionNumber.
        
        Version version = versionService.getVersion(context, item);
        Version firstVersion = versionHistoryService.getFirstVersion(context, history);

        String bareHandle = firstVersion.getItem().getHandle();
        if(bareHandle.matches(".*/.*\\.\\d+"))
        {
            bareHandle = bareHandle.substring(0, bareHandle.lastIndexOf(DOT));
        }

        // add a new Identifier for new item: 12345/100.x
        int versionNumber = version.getVersionNumber();
        String identifier = bareHandle;

        if (versionNumber > 1)
        {
            identifier = identifier.concat(String.valueOf(DOT)).concat(String.valueOf(versionNumber));
        }

        // Ensure this handle does not exist already.
        if (handleService.resolveToObject(context, identifier) == null)
        {
             handleService.createHandle(context, dso, identifier);
        }
        else
        {
            throw new IllegalStateException("A versioned handle is used for another version already!");
        }
        return identifier;
    }
    
    protected void populateHandleMetadata(Context context, Item item, String handle)
            throws SQLException, IOException, AuthorizeException
    {
        String handleref = getCanonicalForm(handle);
        // we want to remove the old handle and insert the new. To do so, we 
        // load all identifiers, clear the metadata field, re add all 
        // identifiers which are not from type handle and add the new handle.
        List<MetadataValue> identifiers = itemService.getMetadata(item, 
                MetadataSchema.DC_SCHEMA, "identifier", "uri", Item.ANY);
        itemService.clearMetadata(context, item, MetadataSchema.DC_SCHEMA, 
                "identifier", "uri", Item.ANY);
        for (MetadataValue identifier : identifiers)
        {
            if (this.supports(identifier.getValue()))
            {
                // ignore handles
                log.debug("Removing identifier " + identifier.getValue());
                continue;
            }
            log.debug("Preserving identifier " + identifier.getValue());
            itemService.addMetadata(context, 
                    item,
                    identifier.getMetadataField(),
                    identifier.getLanguage(),
                    identifier.getValue(),
                    identifier.getAuthority(),
                    identifier.getConfidence());
        }
        
        // Add handle as identifier.uri DC value.
        if (StringUtils.isNotBlank(handleref))
        {
            itemService.addMetadata(context, item, MetadataSchema.DC_SCHEMA, "identifier", "uri", null, handleref);
        }
        itemService.update(context, item);
    }
}
