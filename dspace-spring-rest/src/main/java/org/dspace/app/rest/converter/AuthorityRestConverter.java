/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import org.apache.commons.lang.NotImplementedException;
import org.dspace.app.rest.model.AuthorityRest;
import org.dspace.app.rest.utils.AuthorityUtils;
import org.dspace.content.authority.Choice;
import org.dspace.content.authority.ChoiceAuthority;
import org.springframework.stereotype.Component;

/**
 * This is the converter from/to the ChoiceAuthority in the DSpace API data
 * model and the REST data model
 * 
 * TODO please do not use this convert but use the wrapper {@link AuthorityUtils#convertAuthority(ChoiceAuthority, String)}
 * 
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science.it)
 */
@Component
public class AuthorityRestConverter extends DSpaceConverter<ChoiceAuthority, AuthorityRest> {
	
	@Override
	public AuthorityRest fromModel(ChoiceAuthority step) {
		AuthorityRest authorityRest = new AuthorityRest();		
		authorityRest.setHierarchical(step.isHierarchical());
		authorityRest.setScrollable(step.isScrollable());
		authorityRest.setIdentifier(step.hasIdentifier());
		return authorityRest;
	}

	@Override
	public ChoiceAuthority toModel(AuthorityRest obj) {
		throw new NotImplementedException();
	}
}