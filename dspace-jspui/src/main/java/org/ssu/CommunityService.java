package org.ssu;

import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.springframework.stereotype.Service;
import org.ssu.entity.response.CommunityResponse;

import java.sql.SQLException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CommunityService {
    private final transient org.dspace.content.service.CommunityService communityService = ContentServiceFactory.getInstance().getCommunityService();
    private final transient AuthorizeService authorizeService = AuthorizeServiceFactory.getInstance().getAuthorizeService();

    public CommunityResponse build(Context context) throws SQLException {
        Map<String, List<Community>> subCommunities;
        subCommunities = new HashMap<>();
        List<Community> communities = communityService.findAllTop(context)
                .stream()
                .sorted(Comparator.comparing(Community::getName))
                .collect(Collectors.toList());
        for (Community community : communities) {
            build(community, subCommunities, context);
        }
        return new CommunityResponse.Builder()
                .withCommMap(subCommunities)
                .withIsAdmin(authorizeService.isAdmin(context))
                .withCommunities(communities)
                .build();
    }

    private void build(Community community, Map<String, List<Community>> commMap, Context context) throws SQLException {
        if(authorizeService.authorizeActionBoolean(context, community, Constants.READ)) {
            String comID = community.getID().toString();
            List<Community> communities = community.getSubcommunities()
                    .stream()
                    .sorted(Comparator.comparing(Community::getName))
                    .collect(Collectors.toList());

            for(Collection collection : community.getCollections()) {
                if(!authorizeService.authorizeActionBoolean(context, collection, Constants.READ)) {
                    community.removeCollection(collection);
                }
            }

            if (communities.size() > 0) {
                commMap.put(comID, communities);
                for (Community sub : communities) {
                    build(sub, commMap, context);
                }
            }
        }
    }
}
