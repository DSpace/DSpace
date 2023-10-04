package org.dspace.content.service;

import java.sql.SQLException;
import java.util.List;

import org.dspace.content.Community;
import org.dspace.content.CommunityGroup;
import org.dspace.core.Context;

public interface CommunityGroupService {

    /**
     * Find the CommunityGroup for the ID
     *
     * @param id id of the community group
     * @return communityGroup communityGroup
     */
    CommunityGroup find(int id);

    /**
     * Find the CommunityGroup for the ID
     *
     * @return communityGroup communityGroup
     */
    List<CommunityGroup> findAll();

    /**
     * Return the communities that are part of the given community group.
     *
     * @param context context
     * @param group communityGroup
     *
     * @return communities list of communities
     */
    List<Community> getCommunities(Context context, CommunityGroup group) throws SQLException;

}