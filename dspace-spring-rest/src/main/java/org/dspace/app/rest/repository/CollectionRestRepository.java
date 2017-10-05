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
import org.dspace.app.rest.converter.CollectionConverter;
import org.dspace.app.rest.model.CollectionRest;
import org.dspace.app.rest.model.hateoas.CollectionResource;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Component;

/**
 * This is the repository responsible to manage Item Rest object
 * 
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */

@Component(CollectionRest.CATEGORY + "." + CollectionRest.NAME)
public class CollectionRestRepository extends DSpaceRestRepository<CollectionRest, UUID> {

	@Autowired
	CommunityService communityService;
	
	@Autowired
	CollectionService cs;

	@Autowired
	CollectionConverter converter;
	
	
	public CollectionRestRepository() {
		System.out.println("Repository initialized by Spring");
	}

	@Override
	public CollectionRest findOne(Context context, UUID id) {
		Collection collection = null;
		try {
			collection = cs.find(context, id);
		} catch (SQLException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
		if (collection == null) {
			return null;
		}
		return converter.fromModel(collection);
	}

	@Override
	public Page<CollectionRest> findAll(Context context, Pageable pageable) {
		List<Collection> it = null;
		List<Collection> collections = new ArrayList<Collection>();
		int total = 0;
		try {
			total = cs.countTotal(context);
			it = cs.findAll(context, pageable.getPageSize(), pageable.getOffset());
			for (Collection c: it) {
				collections.add(c);
			}
		} catch (SQLException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
		Page<CollectionRest> page = new PageImpl<Collection>(collections, pageable, total).map(converter);
		return page;
	}

	@SearchRestMethod(name="findAuthorizedByCommunity")
	public Page<CollectionRest> findAuthorizedByCommunity(@Param(value="uuid") UUID communityUuid, Pageable pageable) {
		Context context = obtainContext();
		List<Collection> it = null;
		List<Collection> collections = new ArrayList<Collection>();
		try {
			Community com = communityService.find(context, communityUuid);
			it = cs.findAuthorized(context, com, Constants.ADD);
			for (Collection c: it) {
				collections.add(c);
			}
		} catch (SQLException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
		Page<CollectionRest> page = utils.getPage(collections, pageable).map(converter);
		return page;
	}

	@SearchRestMethod(name="findAuthorized")
	public Page<CollectionRest> findAuthorized(Pageable pageable) {
		Context context = obtainContext();
		List<Collection> it = null;
		List<Collection> collections = new ArrayList<Collection>();
		try {
			it = cs.findAuthorizedOptimized(context, Constants.ADD);
			for (Collection c: it) {
				collections.add(c);
			}
		} catch (SQLException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
		Page<CollectionRest> page = utils.getPage(collections, pageable).map(converter);
		return page;
	}
	
	@Override
	public Class<CollectionRest> getDomainClass() {
		return CollectionRest.class;
	}
	
	@Override
	public CollectionResource wrapResource(CollectionRest collection, String... rels) {
		return new CollectionResource(collection, utils, rels);
	}

}