/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xoai.services.impl.context;

import org.dspace.core.Context;
import org.dspace.xoai.services.api.context.ContextService;
import org.dspace.xoai.services.api.context.ContextServiceException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

public class DSpaceContextService implements ContextService {
    private static final String OAI_CONTEXT = "OAI_CONTEXT";

    @Override
    public Context getContext() throws ContextServiceException {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        Object value = request.getAttribute(OAI_CONTEXT);
        if (value == null || !(value instanceof Context)) {
            request.setAttribute(OAI_CONTEXT, new Context());
        }
        return (Context) request.getAttribute(OAI_CONTEXT);
    }
}
