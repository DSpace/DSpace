/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.utils;

import java.sql.SQLException;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.core.Context;
import org.dspace.discovery.IndexableObject;
import org.dspace.discovery.indexobject.IndexableCollection;
import org.dspace.discovery.indexobject.IndexableCommunity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Utility bean that can resolve a scope in the REST API to a DSpace Object.
 */
@Component
public class ScopeResolver {

    private static final Logger log = LogManager.getLogger();

    @Autowired
    CollectionService collectionService;

    @Autowired
    CommunityService communityService;

    /**
     * Resolve the unique identifier of a scope object to that object.
     *
     * @param context current DSpace session.
     * @param scope UUID of the desired scope object.
     * @return the identified object, or {@code null} if not found.
     */
    public IndexableObject resolveScope(Context context, String scope) {
        IndexableObject scopeObj = null;
        if (StringUtils.isNotBlank(scope)) {
            String myScope = StringUtils.trimToEmpty(scope);
            try {
                UUID uuid = UUID.fromString(myScope);
                Community community = communityService.find(context, uuid);
                if (null != community) {
                    scopeObj = new IndexableCommunity(community);
                } else {
                    Collection collection = collectionService.find(context, uuid);
                    if (null != collection) {
                        scopeObj = new IndexableCollection(collection);
                    } else {
                        log.warn("The given scope string '{}' resolves to neither a Community nor a Collection",
                                StringUtils.trimToEmpty(myScope));
                    }
                }
            } catch (IllegalArgumentException ex) {
                log.warn("The given scope string '{}' is not a UUID", myScope, ex);
            } catch (SQLException ex) {
                log.warn(
                    "Unable to retrieve DSpace Object with ID '{}' from the database",
                    myScope, ex);
            }
        }

        return scopeObj;
    }

}
