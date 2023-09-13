/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.identifier;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import org.apache.logging.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.MetadataSchemaEnum;
import org.dspace.content.MetadataValue;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.DSpaceObjectService;
import org.dspace.core.Context;
import org.dspace.core.LogHelper;
import org.dspace.handle.service.HandleService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * The old DSpace handle identifier service, used to create handles or retrieve objects based on their handle
 *
 * @author Fabio Bolognesi (fabio at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 */
@Component
public class HandleIdentifierProvider extends IdentifierProvider {
    /**
     * log4j category
     */
    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(HandleIdentifierProvider.class);

    /**
     * Prefix registered to no one
     */
    protected static final String EXAMPLE_PREFIX = "123456789";

    @Autowired(required = true)
    protected HandleService handleService;
    @Autowired(required = true)
    protected ContentServiceFactory contentServiceFactory;

    @Override
    public boolean supports(Class<? extends Identifier> identifier) {
        return Handle.class.isAssignableFrom(identifier);
    }

    @Override
    public boolean supports(String identifier) {
        return handleService.parseHandle(identifier) != null;
    }

    @Override
    public String register(Context context, DSpaceObject dso) {
        try {
            String id = mint(context, dso);

            // move canonical to point the latest version
            if (dso instanceof Item || dso instanceof Collection || dso instanceof Community) {
                Item item = (Item) dso;
                populateHandleMetadata(context, item, id);
            }

            return id;
        } catch (IOException | SQLException | AuthorizeException e) {
            log.error(LogHelper.getHeader(context,
                    "Error while attempting to create handle",
                    "Item id: " + dso.getID()), e);
            throw new RuntimeException("Error while attempting to create identifier for Item id: " + dso.getID(), e);
        }
    }

    @Override
    public void register(Context context, DSpaceObject dso, String identifier) {
        try {
            handleService.createHandle(context, dso, identifier);
            if (dso instanceof Item || dso instanceof Collection || dso instanceof Community) {
                populateHandleMetadata(context, dso, identifier);
            }
        } catch (IOException | IllegalStateException | SQLException | AuthorizeException e) {
            log.error(LogHelper.getHeader(context,
                    "Error while attempting to create handle",
                    "Item id: " + dso.getID()), e);
            throw new RuntimeException("Error while attempting to create identifier for Item id: " + dso.getID(), e);
        }
    }


    @Override
    public void reserve(Context context, DSpaceObject dso, String identifier) {
        try {
            handleService.createHandle(context, dso, identifier);
        } catch (IllegalStateException | SQLException e) {
            log.error(LogHelper.getHeader(context,
                    "Error while attempting to create handle",
                    "Item id: " + dso.getID()), e);
            throw new RuntimeException("Error while attempting to create identifier for Item id: " + dso.getID());
        }
    }


    /**
     * Creates a new handle in the database.
     *
     * @param context DSpace context
     * @param dso     The DSpaceObject to create a handle for
     * @return The newly created handle
     */
    @Override
    public String mint(Context context, DSpaceObject dso) {
        if (dso.getHandle() != null) {
            return dso.getHandle();
        }

        try {
            return handleService.createHandle(context, dso);
        } catch (SQLException e) {
            log.error(LogHelper.getHeader(context,
                    "Error while attempting to create handle",
                    "Item id: " + dso.getID()), e);
            throw new RuntimeException("Error while attempting to create identifier for Item id: " + dso.getID());
        }
    }

    @Override
    public DSpaceObject resolve(Context context, String identifier, String... attributes) {
        // We can do nothing with this, return null
        try {
            identifier = handleService.parseHandle(identifier);
            return handleService.resolveToObject(context, identifier);
        } catch (IllegalStateException | SQLException e) {
            log.error(LogHelper.getHeader(context, "Error while resolving handle to item", "handle: " + identifier),
                      e);
        }
//        throw new IllegalStateException("Unsupported Handle Type "
//                + Constants.typeText[handletypeid]);
        return null;
    }

    @Override
    public String lookup(Context context, DSpaceObject dso)
        throws IdentifierNotFoundException, IdentifierNotResolvableException {

        try {
            return handleService.findHandle(context, dso);
        } catch (SQLException sqe) {
            throw new IdentifierNotResolvableException(sqe.getMessage(), sqe);
        }
    }

    @Override
    public void delete(Context context, DSpaceObject dso, String identifier) throws IdentifierException {
        delete(context, dso);
    }

    @Override
    public void delete(Context context, DSpaceObject dso) throws IdentifierException {
        try {
            handleService.unbindHandle(context, dso);
        } catch (SQLException sqe) {
            throw new IdentifierException(sqe.getMessage(), sqe);
        }

    }

    public static String retrieveHandleOutOfUrl(String url)
        throws SQLException {
        // We can do nothing with this, return null
        if (!url.contains("/")) {
            return null;
        }

        String[] splitUrl = url.split("/");

        return splitUrl[splitUrl.length - 2] + "/" + splitUrl[splitUrl.length - 1];
    }

    /**
     * Get the configured Handle prefix string, or a default
     *
     * @return configured prefix or "123456789"
     */
    public static String getPrefix() {
        ConfigurationService configurationService
                = DSpaceServicesFactory.getInstance().getConfigurationService();
        String prefix = configurationService.getProperty("handle.prefix");
        if (null == prefix) {
            prefix = EXAMPLE_PREFIX; // XXX no good way to exit cleanly
            log.error("handle.prefix is not configured; using " + prefix);
        }
        return prefix;
    }

    protected void populateHandleMetadata(Context context, DSpaceObject dso, String handle)
            throws SQLException, IOException, AuthorizeException {
        String handleref = handleService.getCanonicalForm(handle);

        DSpaceObjectService<DSpaceObject> dsoService = contentServiceFactory.getDSpaceObjectService(dso);

        // Add handle as identifier.uri DC value.
        // First check that identifier doesn't already exist.
        boolean identifierExists = false;
        List<MetadataValue> identifiers = dsoService
                .getMetadata(dso, MetadataSchemaEnum.DC.getName(), "identifier", "uri", Item.ANY);
        for (MetadataValue identifier : identifiers) {
            if (handleref.equals(identifier.getValue())) {
                identifierExists = true;
            }
        }
        if (!identifierExists) {
            dsoService.addMetadata(context, dso, MetadataSchemaEnum.DC.getName(),
                    "identifier", "uri", null, handleref);
        }
    }
}
