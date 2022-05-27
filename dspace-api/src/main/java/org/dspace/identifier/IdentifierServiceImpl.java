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

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.core.Context;
import org.dspace.handle.service.HandleService;
import org.dspace.identifier.service.IdentifierService;
import org.springframework.beans.factory.annotation.Autowired;

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
     * log4j category
     */
    private static final Logger log = LogManager.getLogger(IdentifierServiceImpl.class);

    @Autowired(required = true)
    protected ContentServiceFactory contentServiceFactory;
    @Autowired(required = true)
    protected HandleService handleService;

    protected IdentifierServiceImpl() {

    }

    @Autowired(required = true)
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
     */
    @Override
    public void reserve(Context context, DSpaceObject dso)
        throws AuthorizeException, SQLException, IdentifierException {
        for (IdentifierProvider service : providers) {
            try {
                String identifier = service.mint(context, dso);
                if (!StringUtils.isEmpty(identifier)) {
                    service.reserve(context, dso, identifier);
                }
            } catch (IdentifierNotApplicableException e) {
                log.warn("Identifier not reserved (inapplicable): " + e.getMessage());
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
            if (service.supports(identifier)) {
                try {
                    service.reserve(context, dso, identifier);
                } catch (IdentifierNotApplicableException e) {
                    log.warn("Identifier not reserved (inapplicable): " + e.getMessage());
                }
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
            try {
                service.register(context, dso);
            } catch (IdentifierNotApplicableException e) {
                log.warn("Identifier not registered (inapplicable): " + e.getMessage());
            }
        }
        //Update our item / collection / community
        contentServiceFactory.getDSpaceObjectService(dso).update(context, dso);
    }

    @Override
    public void register(Context context, DSpaceObject object, String identifier)
        throws AuthorizeException, SQLException, IdentifierException {
        //We need to commit our context because one of the providers might require the handle created above
        // Next resolve all other services
        boolean registered = false;
        for (IdentifierProvider service : providers) {
            if (service.supports(identifier)) {
                try {
                    service.register(context, object, identifier);
                    registered = true;
                } catch (IdentifierNotApplicableException e) {
                    log.warn("Identifier not registered (inapplicable): " + e.getMessage());
                }
            }
        }
        if (!registered) {
            throw new IdentifierException("Cannot register identifier: Didn't "
                                              + "find a provider that supports this identifier.");
        }
        //Update our item / collection / community
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
                } catch (IdentifierException e) {
                    log.error(e);
                }
            }
        }
        return null;
    }

    @Override
    public List<String> lookup(Context context, DSpaceObject dso) {
        List<String> identifiers = new ArrayList<>();
        // Attempt to lookup DSO's identifiers using every available provider
        // TODO: We may want to eventually limit providers based on DSO type, as not every provider supports every DSO
        for (IdentifierProvider service : providers) {
            try {
                String result = service.lookup(context, dso);
                if (!StringUtils.isEmpty(result)) {
                    if (log.isDebugEnabled()) {
                        try {
                            log.debug("Got an identifier from " + service.getClass().getCanonicalName() + ".");
                        } catch (NullPointerException ex) {
                            log.debug(ex.getMessage(), ex);
                        }
                    }

                    identifiers.add(result);
                }
            } catch (IdentifierNotFoundException ex) {
                // This IdentifierNotFoundException is NOT logged by default, as some providers do not apply to
                // every DSO (e.g. DOIs usually don't apply to EPerson objects). So it is expected some may fail lookup.
                log.debug(service.getClass().getName() + " doesn't find an "
                             + "Identifier for " + contentServiceFactory.getDSpaceObjectService(dso)
                                                                        .getTypeText(dso) + ", "
                             + dso.getID().toString() + ".");
            } catch (IdentifierException ex) {
                log.error(ex);
            }
        }

        try {
            String handle = dso.getHandle();
            if (!StringUtils.isEmpty(handle)) {
                if (!identifiers.contains(handle)
                        && !identifiers.contains("hdl:" + handle)
                        && !identifiers.contains(handleService.getCanonicalForm(handle))) {
                    // The VersionedHandleIdentifierProvider gets loaded by default
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
                } catch (IdentifierException ex) {
                    log.error(ex.getMessage(), ex);
                }
            }

        }
        return null;
    }

    @Override
    public void delete(Context context, DSpaceObject dso) throws AuthorizeException, SQLException, IdentifierException {
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
}
