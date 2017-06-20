/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.atteo.evo.inflector.English;
import org.dspace.app.rest.exception.PaginationException;
import org.dspace.app.rest.exception.RepositoryNotFoundException;
import org.dspace.app.rest.model.LinkRest;
import org.dspace.app.rest.model.LinksRest;
import org.dspace.app.rest.model.RestModel;
import org.dspace.app.rest.model.hateoas.DSpaceResource;
import org.dspace.app.rest.model.hateoas.EmbeddedPage;
import org.dspace.app.rest.repository.DSpaceRestRepository;
import org.dspace.app.rest.repository.LinkRestRepository;
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
@RequestMapping("/api/{apiCategory}/{model}")
@SuppressWarnings("rawtypes")
public class RestResourceController implements InitializingBean {
	@Autowired
	DiscoverableEndpointsService discoverableEndpointsService;

	@Autowired
	Utils utils;

	@Override
	public void afterPropertiesSet() {
		List<Link> links = new ArrayList<Link>();
		for (String r : utils.getRepositories()) {
			// this doesn't work as we don't have an active http request
			// see https://github.com/spring-projects/spring-hateoas/issues/408
			// Link l = linkTo(this.getClass(), r).withRel(r);
			String[] split = r.split("\\.", 2);
			String plural = English.plural(split[1]);
			Link l = new Link("/api/" + split[0] + "/" + plural, plural);
			links.add(l);
			System.out.println(l.getRel() + " " + l.getHref());
		}
		discoverableEndpointsService.register(this, links);
	}
	
	@RequestMapping(method = RequestMethod.GET, value = "/{id:\\d+}")
	@SuppressWarnings("unchecked")
	public DSpaceResource<RestModel> findOne(@PathVariable String apiCategory, @PathVariable String model,
			@PathVariable Integer id, @RequestParam(required = false) String projection) {
		return findOneInternal(apiCategory, model, id, projection);
	}

	@RequestMapping(method = RequestMethod.GET, value = "/{id:[A-z0-9]+}")
	@SuppressWarnings("unchecked")
	public DSpaceResource<RestModel> findOne(@PathVariable String apiCategory, @PathVariable String model,
			@PathVariable String id, @RequestParam(required = false) String projection) {
		return findOneInternal(apiCategory, model, id, projection);
	}

	@RequestMapping(method = RequestMethod.GET, value = "/{uuid:[0-9a-fxA-FX]{8}-[0-9a-fxA-FX]{4}-[0-9a-fxA-FX]{4}-[0-9a-fxA-FX]{4}-[0-9a-fxA-FX]{12}}")
	@SuppressWarnings("unchecked")
	public DSpaceResource<RestModel> findOne(@PathVariable String apiCategory, @PathVariable String model,
			@PathVariable UUID uuid, @RequestParam(required = false) String projection) {
		return findOneInternal(apiCategory, model, uuid, projection);
	}

	private <ID extends Serializable> DSpaceResource<RestModel> findOneInternal(String apiCategory, String model, ID id,
			String projection) {
		checkModelPluralForm(apiCategory, model);
		DSpaceRestRepository<RestModel, ID> repository = utils.getResourceRepository(apiCategory, model);
		RestModel modelObject = null;
		try {
			modelObject = repository.findOne(id);
		} catch (ClassCastException e) {
		}
		if (modelObject == null) {
			throw new ResourceNotFoundException(apiCategory + "." + model + " with id: " + id + " not found");
		}
		DSpaceResource result = repository.wrapResource(modelObject);
		return result;
	}

	@RequestMapping(method = RequestMethod.GET, value = "/{id:\\d+}/{rel}")
	public ResourceSupport findRel(HttpServletRequest request, @PathVariable String apiCategory, @PathVariable String model, @PathVariable Integer id,
			@PathVariable String rel, Pageable page, PagedResourcesAssembler assembler,
			@RequestParam(required = false) String projection) {
		return findRelInternal(request, apiCategory, model, id, rel, page, assembler, projection);
	}
	
	@RequestMapping(method = RequestMethod.GET, value = "/{id:[A-z0-9]+}/{rel}")
	public ResourceSupport findRel(HttpServletRequest request,  @PathVariable String apiCategory, @PathVariable String model, @PathVariable String id,
			@PathVariable String rel, Pageable page, PagedResourcesAssembler assembler,
			@RequestParam(required = false) String projection) {
		return findRelInternal(request, apiCategory, model, id, rel, page, assembler, projection);
	}

