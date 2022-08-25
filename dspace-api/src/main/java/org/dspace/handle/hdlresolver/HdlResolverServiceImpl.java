/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.handle.hdlresolver;

import java.sql.SQLException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.core.Context;
import org.dspace.handle.service.HandleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 
 * Handle Resolver that uses an <code>HandleService</code> to retrieve the right
 * URL of a target Handle.
 * 
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.it)
 *
 */
@Service
public class HdlResolverServiceImpl implements HdlResolverService {

    private static final Logger log = LogManager.getLogger();

    @Autowired(required = true)
    private HandleService handleService;

    @Override
    public HdlResolverDTO resolveBy(String requestURI, String path) {
        return new HdlResolverDTO(requestURI, path);
    }

    @Override
    public String resolveToURL(Context context, HdlResolverDTO hdlResolver) {
        try {
            return this.handleService.resolveToURL(context, hdlResolver.getHandle());
        } catch (SQLException e) {
            log.error("Error while resolving Handle: " + hdlResolver.getHandle(), e);
            throw new RuntimeException("Error while resolving Handle: " + hdlResolver.getHandle(), e);
        }
    }

}
