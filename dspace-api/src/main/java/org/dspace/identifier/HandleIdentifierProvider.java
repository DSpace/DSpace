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
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.handle.service.HandleService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

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

    @Autowired(required = true)
    protected HandleService handleService;
    @Autowired(required = true)
    protected ItemService itemService;

    @Override
    public boolean supports(Class<? extends Identifier> identifier) {
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

        return false;
    }

    @Override
    public String register(Context context, DSpaceObject dso) {
        try{
            String id = mint(context, dso);

            // move canonical to point the latest version
            if(dso instanceof Item)
            {
                Item item = (Item)dso;
                populateHandleMetadata(context, item, id);
            }

            return id;
        }catch (Exception e){
            log.error(LogManager.getHeader(context, "Error while attempting to create handle", "Item id: " + dso.getID()), e);
            throw new RuntimeException("Error while attempting to create identifier for Item id: " + dso.getID(), e);
        }
    }

    @Override
    public void register(Context context, DSpaceObject dso, String identifier) {
        try{
            handleService.createHandle(context, dso, identifier);
            if(dso instanceof Item)
            {
                Item item = (Item)dso;
                populateHandleMetadata(context, item, identifier);
            }
        }catch (Exception e){
            log.error(LogManager.getHeader(context, "Error while attempting to create handle", "Item id: " + dso.getID()), e);
            throw new RuntimeException("Error while attempting to create identifier for Item id: " + dso.getID(), e);
        }
    }


    @Override
    public void reserve(Context context, DSpaceObject dso, String identifier) {
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
     * @exception java.sql.SQLException If a database error occurs
     */
    @Override
    public String mint(Context context, DSpaceObject dso) {
        if(dso.getHandle() != null)
        {
            return dso.getHandle();
        }

        try{
            return handleService.createHandle(context, dso);
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
            return handleService.resolveToObject(context, identifier);
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

    protected void populateHandleMetadata(Context context, Item item, String handle)
            throws SQLException, IOException, AuthorizeException
    {
        String handleref = handleService.getCanonicalForm(handle);

        // Add handle as identifier.uri DC value.
        // First check that identifier doesn't already exist.
        boolean identifierExists = false;
        List<MetadataValue> identifiers = itemService.getMetadata(item, MetadataSchema.DC_SCHEMA, "identifier", "uri", Item.ANY);
        for (MetadataValue identifier : identifiers)
        {
            if (handleref.equals(identifier.getValue()))
            {
                identifierExists = true;
            }
        }
        if (!identifierExists)
        {
            itemService.addMetadata(context, item, MetadataSchema.DC_SCHEMA, "identifier", "uri", null, handleref);
        }
    }
}
