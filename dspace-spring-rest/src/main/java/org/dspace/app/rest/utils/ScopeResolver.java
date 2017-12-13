package org.dspace.app.rest.utils;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.sql.SQLException;
import java.util.UUID;

/**
 * Utility bean that can resolve a scope in the REST API to a DSpace Object
 */
@Component
public class ScopeResolver {

    private static final Logger log = Logger.getLogger(ScopeResolver.class);

    @Autowired
    CollectionService collectionService;

    @Autowired
    CommunityService communityService;

    public DSpaceObject resolveScope(Context context, String scope) {
        DSpaceObject scopeObj = null;
        if (StringUtils.isNotBlank(scope)) {
            try {
                UUID uuid = UUID.fromString(scope);
                scopeObj = communityService.find(context, uuid);
                if (scopeObj == null) {
                    scopeObj = collectionService.find(context, uuid);
                }
            } catch (IllegalArgumentException ex) {
                log.warn("The given scope string " + StringUtils.trimToEmpty(scope) + " is not a UUID", ex);
            } catch (SQLException ex) {
                log.warn("Unable to retrieve DSpace Object with ID " + StringUtils.trimToEmpty(scope) + " from the database", ex);
            }
        }

        return scopeObj;
    }

}
