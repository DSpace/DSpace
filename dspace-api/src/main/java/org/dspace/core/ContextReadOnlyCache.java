/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.core;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.dspace.content.DSpaceObject;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.Set;

/**
 * Object that manages the read-only caches for the Context class
 */
public class ContextReadOnlyCache {

    /**
     * Authorized actions cache that is used when the context is in READ_ONLY mode.
     * The key of the cache is: DSpace Object ID, action ID, Eperson ID.
     */
    private final HashMap<Triple<String, Integer, String>, Boolean> authorizedActionsCache = new HashMap<>();

    /**
     * Group membership cache that is used when the context is in READ_ONLY mode.
     * The key of the cache is: Group Name, Eperson ID.
     */
    private final HashMap<Pair<String, String>, Boolean> groupMembershipCache = new HashMap<>();

    /**
     * Cache for all the groups the current ePerson is a member of when the context is in READ_ONLY mode.
     */
    private final HashMap<String, Set<Group>> allMemberGroupsCache = new HashMap<>();

    public Boolean getCachedAuthorizationResult(DSpaceObject dspaceObject, int action, EPerson eperson) {
        return authorizedActionsCache.get(buildAuthorizedActionKey(dspaceObject, action, eperson));
    }

    public void cacheAuthorizedAction(DSpaceObject dspaceObject, int action, EPerson eperson, Boolean result) {
        authorizedActionsCache.put(buildAuthorizedActionKey(dspaceObject, action, eperson), result);
    }

    public Boolean getCachedGroupMembership(Group group, EPerson eperson) {
        String allMembersGroupKey = buildAllMembersGroupKey(eperson);

        if (CollectionUtils.isEmpty(allMemberGroupsCache.get(allMembersGroupKey))) {
            return groupMembershipCache.get(buildGroupMembershipKey(group, eperson));

        } else {
            return allMemberGroupsCache.get(allMembersGroupKey).contains(group);
        }

    }

    public void cacheGroupMembership(Group group, EPerson eperson, Boolean isMember) {
        if (CollectionUtils.isEmpty(allMemberGroupsCache.get(buildAllMembersGroupKey(eperson)))) {
            groupMembershipCache.put(buildGroupMembershipKey(group, eperson), isMember);
        }
    }

    public void cacheAllMemberGroupsSet(EPerson ePerson, Set<Group> groups) {
        allMemberGroupsCache.put(buildAllMembersGroupKey(ePerson),
                groups);

        //clear the individual groupMembershipCache as we have all memberships now.
        groupMembershipCache.clear();
    }

    public Set<Group> getCachedAllMemberGroupsSet(EPerson ePerson) {
        return allMemberGroupsCache.get(buildAllMembersGroupKey(ePerson));
    }

    public void clear() {
        authorizedActionsCache.clear();
        groupMembershipCache.clear();
        allMemberGroupsCache.clear();
    }

    private String buildAllMembersGroupKey(EPerson ePerson) {
        return ePerson == null ? "" : ePerson.getID().toString();
    }

    private ImmutableTriple<String, Integer, String> buildAuthorizedActionKey(DSpaceObject dspaceObject, int action, EPerson eperson) {
        return new ImmutableTriple<>(dspaceObject == null ? "" : dspaceObject.getID().toString(),
                Integer.valueOf(action),
                eperson == null ? "" : eperson.getID().toString());
    }

    private Pair<String, String> buildGroupMembershipKey(Group group, EPerson eperson) {
        return new ImmutablePair<>(group == null ? "" : group.getName(),
                eperson == null ? "" : eperson.getID().toString());
    }

}
