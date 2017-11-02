/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.dspace.app.rest.converter.SiteConverter;
import org.dspace.app.rest.model.SiteRest;
import org.dspace.app.rest.model.hateoas.SiteResource;
import org.dspace.content.Site;
import org.dspace.content.service.SiteService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

/**
 * This is the repository responsible to manage Item Rest object
 * 
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */

@Component(SiteRest.CATEGORY + "." + SiteRest.NAME)
public class SiteRestRepository extends DSpaceRestRepository<SiteRest, UUID> {

	@Autowired
	SiteService sitesv;

	@Autowired
	SiteConverter converter;
	
	
	public SiteRestRepository() {
	}

	@Override
	public SiteRest findOne(Context context, UUID id) {
		Site site = null;
		try {
			site = sitesv.find(context, id);
		} catch (SQLException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
		if (site == null) {
			return null;
		}
		return converter.fromModel(site);
	}

	@Override
	public Page<SiteRest> findAll(Context context, Pageable pageable) {
		List<Site> sites = new ArrayList<Site>();
		int total = 1;
		try {
			sites.add(sitesv.findSite(context));
		} catch (SQLException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
		Page<SiteRest> page = new PageImpl<Site>(sites, pageable, total).map(converter);
		return page;
	}
	
	@Override
	public Class<SiteRest> getDomainClass() {
		return SiteRest.class;
	}
	
	@Override
	public SiteResource wrapResource(SiteRest site, String... rels) {
		return new SiteResource(site, utils, rels);
	}

}