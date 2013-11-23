/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xoai.services.impl.database;


import org.dspace.content.DSpaceObject;
import org.dspace.handle.HandleManager;
import org.dspace.xoai.services.api.context.ContextService;
import org.dspace.xoai.services.api.context.ContextServiceException;
import org.dspace.xoai.services.api.database.HandleResolver;
import org.dspace.xoai.services.api.database.HandleResolverException;
import org.springframework.beans.factory.annotation.Autowired;

import java.sql.SQLException;

public class DSpaceHandlerResolver implements HandleResolver {
    @Autowired
    private ContextService contextService;

    @Override
    public DSpaceObject resolve(String handle) throws HandleResolverException {
        try {
            return HandleManager.resolveToObject(contextService.getContext(), handle);
        } catch (ContextServiceException e) {
            throw new HandleResolverException(e);
        } catch (SQLException e) {
            throw new HandleResolverException(e);
        }
    }

    @Override
    public String getHandle(DSpaceObject object) throws HandleResolverException {
        try {
            return HandleManager.findHandle(contextService.getContext(), object);
        } catch (SQLException e) {
            throw new HandleResolverException(e);
        } catch (ContextServiceException e) {
            throw new HandleResolverException(e);
        }
    }
}
