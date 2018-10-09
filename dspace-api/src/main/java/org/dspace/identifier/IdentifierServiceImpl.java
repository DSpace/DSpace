/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.identifier;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.core.Context;
import org.dspace.handle.service.HandleService;
import org.dspace.identifier.service.IdentifierService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;

/**
 * The main service class used to reserve, register and resolve identifiers
 *
 * @author Fabio Bolognesi (fabio at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 */
public class IdentifierServiceImpl implements IdentifierService {

    private List<IdentifierProvider> providers;

    /**
     * logging category
     */
    private static final Logger log
            = LoggerFactory.getLogger(IdentifierServiceImpl.class);

    @Autowired(required = true)
    protected ContentServiceFactory contentServiceFactory;
    @Autowired(required = true)
    protected HandleService handleService;

    protected IdentifierServiceImpl() {

    }

    /**
     * Collects all configured IdentifierProvider implementations.
     * @param providers the configured identifier providers.
     */
    @Autowired
    @Required
    public void setProviders(List<IdentifierProvider> providers) {
        this.providers = providers;

        for (IdentifierProvider p : providers) {
            p.setParentService(this);
        }
    }

    /**
     * Reserves identifiers for the item
     *
     * @param context dspace context
     * @param dso     dspace object
     * @throws org.dspace.authorize.AuthorizeException
     * @throws java.sql.SQLException
     * @throws org.dspace.identifier.IdentifierException
     */
    @Override
    public void reserve(Context context, DSpaceObject dso)
        throws AuthorizeException, SQLException, IdentifierException {
        for (IdentifierProvider service : providers) {
            if (isProviderEnabled(dso, service)) {
                String identifier = service.mint(context, dso);
                if (!StringUtils.isEmpty(identifier)) {
                    service.reserve(context, dso, identifier);
                }
            }
        }
        //Update our item
        contentServiceFactory.getDSpaceObjectService(dso).update(context, dso);
    }

    @Override
    public void reserve(Context context, DSpaceObject dso, String identifier)
        throws AuthorizeException, SQLException, IdentifierException {
        // Next resolve all other services
        for (IdentifierProvider service : providers) {
            if (service.supports(identifier) && isProviderEnabled(dso, service)) {
                service.reserve(context, dso, identifier);
            }
        }
        //Update our item
        contentServiceFactory.getDSpaceObjectService(dso).update(context, dso);
    }

    @Override
    public void register(Context context, DSpaceObject dso)
        throws AuthorizeException, SQLException, IdentifierException {
        //We need to commit our context because one of the providers might require the handle created above
        // Next resolve all other services
        for (IdentifierProvider service : providers) {
            if (isProviderEnabled(dso, service)) {
                service.register(context, dso);
            }
        }
        //Update our item
        contentServiceFactory.getDSpaceObjectService(dso).update(context, dso);
    }

    @Override
    public void register(Context context, DSpaceObject object, String identifier)
        throws AuthorizeException, SQLException, IdentifierException {
        //We need to commit our context because one of the providers might require the handle created above
        // Next resolve all other services
        boolean registered = false;
        for (IdentifierProvider service : providers) {
            if (service.supports(identifier) && isProviderEnabled(object, service)) {
                service.register(context, object, identifier);
                registered = true;
            }
        }
        if (!registered) {
            throw new IdentifierException("Cannot register identifier: Didn't "
                                              + "find a provider that supports this identifier.");
        }
        //Update our item
        contentServiceFactory.getDSpaceObjectService(object).update(context, object);
    }

    @Override
    public String lookup(Context context, DSpaceObject dso, Class<? extends Identifier> identifier) {
        for (IdentifierProvider service : providers) {
            if (service.supports(identifier)) {
                try {
                    String result = service.lookup(context, dso);
                    if (result != null) {
                        return result;
                    }
                } catch (IdentifierNotFoundException ex) {
                    log.info(service.getClass().getName() + " doesn't find an "
                                 + "Identifier for " + contentServiceFactory.getDSpaceObjectService(dso)
                                                                            .getTypeText(dso) + ", "
                                 + dso.getID().toString() + ".");
                    log.debug(ex.getMessage(), ex);
                } catch (IdentifierException e) {
                    log.error(e.getMessage(), e);
                }
            }
        }
        return null;
    }

