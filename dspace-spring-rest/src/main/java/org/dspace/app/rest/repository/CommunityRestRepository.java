/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.dspace.app.rest.SearchRestMethod;
import org.dspace.app.rest.converter.CommunityConverter;
import org.dspace.app.rest.model.CommunityRest;
import org.dspace.app.rest.model.hateoas.CommunityResource;
import org.dspace.content.Community;
import org.dspace.content.service.CommunityService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.stereotype.Component;

/**
 * This is the repository responsible to manage Item Rest object
 * 
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */

@Component(CommunityRest.CATEGORY + "." + CommunityRest.NAME)
public class CommunityRestRepository extends DSpaceRestRepository<CommunityRest, UUID> {

	@Autowired
	CommunityService cs;

	@Autowired
	CommunityConverter converter;

	public CommunityRestRepository() {
		System.out.println("Repository initialized by Spring");
	}

	@Override
	public CommunityRest findOne(Context context, UUID id) {
		Community community = null;
		try {
			community = cs.find(context, id);
		} catch (SQLException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
		if (community == null) {
			return null;
		}
		return converter.fromModel(community);
	}

	@Override
	public Page<CommunityRest> findAll(Context context, Pageable pageable) {
		List<Community> it = null;
		List<Community> communities = new ArrayList<Community>();
		int total = 0;
		try {
			total = cs.countTotal(context);
			it = cs.findAll(context, pageable.getPageSize(), pageable.getOffset());
			for (Community c : it) {
				communities.add(c);
			}
		} catch (SQLException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
		Page<CommunityRest> page = new PageImpl<Community>(communities, pageable, total).map(converter);
		return page;
	}

	// TODO: Add methods in dspace api to support pagination of top level
	// communities
	@SearchRestMethod(name="top")
	public Page<CommunityRest> findAllTop(Pageable pageable) {
		List<Community> topCommunities = null;
		try {
			topCommunities = cs.findAllTop(obtainContext());
		} catch (SQLException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
		Page<CommunityRest> page = utils.getPage(topCommunities, pageable).map(converter);
		return page;
	}

	// TODO: add method in dspace api to support direct query for subcommunities
	// with pagination and authorization check
	@SearchRestMethod(name="subCommunities")
	public Page<CommunityRest> findSubCommunities(@Param(value="parent") UUID parentCommunity, Pageable pageable) {
		Context context = obtainContext();
		List<Community> subCommunities = new ArrayList<Community>();
		try {
			Community community = cs.find(context, parentCommunity);
			if (community == null) {
				throw new ResourceNotFoundException(CommunityRest.CATEGORY + "." + CommunityRest.NAME + " with id: " + parentCommunity + " not found");
			}
			subCommunities = community.getSubcommunities();
		} catch (SQLException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
		Page<CommunityRest> page = utils.getPage(subCommunities, pageable).map(converter);
		return page;
	}
	
	@Override
	public Class<CommunityRest> getDomainClass() {
		return CommunityRest.class;
	}

	@Override
	public CommunityResource wrapResource(CommunityRest community, String... rels) {
		return new CommunityResource(community, utils, rels);
	}

}