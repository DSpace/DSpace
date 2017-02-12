package org.dspace.app.rest;

import java.util.ArrayList;
import java.util.UUID;

import org.dspace.app.rest.exception.PaginationException;
import org.dspace.app.rest.exception.RepositoryNotFoundException;
import org.dspace.app.rest.model.BitstreamRest;
import org.dspace.app.rest.model.RestModel;
import org.dspace.app.rest.model.hateoas.DSpaceResource;
import org.dspace.app.rest.repository.BitstreamRestRepository;
import org.dspace.app.rest.repository.DSpaceRestRepository;
import org.dspace.app.rest.utils.Utils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityLinks;
import org.springframework.hateoas.ExposesResourceFor;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resources;
import org.springframework.hateoas.mvc.ControllerLinkBuilder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.*;

@RestController
//@ExposesResourceFor(BitstreamRest.class)
@RequestMapping("/api/core/{model}s")
@SuppressWarnings("rawtypes")
public class RestResourceController {

//	@Autowired
//	EntityLinks entityLinks;

	@Autowired
	ApplicationContext applicationContext;
	
	@RequestMapping(method = RequestMethod.GET, value = "/{uuid}")
	@SuppressWarnings("unchecked")
	DSpaceResource<BitstreamRest> findOne(@PathVariable String model, @PathVariable UUID uuid, @RequestParam(required=false) String projection) {
		DSpaceRestRepository repository = getResourceRepository(model);
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
		DSpaceRestRepository<T, ?> repository = getResourceRepository(model);
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

	private Class<?> getResourceClass(String model) {
		return getResourceRepository(model).getDomainClass();
	}

	private DSpaceRestRepository getResourceRepository(String model) {
		try {
			return applicationContext.getBean(model, DSpaceRestRepository.class);
		} catch (NoSuchBeanDefinitionException e) {
			throw new RepositoryNotFoundException(model);
		}
	}
}
