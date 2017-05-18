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

import org.dspace.app.rest.converter.GroupConverter;
import org.dspace.app.rest.model.GroupRest;
import org.dspace.app.rest.model.hateoas.GroupResource;
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
 * This is the repository responsible to manage Group Rest object
 * 
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */

@Component(GroupRest.CATEGORY + "." + GroupRest.NAME)
public class GroupRestRepository extends DSpaceRestRepository<GroupRest, UUID> {
	GroupService gs = EPersonServiceFactory.getInstance().getGroupService();
	
	@Autowired
	GroupConverter converter;
	
	@Override
	public GroupRest findOne(Context context, UUID id) {
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
	public Page<GroupRest> findAll(Context context, Pageable pageable) {
		List<Group> groups = null;
		int total = 0;
		try {
			total = gs.countTotal(context);
			groups = gs.findAll(context, null, pageable.getPageSize(), pageable.getOffset());
		} catch (SQLException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
		Page<GroupRest> page = new PageImpl<Group>(groups, pageable, total).map(converter);
		return page;
	}
	
	@Override
	public Class<GroupRest> getDomainClass() {
		return GroupRest.class;
	}
	
	@Override
	public GroupResource wrapResource(GroupRest eperson, String... rels) {
		return new GroupResource(eperson, utils, rels);
	}

}