/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.utils;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;

import java.util.List;

import org.dspace.app.rest.exception.PaginationException;
import org.dspace.app.rest.exception.RepositoryNotFoundException;
import org.dspace.app.rest.model.CommunityRest;
import org.dspace.app.rest.model.RestModel;
import org.dspace.app.rest.model.hateoas.DSpaceResource;
import org.dspace.app.rest.repository.DSpaceRestRepository;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.Link;
import org.springframework.stereotype.Component;

/**
 * Collection of utility methods
 * 
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 * 
 */
@Component
public class Utils {
	@Autowired
	ApplicationContext applicationContext;
	
	public <T> Page<T> getPage(List<T> fullContents, Pageable pageable) {
		int total = fullContents.size();
		List<T> pageContent = null;
		if (pageable.getOffset() > total) {
			throw new PaginationException(total);
		} else {
			if (pageable.getOffset() + pageable.getPageSize() > total) {
				pageContent = fullContents.subList(pageable.getOffset(), total);
			} else {
				pageContent = fullContents.subList(pageable.getOffset(), pageable.getOffset() + pageable.getPageSize());
			}
			return new PageImpl<T>(pageContent, pageable, total);
		}
	}

	public Link linkToSingleResource(DSpaceResource r, String rel) {
		RestModel data = r.getData();
		return linkToSingleResource(data, rel);
	}

	public Link linkToSingleResource(RestModel data, String rel) {
		return linkTo(data.getController(), data.getType()).slash(data).withRel(rel);
	}

	public Link linkToSubResource(RestModel data, String rel) {
		return linkTo(data.getController(), data.getType()).slash(data).slash(rel).withRel(rel);
	}

	public DSpaceRestRepository getResourceRepository(String modelPlural) {
		String model = makeSingular(modelPlural);
		try {
			return applicationContext.getBean(model, DSpaceRestRepository.class);
		} catch (NoSuchBeanDefinitionException e) {
			throw new RepositoryNotFoundException(model);
		}
	}
	
	public String[] getRepositories() {
		return applicationContext.getBeanNamesForType(DSpaceRestRepository.class);
	}
	
	public static String makeSingular(String modelPlural) {
		//The old dspace res package includes the evo inflection library which has a plural() function but no singular function
		if (modelPlural.equals("communities")) {
			return CommunityRest.NAME;
		}
		return modelPlural.replaceAll("s$", "");
	}

	public Link linkToSubResource(String baseUrl, String name) {
		return new Link(baseUrl + "/" + name).withRel(name);
	}
}