	@RequestMapping(method = RequestMethod.GET, value = "/{uuid:[0-9a-fxA-FX]{8}-[0-9a-fxA-FX]{4}-[0-9a-fxA-FX]{4}-[0-9a-fxA-FX]{4}-[0-9a-fxA-FX]{12}}/{rel}")
	public ResourceSupport findRel(HttpServletRequest request, @PathVariable String apiCategory, @PathVariable String model, @PathVariable UUID uuid,
			@PathVariable String rel, Pageable page, PagedResourcesAssembler assembler,
			@RequestParam(required = false) String projection) {
		return findRelInternal(request, apiCategory, model, uuid, rel, page, assembler, projection);
	}

	private <ID extends Serializable> ResourceSupport findRelInternal(HttpServletRequest request, String apiCategory, String model, ID uuid,
			String rel, Pageable page, PagedResourcesAssembler assembler, String projection) {
		checkModelPluralForm(apiCategory, model);
		DSpaceRestRepository<RestModel, ID> repository = utils.getResourceRepository(apiCategory, model);
		
		LinksRest linksAnnotation = repository.getDomainClass().getDeclaredAnnotation(LinksRest.class);
		if (linksAnnotation != null) {
			LinkRest[] links = linksAnnotation.links();
			for (LinkRest l : links) {
				if (StringUtils.equals(rel, l.name())) {
					LinkRestRepository linkRepository = utils.getLinkResourceRepository(apiCategory, model, rel);
					Method[] methods = linkRepository.getClass().getMethods();
					for (Method m : methods) { 
						if (StringUtils.equals(m.getName(), l.method())) {
							try {
								Page<? extends Serializable> pageResult = (Page<? extends RestModel>) m.invoke(linkRepository, request, uuid, page, projection);
								Link link = linkTo(this.getClass(), apiCategory, English.plural(model)).slash(uuid).slash(rel).withSelfRel();
								PagedResources<? extends ResourceSupport> result = assembler.toResource(pageResult.map(linkRepository::wrapResource), link);
								return result;
							} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
								throw new RuntimeException(e.getMessage(), e);
							}
						}
					}
					// TODO custom exception
					throw new RuntimeException("Method for relation " + rel + " not found: " + l.name());
				}
			}
		}
		
		RestModel modelObject = repository.findOne(uuid);
		DSpaceResource result = repository.wrapResource(modelObject, rel);
		if (result.getLink(rel) == null) {
			//TODO create a custom exception
			throw new ResourceNotFoundException(rel + "undefined for "+ model);
		}
		else if (result.getEmbedded().get(rel) instanceof EmbeddedPage){
			// this is a very inefficient scenario. We have an embedded list
			// already fully retrieved that we need to limit with pagination
			// parameter. BTW change the default sorting is not implemented at
			// the current stage and could be overcompex to implement
			// if we really want to implement pagination we should implement a
			// link repository so to fall in the previous block code
			EmbeddedPage ep = (EmbeddedPage) result.getEmbedded().get(rel);
			List<? extends RestModel> fullList = ep.getFullList();
			if (fullList == null || fullList.size() == 0) return null;
			int start = page.getOffset();
			int end = (start + page.getPageSize()) > fullList.size() ? fullList.size() : (start + page.getPageSize());
			DSpaceRestRepository<RestModel, ?> resourceRepository = utils.getResourceRepository(fullList.get(0).getCategory(), fullList.get(0).getType());
			PageImpl<RestModel> pageResult = new PageImpl(fullList.subList(start, end), page, fullList.size());
			return assembler.toResource(pageResult	.map(resourceRepository::wrapResource));
		}
		else {
			ResourceSupport resu = (ResourceSupport) result.getEmbedded().get(rel);
			return resu;
		}
	}

	@RequestMapping(method = RequestMethod.GET)
	@SuppressWarnings("unchecked")
	public <T extends RestModel> PagedResources<DSpaceResource<T>> findAll(@PathVariable String apiCategory,
			@PathVariable String model, Pageable page, PagedResourcesAssembler assembler,
			@RequestParam(required = false) String projection) {
		DSpaceRestRepository<T, ?> repository = utils.getResourceRepository(apiCategory, model);
		Link link = linkTo(methodOn(this.getClass(), apiCategory, English.plural(model)).findAll(apiCategory, model, page, assembler, projection)).withSelfRel();

		Page<DSpaceResource<T>> resources;
		try {
			resources = repository.findAll(page).map(repository::wrapResource);
		} catch (PaginationException pe) {
			resources = new PageImpl<DSpaceResource<T>>(new ArrayList<DSpaceResource<T>>(), page, pe.getTotal());
		}
		PagedResources<DSpaceResource<T>> result = assembler.toResource(resources, link);
		return result;
	}

	/**
	 * Check that the model is specified in its plural form, otherwise throw a RepositoryNotFound exception
	 *  
	 * @param model
	 */
	private void checkModelPluralForm(String apiCategory, String model) {
		if (StringUtils.equals(utils.makeSingular(model), model)) {
			throw new RepositoryNotFoundException(apiCategory, model);
		}
	}
}
