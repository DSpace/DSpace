/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.dspace.app.rest.converter.AuthorityRestConverter;
import org.dspace.app.rest.model.AuthorityRest;
import org.dspace.app.rest.model.hateoas.AuthorityResource;
import org.dspace.app.rest.utils.AuthorityUtils;
import org.dspace.content.authority.Choice;
import org.dspace.content.authority.ChoiceAuthority;
import org.dspace.content.authority.service.ChoiceAuthorityService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

/**
 * Controller for exposition of authority services
 * 
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science.it)
 *
 */
@Component(AuthorityRest.CATEGORY + "." + AuthorityRest.NAME)
public class AuthorityRestRepository extends DSpaceRestRepository<AuthorityRest, String> {

	@Autowired
	private ChoiceAuthorityService cas;
	
	@Autowired
	private AuthorityUtils authorityUtils;
	
	@Override
	public AuthorityRest findOne(Context context, String name) {
		ChoiceAuthority source = cas.getChoiceAuthorityByAuthorityName(name);
		AuthorityRest result = authorityUtils.convertAuthority(source, name);
		return result;
	}

	@Override
	public Page<AuthorityRest> findAll(Context context, Pageable pageable) {
		Set<String> authoritiesName = cas.getChoiceAuthoritiesNames();
		List<AuthorityRest> results = new ArrayList<AuthorityRest>();
		for(String authorityName : authoritiesName) {
			ChoiceAuthority source = cas.getChoiceAuthorityByAuthorityName(authorityName);						
			AuthorityRest result = authorityUtils.convertAuthority(source, authorityName);
			results.add(result);
		}
		return new PageImpl<AuthorityRest>(results, pageable, results.size());
	}

	@Override
	public Class<AuthorityRest> getDomainClass() {
		return AuthorityRest.class;
	}

	@Override
	public AuthorityResource wrapResource(AuthorityRest model, String... rels) {
		return new AuthorityResource(model, utils, rels);
	}

}
