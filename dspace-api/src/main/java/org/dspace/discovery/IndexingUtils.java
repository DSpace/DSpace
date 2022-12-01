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
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.core.Constants;
import org.dspace.core.Context;

/**
 * Util methods used by indexing.
 *
 * @author Koen Pauwels (koen.pauwels at atmire dot com)
 */
public abstract class IndexingUtils {
    /**
     * Retrieve all ancestor communities of a given community, with the first one being the given community and the
     * last one being the root.
     *
     * TODO: can be done in a single SQL query with recursive common table expressions
     * TODO: should probably be moved to CommunityService
     *
     * @param context   DSpace context object
     * @param community Community for which we search the ancestors
     * @return A stream of ancestor communities.
     * @throws SQLException if database error
     */
    static Stream<Community> getAncestorCommunities(Context context, Community community) throws SQLException {
        ArrayList<Community> communities = new ArrayList<>();
        while (community != null) {
            communities.add(community);
            community = (Community) ContentServiceFactory.getInstance().getDSpaceObjectService(community)
                .getParentObject(context, community);
        }
        return communities.stream();
    }

    /**
     * Retrieve the ids of all groups that have ADMIN rights to the given community, either directly
     * (through direct resource policy) or indirectly (through a policy on an ancestor community).
     *
     * @param context   DSpace context object
     * @param community Community for which we search the admin group IDs
     * @return A stream of admin group IDs
     * @throws SQLException if database error
     */
    static Stream<UUID> findTransitiveAdminGroupIds(Context context, Community community) throws SQLException {
        return getAncestorCommunities(context, community)
            .map(parent -> parent.getAdministrators().getID());
    }

    /**
     * Retrieve the ids of all groups that have ADMIN rights to the given collection, either directly
     * (through direct resource policy) or indirectly (through a policy on its community, or one of
     * its ancestor communities).
     *
     * @param context   DSpace context object
     * @param collection Collection for which we search the admin group IDs
     * @return A stream of admin group IDs
     * @throws SQLException if database error
     */
    static Stream<UUID> findTransitiveAdminGroupIds(Context context, Collection collection) throws SQLException {
        UUID directAdminGroupId = collection.getAdministrators().getID();
        List<Stream<UUID>> subResults = Arrays.asList(Stream.of(directAdminGroupId));
        for (Community community : collection.getCommunities()) {
            subResults.add(findTransitiveAdminGroupIds(context, community));
        }
        return sequence(subResults);
    }

    /**
     * Retrieve the ids of all groups that have ADMIN rights on the given item, either directly
     * (through direct resource policy) or indirectly (through a policy on the owning collection, or on
     * the owning collection's community, or on any of that community's ancestor communities).
     *
     * @param authService
     * @param context
     * @param item
     * @return
     * @throws SQLException
     *
     * @param authService   The authentication service
     * @param context       DSpace context object
     * @param item          Item for which we search the admin group IDs
     * @return A stream of admin group IDs
     * @throws SQLException if database error
     */
    static Stream<UUID> findTransitiveAdminGroupIds(AuthorizeService authService, Context context, Item item)
        throws SQLException {
        Stream<UUID> directAdminGroupIds = authService.getPoliciesActionFilter(context, item, Constants.ADMIN)
            .stream()
            .filter(policy -> policy.getGroup() != null)
            .map(policy -> policy.getGroup().getID());
        List<Stream<UUID>> subResults = Arrays.asList(directAdminGroupIds);
        for (Collection coll : item.getCollections()) {
            subResults.add(findTransitiveAdminGroupIds(context, coll));
        }
        return sequence(subResults);
    }

    /**
     * Retrieve group and eperson IDs for all groups and eperson who have _any_ of the given authorizations
     * on the given DSpaceObject. The resulting IDs are prefixed with "e" in the case of an eperson ID, and "g" in the
     * case of a group ID.
     *
     * @param authService   The authentication service
     * @param context       DSpace context object
     * @param obj           DSpaceObject for which we search the admin group IDs
     * @return  A stream of admin group IDs as Strings, prefixed with either "e" or "g", depending on whether it is a
     *          group or eperson ID.
     * @throws SQLException if database error
     */
    static Stream<String> findDirectAuthorizedGroupsAndEPersonsPrefixedIds(AuthorizeService authService,
        Context context, DSpaceObject obj, int[] authorizations) throws SQLException {

        ArrayList<Stream<String>> subResults = new ArrayList<>();
        for (int auth : authorizations) {
            Stream<String> subResult = authService.getPoliciesActionFilter(context, obj, auth).stream()
                .map(policy -> policy.getGroup() == null ? "e" + policy.getEPerson().getID()
                                                         : "g" + policy.getGroup().getID());
            subResults.add(subResult);
            // TODO: context.uncacheEntitiy(policy);
        }
        return sequence(subResults);
    }

    private static <T> Stream<T> sequence(List<Stream<T>> subResults) {
        return subResults.stream().flatMap(x -> x);
    }
}
