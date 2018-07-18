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

@Component
public class UUIDIdentifierProvider extends IdentifierProvider {

    private static Logger log = Logger.getLogger(UUIDIdentifierProvider.class);

    @Autowired
    private ContentServiceFactory contentServiceFactory;

    public boolean supports(Class<?> identifier) {
        return UUID.class.isAssignableFrom(identifier);
    }

    public boolean supports(String identifier) {
        return (UUIDUtils.fromString(identifier) != null);
    }

    public String register(Context context, DSpaceObject item) throws IdentifierException {
        log.trace("Register method of the UUIDIdentifierProvider");
        return null;
    }

    public String mint(Context context, DSpaceObject dso) throws IdentifierException {
        log.trace("Mint method of the UUIDIdentifierProvider");
        return null;
    }

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

    public String lookup(Context context, DSpaceObject object)
        throws IdentifierNotFoundException, IdentifierNotResolvableException {
        return object == null || object.getID() == null ? null : object.getID().toString();
    }


    public void delete(Context context, DSpaceObject dso) throws IdentifierException {
        log.trace("Delete method of the UUIDIdentifierProvider");
    }

    public void delete(Context context, DSpaceObject dso, String identifier) throws IdentifierException {
        log.trace("Delete method of the UUIDIdentifierProvider");
    }

    public void reserve(Context context, DSpaceObject dso, String identifier) throws IdentifierException {
        log.trace("Reserve method of the UUIDIdentifierProvider");
    }

    public void register(Context context, DSpaceObject object, String identifier) throws IdentifierException {
        log.trace("Register method of the UUIDIdentifierProvider");
    }
}
