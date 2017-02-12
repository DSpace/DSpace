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
import java.util.UUID;

import org.dspace.app.rest.exception.PaginationException;
import org.dspace.app.rest.model.BitstreamRest;
import org.dspace.app.rest.model.RestModel;
import org.dspace.app.rest.model.hateoas.DSpaceResource;
import org.dspace.app.rest.repository.DSpaceRestRepository;
import org.dspace.app.rest.utils.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedResources;
import org.springframework.web.bind.annotation.PathVariable;
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
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */
@RestController
@RequestMapping("/api/core/{model}s")
@SuppressWarnings("rawtypes")
public class RestResourceController {
	@Autowired
	Utils utils;
	
	@RequestMapping(method = RequestMethod.GET, value = "/{uuid}")
	@SuppressWarnings("unchecked")
	DSpaceResource<BitstreamRest> findOne(@PathVariable String model, @PathVariable UUID uuid, @RequestParam(required=false) String projection) {
		DSpaceRestRepository repository = utils.getResourceRepository(model);
		RestModel modelObject = repository.findOne(uuid);
		DSpaceResource result = repository.wrapResource(modelObject);
		//Link link = entityLinks.linkFor(getResourceClass(model), model, uuid).withSelfRel();
//		Link link = linkTo(this.getClass(), model).slash(modelObject).withSelfRel();
//		result.add(link);
		return result;
	}

	@RequestMapping(method = RequestMethod.GET)
	@SuppressWarnings("unchecked")
	<T extends RestModel> PagedResources<DSpaceResource<T>> findAll(@PathVariable String model, Pageable page, PagedResourcesAssembler assembler, @RequestParam(required=false) String projection) {
		DSpaceRestRepository<T, ?> repository = utils.getResourceRepository(model);
//		Link link = entityLinks.linkFor(getResourceClass(model), model, page).withSelfRel();
		Link link = linkTo(this.getClass(), model).withSelfRel();
		
		Page<DSpaceResource<T>> resources;
		try {
			resources = repository.findAll(page).map(repository::wrapResource);
//			resources.forEach(r -> {
//				Link linkToSingleResource = Utils.linkToSingleResource(r, Link.REL_SELF);
//				r.add(linkToSingleResource);
//			});
		} catch (PaginationException pe) {
			resources = new PageImpl<DSpaceResource<T>>(new ArrayList<DSpaceResource<T>>(), page, pe.getTotal());
		}
		PagedResources<DSpaceResource<T>> result = assembler.toResource(resources, link);
		return result;
	}
}
