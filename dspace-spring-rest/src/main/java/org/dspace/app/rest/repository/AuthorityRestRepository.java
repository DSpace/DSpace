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

import org.dspace.app.rest.model.AuthorityRest;
import org.dspace.app.rest.model.hateoas.AuthorityResource;
import org.dspace.app.rest.utils.AuthorityUtils;
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
	private AuthorityUtils authorityUtils;
	
	@Override
	public AuthorityRest findOne(Context context, String name) {
		AuthorityRest authorityRest = authorityUtils.getAuthority(name);
		if(authorityRest == null) {
			return null;
		}
		return authorityRest;
	}

	@Override
	public Page<AuthorityRest> findAll(Context context, Pageable pageable) {
		List<AuthorityRest> authorities = authorityUtils.getAuthorities();
		return new PageImpl<AuthorityRest>(authorities, pageable, authorities.size());
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
