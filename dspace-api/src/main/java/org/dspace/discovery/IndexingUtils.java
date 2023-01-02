/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.dspace.authorize.ResourcePolicy;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.core.Context;

/**
 * Util methods used by indexing.
 *
 * @author Koen Pauwels (koen.pauwels at atmire dot com)
 */
public class IndexingUtils {
    private IndexingUtils() {
    }

    /**
     * Retrieve all ancestor communities of a given community, with the first one being the given community and the
     * last one being the root.
     * <p>
     *
     * @param context   DSpace context object
     * @param community Community for which we search the ancestors
     * @return A list of ancestor communities.
     * @throws SQLException if database error
     */
    static List<Community> getAncestorCommunities(Context context, Community community) throws SQLException {
        ArrayList<Community> communities = new ArrayList<>();
        while (community != null) {
            communities.add(community);
            community = (Community) ContentServiceFactory.getInstance().getDSpaceObjectService(community)
                .getParentObject(context, community);
        }
        return communities;
    }

    /**
     * Retrieve the ids of all groups that have ADMIN rights to the given community, either directly
     * (through direct resource policy) or indirectly (through a policy on an ancestor community).
     *
     * @param context   DSpace context object
     * @param community Community for which we search the admin group IDs
     * @return A list of admin group IDs
     * @throws SQLException if database error
     */
    static List<UUID> findTransitiveAdminGroupIds(Context context, Community community) throws SQLException {
        return getAncestorCommunities(context, community).stream()
            .filter(parent -> parent.getAdministrators() != null)
            .map(parent -> parent.getAdministrators().getID())
            .collect(Collectors.toList());
    }

    /**
     * Retrieve the ids of all groups that have ADMIN rights to the given collection, either directly
     * (through direct resource policy) or indirectly (through a policy on its community, or one of
     * its ancestor communities).
     *
     * @param context    DSpace context object
     * @param collection Collection for which we search the admin group IDs
     * @return A list of admin group IDs
     * @throws SQLException if database error
     */
    static List<UUID> findTransitiveAdminGroupIds(Context context, Collection collection) throws SQLException {
        List<UUID> ids = new ArrayList<>();
        if (collection.getAdministrators() != null) {
            ids.add(collection.getAdministrators().getID());
        }
        for (Community community : collection.getCommunities()) {
            for (UUID id : findTransitiveAdminGroupIds(context, community)) {
                ids.add(id);
            }
        }
        return ids;
    }

    /**
     * Retrieve group and eperson IDs for all groups and eperson who have _any_ of the given authorizations
     * on the given DSpaceObject. The resulting IDs are prefixed with "e" in the case of an eperson ID, and "g" in the
     * case of a group ID.
     *
     * @param authService The authentication service
     * @param context     DSpace context object
     * @param obj         DSpaceObject for which we search the admin group IDs
     * @return A stream of admin group IDs as Strings, prefixed with either "e" or "g", depending on whether it is a
     * group or eperson ID.
     * @throws SQLException if database error
     */
    static List<String> findDirectlyAuthorizedGroupAndEPersonPrefixedIds(
        AuthorizeService authService, Context context, DSpaceObject obj, int[] authorizations)
        throws SQLException {
        ArrayList<String> prefixedIds = new ArrayList<>();
        for (int auth : authorizations) {
            for (ResourcePolicy policy : authService.getPoliciesActionFilter(context, obj, auth)) {
                String prefixedId = policy.getGroup() == null
                    ? "e" + policy.getEPerson().getID()
                    : "g" + policy.getGroup().getID();
                prefixedIds.add(prefixedId);
                context.uncacheEntity(policy);
            }
        }
        return prefixedIds;
    }
}
