package org.dspace.content;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.dspace.content.service.CommunityGroupService;
import org.dspace.content.service.CommunityService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;

public class CommunityGroupServiceImpl implements CommunityGroupService {

    @Autowired(required = true)
    protected CommunityService communityService;

    @Override
    public CommunityGroup find(int id) {
        return CommunityGroup.communityGroups.get(id);
    }

    @Override
    public List<CommunityGroup> findAll() {
        return new ArrayList<CommunityGroup>(CommunityGroup.communityGroups.values());
    }

    @Override
    public List<Community> getCommunities(Context context, CommunityGroup group) throws SQLException {
        return communityService.findByCommunityGroupTop(context, group);
    }
}