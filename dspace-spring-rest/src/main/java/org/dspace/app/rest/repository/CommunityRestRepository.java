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

import org.dspace.app.rest.converter.CommunityConverter;
import org.dspace.app.rest.model.CommunityRest;
import org.dspace.app.rest.model.hateoas.CommunityResource;
import org.dspace.content.Community;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CommunityService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

/**
 * This is the repository responsible to manage Item Rest object
 * 
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */

@Component(CommunityRest.NAME)
public class CommunityRestRepository extends DSpaceRestRepository<CommunityRest, UUID> {
	CommunityService cs = ContentServiceFactory.getInstance().getCommunityService();
	@Autowired
	CommunityConverter converter;
	
	
	public CommunityRestRepository() {
		System.out.println("Repository initialized by Spring");
	}

	@Override
	public CommunityRest findOne(Context context, UUID id, String projection) {
		Community community = null;
		try {
			community = cs.find(context, id);
		} catch (SQLException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
		if (community == null) {
			return null;
		}
		return converter.fromModel(community, projection);
	}

	@Override
	public Page<CommunityRest> findAll(Context context, Pageable pageable) {
		List<Community> it = null;
		List<Community> communities = new ArrayList<Community>();
		int total = 0;
		try {
			total = cs.countTotal(context);
			it = cs.findAll(context, pageable.getPageSize(), pageable.getOffset());
			for (Community c: it) {
				communities.add(c);
			}
		} catch (SQLException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
		Page<CommunityRest> page = new PageImpl<Community>(communities, pageable, total).map(converter);
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