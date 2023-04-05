/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.util.LinkedList;
import java.util.List;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;

import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.model.CommunityGroupRest;
import org.dspace.app.rest.model.CommunityRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.content.Community;
import org.dspace.core.Context;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.DiscoverResult;
import org.dspace.discovery.IndexableObject;
import org.dspace.discovery.SearchService;
import org.dspace.discovery.SearchServiceException;
import org.dspace.discovery.indexobject.IndexableCommunity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * Link repository for "communities" subresource of a community group.
 *
 * @author Mohamed Abdul Rasheed (mohideen at umd.edu)
 */
@Component(CommunityGroupRest.CATEGORY + "." + CommunityGroupRest.NAME + "." + CommunityGroupRest.COMMUNITIES)
public class CommunityGroupCommunityLinkRepository extends AbstractDSpaceRestRepository
        implements LinkRestRepository {

    private static final Logger log = org.apache.logging.log4j.LogManager
            .getLogger(CommunityGroupCommunityLinkRepository.class);

    @Autowired
    SearchService searchService;

    @PreAuthorize("permitAll()")
    public Page<CommunityRest> getCommunities(@Nullable HttpServletRequest request,
            Integer communityGroupId,
            Pageable pageable,
            Projection projection) {
        try {
            Context context = obtainContext();
            List<Community> topLevelCommunities = new LinkedList<Community>();
            DiscoverQuery discoverQuery = new DiscoverQuery();
            discoverQuery.setQuery("*:*");
            discoverQuery.setDSpaceObjectFilter(IndexableCommunity.TYPE);
            discoverQuery.addFilterQueries("-location.parent:*");
            discoverQuery.addFilterQueries("community_group:" + communityGroupId.toString());
            discoverQuery.setStart(Math.toIntExact(pageable.getOffset()));
            discoverQuery.setSortField("dc.title_sort", DiscoverQuery.SORT_ORDER.asc);
            discoverQuery.setMaxResults(pageable.getPageSize());
            DiscoverResult resp = searchService.search(context, discoverQuery);
            long tot = resp.getTotalSearchResults();
            for (IndexableObject solrCommunities : resp.getIndexableObjects()) {
                Community c = ((IndexableCommunity) solrCommunities).getIndexedObject();
                topLevelCommunities.add(c);
            }
            return converter.toRestPage(topLevelCommunities, pageable, tot, utils.obtainProjection());
        } catch (SearchServiceException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}
