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
import org.dspace.app.rest.model.EPersonRest;
import org.dspace.app.rest.model.GroupRest;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * This is the converter from/to the EPerson in the DSpace API data model and the
 * REST data model
 * 
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */
@Component
public class EPersonConverter extends DSpaceObjectConverter<EPerson, org.dspace.app.rest.model.EPersonRest> {
	@Autowired(required = true)
	private GroupConverter epersonGroupConverter;

	private static final Logger log = Logger.getLogger(EPersonConverter.class);
	
	@Override
	public EPersonRest fromModel(EPerson obj) {
		EPersonRest eperson = super.fromModel(obj);
		eperson.setLastActive(obj.getLastActive());
		eperson.setNetid(obj.getNetid());
		eperson.setCanLogIn(obj.canLogIn());
		eperson.setRequireCertificate(obj.getRequireCertificate());
		eperson.setSelfRegistered(obj.getSelfRegistered());
		eperson.setEmail(obj.getEmail());
		List<GroupRest> groups = new ArrayList<GroupRest>();
		for (Group g : obj.getGroups()) {
			groups.add(epersonGroupConverter.convert(g));
		}
		eperson.setGroups(groups);
		return eperson;
	}

	@Override
	public EPerson toModel(EPersonRest obj) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected EPersonRest newInstance() {
		return new EPersonRest();
	}

}
