/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import org.dspace.app.rest.converter.EPersonGroupConverter;
import org.dspace.app.rest.model.EPersonGroupRest;
import org.dspace.app.rest.model.hateoas.EPersonGroupResource;
import org.dspace.core.Context;
import org.dspace.eperson.Group;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.GroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

/**
 * This is the repository responsible to manage EPerson Rest object
 * 
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */

@Component(EPersonGroupRest.NAME)
public class EPersonGroupRestRepository extends DSpaceRestRepository<EPersonGroupRest, UUID> {
	GroupService gs = EPersonServiceFactory.getInstance().getGroupService();
	
	@Autowired
	EPersonGroupConverter converter;
	
	@Override
	public EPersonGroupRest findOne(Context context, UUID id) {
		Group group = null;
		try {
			group = gs.find(context, id);
		} catch (SQLException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
		if (group == null) {
			return null;
		}
		return converter.fromModel(group);
	}

	@Override
	public Page<EPersonGroupRest> findAll(Context context, Pageable pageable) {
		List<Group> groups = null;
		int total = 0;
		try {
			total = gs.countTotal(context);
			groups = gs.findAll(context, null, pageable.getPageSize(), pageable.getOffset());
		} catch (SQLException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
		Page<EPersonGroupRest> page = new PageImpl<Group>(groups, pageable, total).map(converter);
		return page;
	}
	
	@Override
	public Class<EPersonGroupRest> getDomainClass() {
		return EPersonGroupRest.class;
	}
	
	@Override
	public EPersonGroupResource wrapResource(EPersonGroupRest eperson, String... rels) {
		return new EPersonGroupResource(eperson, utils, rels);
	}

}