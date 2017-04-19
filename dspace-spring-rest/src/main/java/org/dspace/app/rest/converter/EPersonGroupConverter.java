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
import org.dspace.app.rest.model.EPersonGroupRest;
import org.dspace.eperson.Group;
import org.springframework.stereotype.Component;

/**
 * This is the converter from/to the EPerson Group in the DSpace API data model
 * and the REST data model
 * 
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */
@Component
public class EPersonGroupConverter extends DSpaceObjectConverter<Group, org.dspace.app.rest.model.EPersonGroupRest> {

	private static final Logger log = Logger.getLogger(EPersonGroupConverter.class);

	@Override
	public EPersonGroupRest fromModel(Group obj) {
		EPersonGroupRest epersongroup = super.fromModel(obj);
		epersongroup.setPermanent(obj.isPermanent());
		List<EPersonGroupRest> groups = new ArrayList<EPersonGroupRest>();
		for (Group g : obj.getMemberGroups()) {
			groups.add(convert(g));
		}
		epersongroup.setGroups(groups);
		
		return epersongroup;
	}

	@Override
	public Group toModel(EPersonGroupRest obj) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected EPersonGroupRest newInstance() {
		return new EPersonGroupRest();
	}

}
