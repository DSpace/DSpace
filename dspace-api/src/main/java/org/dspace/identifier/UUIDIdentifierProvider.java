/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.identifier;

import java.sql.SQLException;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.dspace.content.DSpaceObject;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.DSpaceObjectService;
import org.dspace.core.Context;
import org.dspace.util.UUIDUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * This is a new IdentifierProvider that will be able to resolve the UUID of any existing DSpaceObject to it's proper
 * object. This class will loop over every DSpaceObjectService (e.g. CollectionService) and try to find the UUID by
 * doing a lookup.
 * Because of this, using this IdentifierProvider too often may cause in performance issues when dealing with large
 * database, therefore it is recommended to keep the usage of this to a minimum and only use it when absolutely
 * necessary.
 */
@Component
public class UUIDIdentifierProvider extends IdentifierProvider {

    private static Logger log = Logger.getLogger(UUIDIdentifierProvider.class);

    @Autowired
    private ContentServiceFactory contentServiceFactory;

    /**
     * Please see {@link IdentifierProvider#supports(Class)} for full documentation
     */
    public boolean supports(Class<?> identifier) {
        return UUID.class.isAssignableFrom(identifier);
    }

    /**
     * Please see {@link IdentifierProvider#supports(String)} for full documentation
     */
    public boolean supports(String identifier) {
        return (UUIDUtils.fromString(identifier) != null);
    }

    /**
     * Please see {@link IdentifierProvider#register(Context, DSpaceObject)} for full documentation
     */
    public String register(Context context, DSpaceObject item) throws IdentifierException {
        log.trace("Register method of the UUIDIdentifierProvider");
        return null;
    }

    /**
     * Please see {@link IdentifierProvider#mint(Context, DSpaceObject)} for full documentation
     */
    public String mint(Context context, DSpaceObject dso) throws IdentifierException {
        log.trace("Mint method of the UUIDIdentifierProvider");
        return null;
    }

    /**
     * This method resolves the given Identifier to a DSpaceObject by searching across all the DSpaceObjectServices.
     * Note that this method can be very resource intensive on your DSpace Application and cause performance issues
     * when used
     * a lot with bigger databases. Therefore we recommend you to only use this method when absolutely necessary.
     *
     * @param context    The relevant DSpace Context.
     * @param identifier to be resolved.
     * @param attributes additional information for resolving {@code identifier}.
     * @return The DSpaceObject that has this UUID as an identifier.
     * @throws IdentifierNotFoundException
     * @throws IdentifierNotResolvableException
     */
    public DSpaceObject resolve(Context context, String identifier, String... attributes)
        throws IdentifierNotFoundException, IdentifierNotResolvableException {

        UUID uuid = UUIDUtils.fromString(identifier);

        if (uuid != null) {
            for (DSpaceObjectService dSpaceObjectService : contentServiceFactory.getDSpaceObjectServices()) {
                try {
                    DSpaceObject dSpaceObject = dSpaceObjectService.find(context, uuid);
                    if (dSpaceObject != null) {
                        return dSpaceObject;
                    }
                } catch (SQLException e) {
                    log.error(e, e);
                }
            }
        }

        return null;
    }

    /**
     * Please see {@link IdentifierProvider#lookup(Context, DSpaceObject)} for full documentation
     */
    public String lookup(Context context, DSpaceObject object)
        throws IdentifierNotFoundException, IdentifierNotResolvableException {
        return object == null || object.getID() == null ? null : object.getID().toString();
    }


    /**
     * Not applicable for this UUIDIdentifierProvider
     */
    public void delete(Context context, DSpaceObject dso) throws IdentifierException {
        log.trace("Delete method of the UUIDIdentifierProvider");
    }

    /**
     * Not applicable for this UUIDIdentifierProvider
     */
    public void delete(Context context, DSpaceObject dso, String identifier) throws IdentifierException {
        log.trace("Delete method of the UUIDIdentifierProvider");
    }

    /**
     * Not applicable for this UUIDIdentifierProvider
     */
    public void reserve(Context context, DSpaceObject dso, String identifier) throws IdentifierException {
        log.trace("Reserve method of the UUIDIdentifierProvider");
    }

    /**
     * Not applicable for this UUIDIdentifierProvider
     */
    public void register(Context context, DSpaceObject object, String identifier) throws IdentifierException {
        log.trace("Register method of the UUIDIdentifierProvider");
    }
}
