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

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.dspace.app.rest.model.AuthorityEntryRest;
import org.dspace.app.rest.model.AuthorityRest;
import org.dspace.app.rest.model.QueryObject;
import org.dspace.app.rest.model.hateoas.AuthorityEntryResource;
import org.dspace.app.rest.utils.AuthorityUtils;
import org.dspace.content.Collection;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.stereotype.Component;

/**
 * Controller for exposition of authority services
 * 
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science.it)
 *
 */
@Component(AuthorityRest.CATEGORY + "." + AuthorityRest.NAME + "." + AuthorityRest.ENTRIES)
public class AuthorityEntryLinkRepository extends AbstractDSpaceRestRepository
		implements LinkRestRepository<AuthorityEntryRest> {

	@Autowired
	private AuthorityUtils authorityUtils;

	CollectionService cs = ContentServiceFactory.getInstance().getCollectionService();
	
	@Override
	public ResourceSupport wrapResource(AuthorityEntryRest model, String... rels) {
		return new AuthorityEntryResource(model);
	}

	public Page<AuthorityEntryRest> listAuthorityEntries(HttpServletRequest request, String name, 
			Pageable pageable, String projection) {
		Context context = obtainContext();
		String query = request.getParameter("query");
		String metadata = request.getParameter("metadata");
		String uuidCollectìon = request.getParameter("uuid");
		Collection collection = null;
		if(StringUtils.isNotBlank(uuidCollectìon)) {
			try {
				collection = cs.find(context, UUID.fromString(uuidCollectìon));
			} catch (SQLException e) {
				new RuntimeException(e);
			}
		}
		List<AuthorityEntryRest> authorities = authorityUtils.query(metadata, query, collection, pageable.getOffset(), pageable.getPageSize(), context.getCurrentLocale());
		return new PageImpl<AuthorityEntryRest>(authorities, pageable, authorities.size());
	}
	
}