    @Override
    public List<String> lookup(Context context, DSpaceObject dso) {
        List<String> identifiers = new ArrayList<>();
        for (IdentifierProvider service : providers) {
            try {
                String result = service.lookup(context, dso);
                if (!StringUtils.isEmpty(result)) {
                    if (log.isDebugEnabled()) {
                        try {
                            log.debug("Got an identifier from "
                                          + service.getClass().getCanonicalName() + ".");
                        } catch (NullPointerException ex) {
                            log.debug(ex.getMessage(), ex);
                        }
                    }

                    identifiers.add(result);
                }
            } catch (IdentifierNotFoundException ex) {
                log.info(service.getClass().getName() + " doesn't find an "
                             + "Identifier for " + contentServiceFactory.getDSpaceObjectService(dso)
                                                                        .getTypeText(dso) + ", "
                             + dso.getID().toString() + ".");
                log.debug(ex.getMessage(), ex);
            } catch (IdentifierException ex) {
                log.error(ex.getMessage(), ex);
            }
        }

        try {
            String handle = dso.getHandle();
            if (!StringUtils.isEmpty(handle)) {
                if (!identifiers.contains(handle)
                    && !identifiers.contains("hdl:" + handle)
                    && !identifiers.contains(handleService.getCanonicalForm(handle))) {
                    // The VerionedHandleIdentifierProvider gets loaded by default
                    // it returns handles without any scheme (neither hdl: nor http:).
                    // If the VersionedHandleIdentifierProvider is not loaded,
                    // we adds the handle in way it would.
                    // Generally it would be better if identifiers would be added
                    // here in a way they could be recognized.
                    log.info("Adding handle '" + handle + "' to the "
                                 + "array of looked up identifiers.");
                    identifiers.add(handle);
                }
            }
        } catch (Exception ex) {
            // nothing is expected here, but if an exception is thrown it
            // should not stop everything running.
            log.error(ex.getMessage(), ex);
        }

        log.debug("Found identifiers: " + identifiers.toString());
        return identifiers;
    }

    @Override
    public DSpaceObject resolve(Context context, String identifier)
        throws IdentifierNotFoundException, IdentifierNotResolvableException {
        for (IdentifierProvider service : providers) {
            if (service.supports(identifier)) {
                try {
                    DSpaceObject result = service.resolve(context, identifier);
                    if (result != null) {
                        return result;
                    }
                } catch (IdentifierNotFoundException ex) {
                    log.info(service.getClass().getName() + " cannot resolve "
                                 + "Identifier " + identifier + ": identifier not "
                                 + "found.");
                    log.debug(ex.getMessage(), ex);
                } catch (IdentifierException ex) {
                    log.error(ex.getMessage(), ex);
                }
            }

        }
        return null;
    }

    @Override
    public void delete(Context context, DSpaceObject dso)
        throws AuthorizeException, SQLException, IdentifierException {
        for (IdentifierProvider service : providers) {
            try {
                service.delete(context, dso);
            } catch (IdentifierException e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    @Override
    public void delete(Context context, DSpaceObject dso, String identifier)
        throws AuthorizeException, SQLException, IdentifierException {
        for (IdentifierProvider service : providers) {
            try {
                if (service.supports(identifier)) {
                    service.delete(context, dso, identifier);
                }
            } catch (IdentifierException e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    /**
     * Test whether an identifier provider been disabled in the container of
     * this object.  If the object is not an Item, providers cannot be disabled.
     * Otherwise, if *all* containers are marked as disabling generation of this
     * provider's type of identifier, then generation is disabled.  A container
     * is marked to disable a provider's identifier type if it has a metadata
     * field 'local.identifier_generation_disabled.TYPE' where TYPE is the
     * {@link IdentifierProvider#getIdentifierTypeName() "type name"} of the
     * provider and the field's first value is "true".
     *
     * @return false if this provider is disabled.
     */
    private boolean isProviderEnabled(DSpaceObject dso, IdentifierProvider provider)
            throws SQLException {
        if (!(dso instanceof Item)) {
            return true;
        }

        Item item = (Item)dso;
        String providerTypeName = provider.getIdentifierTypeName();
        log.debug("Is provider {} enabled for Item {}?",
                providerTypeName, dso.getID());

        List<Collection> collections = item.getCollections();
        log.debug("Item is in {} collections", collections.size());
        // Provider is enabled if there are no collections to disable it
        if (collections.isEmpty()) {
            log.debug("Returning true");
            return true;
        }

        boolean disabled = true;
        for (Collection collection : collections) {
            log.debug("Checking Collection {} {}",
                    collection.getID(), collection.getName());
            CollectionService collectionService = contentServiceFactory.getCollectionService();
            List<MetadataValue> flagValues = collectionService.getMetadata(collection, "local",
                    "identifier_generation_disabled", providerTypeName, Item.ANY);
            if (!flagValues.isEmpty()) {
                MetadataValue flag = flagValues.get(0);
                log.debug("local.identifier_generation_disabled[0] = {}", flag);
                disabled &= Boolean.parseBoolean(flag.getValue());
            } else {
                log.debug("local.identifier_generation_disabled is not set");
                disabled = false;
            }
            log.debug("disabled = {}", disabled);
        }

        log.debug("Returning {}", !disabled);
        return !disabled;
    }
}
