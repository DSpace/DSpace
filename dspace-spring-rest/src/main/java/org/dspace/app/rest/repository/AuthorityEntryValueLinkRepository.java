/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import javax.servlet.http.HttpServletRequest;

import org.dspace.app.rest.converter.AuthorityEntryRestConverter;
import org.dspace.app.rest.model.AuthorityEntryRest;
import org.dspace.app.rest.model.AuthorityRest;
import org.dspace.app.rest.model.hateoas.AuthorityEntryResource;
import org.dspace.app.rest.utils.AuthorityUtils;
import org.dspace.content.authority.Choice;
import org.dspace.content.authority.ChoiceAuthority;
import org.dspace.content.authority.service.ChoiceAuthorityService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.stereotype.Component;

/**
 * Controller for exposition of authority services
 * 
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science.it)
 *
 */
@Component(AuthorityRest.CATEGORY + "." + AuthorityRest.NAME + "." + AuthorityRest.ENTRY)
public class AuthorityEntryValueLinkRepository extends AbstractDSpaceRestRepository
		implements LinkRestRepository<AuthorityEntryRest> {

	@Autowired
	private ChoiceAuthorityService cas;

	@Autowired
	private AuthorityUtils authorityUtils;
	
	@Override
	public ResourceSupport wrapResource(AuthorityEntryRest model, String... rels) {
		return new AuthorityEntryResource(model);
	}
	
	public AuthorityEntryRest getResource(HttpServletRequest request, String name, String relId,
			Pageable pageable, String projection) {
		Context context = obtainContext();
		ChoiceAuthority choiceAuthority = cas.getChoiceAuthorityByAuthorityName(name);
		Choice choice = choiceAuthority.getChoice(null, relId, context.getCurrentLocale().toString());
		return authorityUtils.convertEntry(choice, name);
	}
	
}
