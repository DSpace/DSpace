/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.handle.hdlresolver;

import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.core.Context;
import org.dspace.handle.service.HandleService;
import org.dspace.services.ConfigurationService;
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

    public static final String LISTHANDLES_HIDE_PROP = "handle.hide.listhandles";

    private static final Logger log = LogManager.getLogger();

    @Autowired(required = true)
    private HandleService handleService;

    @Autowired(required = true)
    private ConfigurationService configurationService;

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

    @Override
    public List<String> listPrefixes() {
        return Stream.concat(
                Stream.of(this.handleService.getAdditionalPrefixes()),
                Stream.of(this.handleService.getPrefix())
            )
            .filter(StringUtils::isNotBlank)
            .collect(Collectors.toList());
    }

    @Override
    public List<String> listHandles(Context context, String prefix) {
        List<String> handlesForPrefix = List.of();
        try {
            handlesForPrefix = this.handleService.getHandlesForPrefix(context, prefix);
        } catch (SQLException e) {
            log.error("Error while listing handles for prefix: " + prefix, e);
            throw new RuntimeException("Error while listing handles for prefix: " + prefix, e);
        }
        return handlesForPrefix;
    }

    @Override
    public boolean isListhandlesEnabled() {
        return !this.configurationService.getBooleanProperty(LISTHANDLES_HIDE_PROP);
    }

}
