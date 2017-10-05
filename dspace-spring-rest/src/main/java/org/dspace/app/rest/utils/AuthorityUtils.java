/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.utils;

import org.dspace.app.rest.model.AuthorityRest;
import org.dspace.content.authority.factory.ContentAuthorityServiceFactory;
import org.dspace.content.authority.service.ChoiceAuthorityService;
import org.springframework.stereotype.Component;

/**
 * Utility methods to expose the authority framework over REST
 * 
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 * 
 */
@Component
public class AuthorityUtils {
	private ChoiceAuthorityService cas = ContentAuthorityServiceFactory.getInstance().getChoiceAuthorityService();
	
	public AuthorityRest getAuthority(String schema, String element, String qualifier) {
		AuthorityRest authorityRest = new AuthorityRest();
		authorityRest.setName(cas.getChoiceAuthorityName(schema, element, qualifier));
		authorityRest.setHierarchical(cas.isHierarchical(schema, element, qualifier));
		authorityRest.setScrollable(cas.isScrollable(schema, element, qualifier));
		return authorityRest;
	}
}
