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

import org.atteo.evo.inflector.English;
import org.dspace.app.rest.exception.PaginationException;
import org.dspace.app.rest.exception.RepositoryNotFoundException;
import org.dspace.app.rest.model.BitstreamRest;
import org.dspace.app.rest.model.RestModel;
import org.dspace.app.rest.model.hateoas.DSpaceResource;
import org.dspace.app.rest.repository.DSpaceRestRepository;
import org.dspace.app.rest.utils.Utils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.hateoas.core.EvoInflectorRelProvider;
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
@RequestMapping("/api/core/{model}")
@SuppressWarnings("rawtypes")
public class RestResourceController implements InitializingBean {
	@Autowired
	DiscoverableEndpointsService discoverableEndpointsService;
	
	@Autowired
	Utils utils;

	@Override
	public void afterPropertiesSet()  {
		List<Link> links = new ArrayList<Link>();
		for (String r : utils.getRepositories()) {
			// this doesn't work as we don't have an active http request
			// see https://github.com/spring-projects/spring-hateoas/issues/408
			// Link l = linkTo(this.getClass(), r).withRel(r);
			String plural = English.plural(r);
			Link l = new Link("/api/core/" + plural, plural);
			links.add(l);
			System.out.println(l.getRel() + " " + l.getHref());
		}
		discoverableEndpointsService.register(this, links);
	}
	
	@RequestMapping(method = RequestMethod.GET, value = "/{id:\\d+}")
	@SuppressWarnings("unchecked")
	DSpaceResource<RestModel> findOne(@PathVariable String model, @PathVariable Integer id, @RequestParam(required=false) String projection) {
		return findOneInternal(model, id, projection);
	}
	
	@RequestMapping(method = RequestMethod.GET, value = "/{uuid:[0-9a-fxA-FX]{8}-[0-9a-fxA-FX]{4}-[0-9a-fxA-FX]{4}-[0-9a-fxA-FX]{4}-[0-9a-fxA-FX]{12}}")
	@SuppressWarnings("unchecked")
	DSpaceResource<RestModel> findOne(@PathVariable String model, @PathVariable UUID uuid, @RequestParam(required=false) String projection) {
		return findOneInternal(model, uuid, projection);
	}
	
	private <ID extends Serializable> DSpaceResource<RestModel> findOneInternal(String model, ID id, String projection) {
		DSpaceRestRepository<RestModel, ID> repository = utils.getResourceRepository(model);
		RestModel modelObject = null;
		try {
			modelObject = repository.findOne(id, projection);
		} catch (ClassCastException e) {
		}
		if (modelObject == null) {
			throw new ResourceNotFoundException(model + " with id: " + id + " not found");
		}
		DSpaceResource result = repository.wrapResource(modelObject);
		return result;
	}

	@RequestMapping(method = RequestMethod.GET, value = "/{id:\\d+}/{rel}")
	ResourceSupport findRel(@PathVariable String model, @PathVariable Integer id, @PathVariable String rel, @RequestParam(required=false) String projection) {
		return findRelInternal(model, id, rel, projection);
	}
	
	@RequestMapping(method = RequestMethod.GET, value = "/{uuid:[0-9a-fxA-FX]{8}-[0-9a-fxA-FX]{4}-[0-9a-fxA-FX]{4}-[0-9a-fxA-FX]{4}-[0-9a-fxA-FX]{12}}/{rel}")
	ResourceSupport findRel(@PathVariable String model, @PathVariable UUID uuid, @PathVariable String rel, @RequestParam(required=false) String projection) {
		return findRelInternal(model, uuid, rel, projection);
	}
	
	private <ID extends Serializable> ResourceSupport findRelInternal(String model, ID uuid, String rel, String projection) {
		// FIXME this is a very bad implementation as it leads most of times to
		// more round-trip on the database and retrieval of unneeded infromation
		DSpaceRestRepository<RestModel, ID> repository = utils.getResourceRepository(model);
		RestModel modelObject = repository.findOne(uuid, projection);
		DSpaceResource result = repository.wrapResource(modelObject, rel);
		if (result.getLink(rel) == null) {
			//TODO create a custom exception
			throw new ResourceNotFoundException(rel + "undefined for "+ model);
		}
		
		ResourceSupport resu = (ResourceSupport) result.getEmbedded().get(rel);
		return resu;
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
