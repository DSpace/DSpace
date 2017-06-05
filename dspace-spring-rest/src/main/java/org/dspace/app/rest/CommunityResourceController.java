/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.dspace.app.rest.exception.PaginationException;
import org.dspace.app.rest.model.CommunityRest;
import org.dspace.app.rest.model.hateoas.CommunityResource;
import org.dspace.app.rest.model.hateoas.DSpaceResource;
import org.dspace.app.rest.repository.CommunityRestRepository;
import org.dspace.app.rest.repository.DSpaceRestRepository;
import org.dspace.app.rest.utils.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedResources;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * This is the main entry point of the new REST API. Its responsibility is to
 * provide a consistent behaviors for all the exposed resources in terms of
 * returned HTTP codes, endpoint URLs, HTTP verbs to methods translation, etc.
 * It delegates to the repository the business logic
 * 
 * @author Terry Brady (terry.brady at georgetown.edu)
 *
 */
@RestController
@RequestMapping("/api/core/communities")
@SuppressWarnings("rawtypes")
public class CommunityResourceController {
	@Autowired
	Utils utils;
	
	@SearchRestMethod
	@RequestMapping(method = RequestMethod.GET, value = "/search/top")
	@SuppressWarnings("unchecked")
	PagedResources<CommunityResource> findTopCommunies(Pageable page, PagedResourcesAssembler assembler, @RequestParam(required=false) String projection) {
		String model = CommunityRest.NAME;
		Link link = linkTo(this.getClass(), model).withSelfRel();
		DSpaceRestRepository<CommunityRest, ?> repository = utils.getResourceRepository(model);
		CommunityRestRepository commrepository = (CommunityRestRepository)repository;
		Page<DSpaceResource<CommunityRest>> resources;
		try {
			 resources =commrepository.findAllTop(page).map(repository::wrapResource);
		} catch (PaginationException pe) {
			resources = new PageImpl<DSpaceResource<CommunityRest>>(new ArrayList<DSpaceResource<CommunityRest>>(), page, pe.getTotal());
		}
		PagedResources<CommunityResource> result = assembler.toResource(resources, link);
		return result;
	}
}