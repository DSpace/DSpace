/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.sql.SQLException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import jakarta.annotation.Nullable;
import jakarta.servlet.http.HttpServletRequest;
import org.dspace.app.rest.model.CommunityRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.content.Community;
import org.dspace.content.service.CommunityService;
import org.dspace.core.Context;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.DiscoverResult;
import org.dspace.discovery.IndexableObject;
import org.dspace.discovery.SearchService;
import org.dspace.discovery.SearchServiceException;
import org.dspace.discovery.indexobject.IndexableCommunity;
import org.dspace.discovery.indexobject.factory.IndexObjectFactoryFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * Link repository for "subcommunities" subresource of an individual community.
 */
@Component(CommunityRest.CATEGORY + "." + CommunityRest.PLURAL_NAME + "." + CommunityRest.SUBCOMMUNITIES)
public class CommunitySubcommunityLinkRepository extends AbstractDSpaceRestRepository
        implements LinkRestRepository {

    @Autowired
    CommunityService communityService;

    @Autowired
    SearchService searchService;

    @PreAuthorize("hasPermission(#communityId, 'COMMUNITY', 'READ')")
    public Page<CommunityRest> getSubcommunities(@Nullable HttpServletRequest request,
                                                 UUID communityId,
                                                 @Nullable Pageable optionalPageable,
                                                 Projection projection) {
        try {
            Context context = obtainContext();
            Community community = communityService.find(context, communityId);
            if (community == null) {
                throw new ResourceNotFoundException("No such community: " + communityId);
            }
            Pageable pageable = utils.getPageable(optionalPageable);
            List<Community> publicSubcommunities = new LinkedList<Community>();
            IndexObjectFactoryFactory indexObjectFactory = IndexObjectFactoryFactory.getInstance();
            IndexableObject scopeObject = indexObjectFactory.getIndexableObjects(context, community).get(0);
            DiscoverQuery discoverQuery = new DiscoverQuery();
            discoverQuery.setQuery("*:*");
            discoverQuery.setDSpaceObjectFilter(IndexableCommunity.TYPE);
            discoverQuery.addFilterQueries("location.parent:" + communityId);
            discoverQuery.setStart(Math.toIntExact(pageable.getOffset()));
            discoverQuery.setMaxResults(pageable.getPageSize());
            discoverQuery.setSortField("dc.title_sort", DiscoverQuery.SORT_ORDER.asc);
            Iterator<Order> orderIterator = pageable.getSort().iterator();
            if (orderIterator.hasNext()) {
                Order order = orderIterator.next();
                discoverQuery.setSortField(
                    order.getProperty() + "_sort",
                    order.getDirection().isAscending() ? DiscoverQuery.SORT_ORDER.asc : DiscoverQuery.SORT_ORDER.desc
                );
            }
            DiscoverResult resp = searchService.search(context, scopeObject, discoverQuery);
            long tot = resp.getTotalSearchResults();
            for (IndexableObject solrCommunities : resp.getIndexableObjects()) {
                Community c = ((IndexableCommunity) solrCommunities).getIndexedObject();
                publicSubcommunities.add(c);
            }
            return converter.toRestPage(publicSubcommunities, pageable, tot, utils.obtainProjection());
        } catch (SQLException | SearchServiceException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}
