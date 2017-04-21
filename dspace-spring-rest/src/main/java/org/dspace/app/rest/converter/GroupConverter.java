/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.dspace.app.rest.model.GroupRest;
import org.dspace.eperson.Group;
import org.springframework.stereotype.Component;

/**
 * This is the converter from/to the Group in the DSpace API data model
 * and the REST data model
 * 
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */
@Component
public class GroupConverter extends DSpaceObjectConverter<Group, org.dspace.app.rest.model.GroupRest> {

	private static final Logger log = Logger.getLogger(GroupConverter.class);

	@Override
	public GroupRest fromModel(Group obj) {
		GroupRest epersongroup = super.fromModel(obj);
		epersongroup.setPermanent(obj.isPermanent());
		List<GroupRest> groups = new ArrayList<GroupRest>();
		for (Group g : obj.getMemberGroups()) {
			groups.add(convert(g));
		}
		epersongroup.setGroups(groups);
		
		return epersongroup;
	}

	@Override
	public Group toModel(GroupRest obj) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected GroupRest newInstance() {
		return new GroupRest();
	}

}
