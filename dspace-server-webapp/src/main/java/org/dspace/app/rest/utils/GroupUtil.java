/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.utils;

import java.sql.SQLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.core.Context;
import org.dspace.eperson.Group;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * A class which provides utility methods for Groups
 */
@Component
public class GroupUtil {

    private GroupUtil() {
    }

    /**
     * UUID regex used in the collection regex
     */
    private static final String UUID_REGEX = "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}\\-[0-9a-fA-F]{4}\\-[0" +
            "-9a-fA-F]{12}";
    /**
     * Collection regex used to extract the ID
     */
    private static final String COLLECTION_REGEX = "COLLECTION_(" + UUID_REGEX + ")_.*?";

    /**
     * The community prefix: all groups which are specific to
     * a community start with this.
     */
    private static final String COMMUNITY_PREFIX = "COMMUNITY_";

    /**
     * These are the possible community suffixes. All groups which are
     * specific to a community will end with one of these.
     */
    private static final String[] COMMUNITY_SUFFIXES = {"_ADMIN"};

    @Autowired
    protected CollectionService collectionService;

    @Autowired
    protected CommunityService communityService;

    /**
     * Get the collection a given group is related to, or null if it is not related to a collection.
     */
    public Collection getCollection(Context context, Group group) throws SQLException {

        String groupName = group.getName();

        if (groupName == null) {
            return null;
        }

        Matcher groupNameMatcher = Pattern.compile(COLLECTION_REGEX).matcher(groupName);

        if (groupNameMatcher.find()) {
            String uuid = groupNameMatcher.group();
            Collection collection = collectionService.findByIdOrLegacyId(context, uuid);
            if (collection != null) {
                return collection;
            }
        }

        return null;
    }

    /**
     * Get the community a given group is related to, or null if it is not related to a community.
     */
    public Community getCommunity(Context context, Group group) throws SQLException {

        String groupName = group.getName();

        if (groupName == null || !groupName.startsWith(COMMUNITY_PREFIX)) {
            return null;
        }
        for (String suffix : COMMUNITY_SUFFIXES) {
            if (groupName.endsWith(suffix)) {
                String idString = groupName.substring(COMMUNITY_PREFIX.length());
                idString = idString.substring(0, idString.length() - suffix.length());

                Community community = communityService.findByIdOrLegacyId(context, idString);
                if (community != null) {
                    return community;
                } else {
                    return null;
                }
            }
        }

        return null;
    }
}
