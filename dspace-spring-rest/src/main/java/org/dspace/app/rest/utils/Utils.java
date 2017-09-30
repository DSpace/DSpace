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

import org.apache.commons.lang3.StringUtils;
import org.atteo.evo.inflector.English;
import org.dspace.app.rest.exception.PaginationException;
import org.dspace.app.rest.exception.RepositoryNotFoundException;
import org.dspace.app.rest.model.CommunityRest;
import org.dspace.app.rest.model.LinkRest;
import org.dspace.app.rest.model.LinksRest;
import org.dspace.app.rest.model.RestModel;
import org.dspace.app.rest.model.hateoas.DSpaceResource;
import org.dspace.app.rest.repository.DSpaceRestRepository;
import org.dspace.app.rest.repository.LinkRestRepository;
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
		return linkTo(data.getController(), data.getCategory(), English.plural(data.getType())).slash(data)
				.withRel(rel);
	}

	public Link linkToSubResource(RestModel data, String rel) {
		return linkToSubResource(data, rel, rel);
	}

	public Link linkToSubResource(RestModel data, String rel, String path) {
		return linkTo(data.getController(), data.getCategory(), English.plural(data.getType())).slash(data).slash(path)
				.withRel(rel);
	}

	public DSpaceRestRepository getResourceRepository(String apiCategory, String modelPlural) {
		String model = makeSingular(modelPlural);
		try {
			return applicationContext.getBean(apiCategory + "." + model, DSpaceRestRepository.class);
		} catch (NoSuchBeanDefinitionException e) {
			throw new RepositoryNotFoundException(apiCategory, model);
		}
	}

	public String[] getRepositories() {
		return applicationContext.getBeanNamesForType(DSpaceRestRepository.class);
	}

	public static String makeSingular(String modelPlural) {
		// The old dspace res package includes the evo inflection library which
		// has a plural() function but no singular function
		if (modelPlural.equals("communities")) {
			return CommunityRest.NAME;
		}
		return modelPlural.replaceAll("s$", "");
	}

	/**
	 * Retrieve the LinkRestRepository associated with a specific link from the
	 * apiCategory and model specified in the parameters.
	 * 
	 * @param apiCategory
	 *            the apiCategory
	 * @param modelPlural
	 *            the model name in its plural form
	 * @param rel
	 *            the name of the relation
	 * @return
	 */
	public LinkRestRepository getLinkResourceRepository(String apiCategory, String modelPlural, String rel) {
		String model = makeSingular(modelPlural);
		try {
			return applicationContext.getBean(apiCategory + "." + model + "." + rel, LinkRestRepository.class);
		} catch (NoSuchBeanDefinitionException e) {
			throw new RepositoryNotFoundException(apiCategory, model);
		}
	}

	/**
	 * 
	 * @param rel
	 * @param domainClass
	 * @return the LinkRest annotation corresponding to the specified rel in the
	 *         domainClass. Null if not found
	 */
	public LinkRest getLinkRest(String rel, Class<RestModel> domainClass) {
		LinkRest linkRest = null;
		LinksRest linksAnnotation = domainClass.getDeclaredAnnotation(LinksRest.class);
		if (linksAnnotation != null) {
			LinkRest[] links = linksAnnotation.links();
			for (LinkRest l : links) {
				if (StringUtils.equals(rel, l.name())) {
					linkRest = l;
					break;
				}
			}
		}
		return linkRest;
	}
}
