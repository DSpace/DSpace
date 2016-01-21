/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xoai.services.impl;

import java.sql.SQLException;
import javax.inject.Inject;
import org.dspace.content.DSpaceObject;
import org.dspace.handle.factory.HandleServiceFactory;
import org.dspace.handle.service.HandleService;
import org.dspace.xoai.services.api.context.ContextService;
import org.dspace.xoai.services.api.context.ContextServiceException;
import org.dspace.xoai.services.api.HandleResolver;
import org.dspace.xoai.services.api.HandleResolverException;

public class DSpaceHandleResolver implements HandleResolver {
    @Inject
    private ContextService contextService;

    private final HandleService handleService;

    public DSpaceHandleResolver()
    {
        handleService = HandleServiceFactory.getInstance().getHandleService();
    }

    @Override
    public DSpaceObject resolve(String handle) throws HandleResolverException {
        try {
            return handleService.resolveToObject(contextService.getContext(), handle);
        } catch (ContextServiceException | SQLException e) {
            throw new HandleResolverException(e);
        }
    }

    @Override
    public String getHandle(DSpaceObject object) throws HandleResolverException {
        try {
            return handleService.findHandle(contextService.getContext(), object);
        } catch (SQLException | ContextServiceException e) {
            throw new HandleResolverException(e);
        }
    }
}
