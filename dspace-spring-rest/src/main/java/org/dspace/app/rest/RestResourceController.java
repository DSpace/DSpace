/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.springframework.data.rest.webmvc.ControllerUtils.EMPTY_RESOURCE_LIST;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.atteo.evo.inflector.English;
import org.dspace.app.rest.exception.PaginationException;
import org.dspace.app.rest.exception.RepositoryNotFoundException;
import org.dspace.app.rest.exception.RepositorySearchMethodNotFoundException;
import org.dspace.app.rest.exception.RepositorySearchNotFoundException;
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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.MethodParameter;
import org.springframework.core.convert.ConversionException;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.support.QueryMethodParameterConversionException;
import org.springframework.data.repository.support.RepositoryInvoker;
import org.springframework.data.rest.core.mapping.ResourceMetadata;
import org.springframework.data.rest.webmvc.PersistentEntityResourceAssembler;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.data.rest.webmvc.RootResourceInformation;
import org.springframework.data.rest.webmvc.support.DefaultedPageable;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.TypeInformation;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.hateoas.Resources;
import org.springframework.hateoas.core.AnnotationAttribute;
import org.springframework.hateoas.core.MethodParameters;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
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
	private static final AnnotationAttribute PARAM_ANNOTATION = new AnnotationAttribute(Param.class);
	private static final String NAME_NOT_FOUND = "Unable to detect parameter names for query method %s! Use @Param or compile with -parameters on JDK 8.";
	@Autowired(required=true)
	@Qualifier(value="mvcConversionService")
	private ConversionService conversionService;
	
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
		if (haveSearchMethods(repository)) {
			result.add(linkTo(this.getClass(), apiCategory, model).slash("search").withRel("search"));
		}
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
	
	@RequestMapping(method = RequestMethod.GET, value="/search")
	ResourceSupport listSearchMethods(@PathVariable String apiCategory, @PathVariable String model) {
		ResourceSupport root = new ResourceSupport();
		DSpaceRestRepository repository = utils.getResourceRepository(apiCategory, model);
		boolean searchEnabled = false;
		for (Method method : repository.getClass().getMethods()) {
			SearchRestMethod ann = method.getAnnotation(SearchRestMethod.class);
			if (ann != null) {
				String name = ann.name();
				if (name.isEmpty()) {
					name = method.getName();
				}
				Link link = linkTo(this.getClass(), apiCategory, model).slash("search").slash(name).withRel(name);
				root.add(link);
				searchEnabled = true;
			}
		}
		if (!searchEnabled) {
			throw new RepositorySearchNotFoundException(model);
		}
		return root;
	}
	
	
	@RequestMapping(method = RequestMethod.GET, value="/search/{searchMethod}")
	@SuppressWarnings("unchecked")
	<T extends RestModel> PagedResources<DSpaceResource<T>> executeSearchMethods(@PathVariable String apiCategory, 
			@PathVariable String model, @PathVariable String searchMethod, Pageable pageable, Sort sort, PagedResourcesAssembler assembler, 
			@RequestParam MultiValueMap<String, Object> parameters) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
			
		Link link = linkTo(this.getClass(), apiCategory, model).slash("search").slash(searchMethod).withSelfRel();
		DSpaceRestRepository repository = utils.getResourceRepository(apiCategory, model);
		Page<DSpaceResource<T>> resources = null;
		boolean searchEnabled = false;
		boolean searchMethodFound = false;
		for (Method method : repository.getClass().getMethods()) {
			SearchRestMethod ann = method.getAnnotation(SearchRestMethod.class);
			if (ann != null) {
				searchEnabled = true;
				String name = ann.name();
				if (name.isEmpty()) {
					name = method.getName();
				}
				if (StringUtils.equals(name, searchMethod)) {
					searchMethodFound = true;
					resources = ((Page<T>) executeQueryMethod(repository, parameters, method, pageable, sort, assembler)).map(repository::wrapResource);
					break;
				}
			}
		}
		if (!searchMethodFound && searchEnabled) {
			throw new RepositorySearchMethodNotFoundException(model, searchMethod);
		}
		if (!searchEnabled) {
			throw new RepositorySearchNotFoundException(model);
		}
		PagedResources<DSpaceResource<T>> result = assembler.toResource(resources, link);
		return result;
	}
	
	private boolean haveSearchMethods(DSpaceRestRepository repository) {
		for (Method method : repository.getClass().getMethods()) {
			SearchRestMethod ann = method.getAnnotation(SearchRestMethod.class);
			if (ann != null) {
				return true;
			}
		}
		return false;
	}
	
	/*
	 * Adapted from org.springframework.data.rest.webmvc.RepositorySearchController.executeQueryMethod(RepositoryInvoker, MultiValueMap<String, Object>, Method, DefaultedPageable, Sort, PersistentEntityResourceAssembler)
	 */
	private Object executeQueryMethod(DSpaceRestRepository repository,
			MultiValueMap<String, Object> parameters, Method method, Pageable pageable, Sort sort,
			PagedResourcesAssembler assembler) {

		MultiValueMap<String, Object> result = new LinkedMultiValueMap<String, Object>(parameters);
		MethodParameters methodParameters = new MethodParameters(method, new AnnotationAttribute(Param.class));

		for (Entry<String, List<Object>> entry : parameters.entrySet()) {

			MethodParameter parameter = methodParameters.getParameter(entry.getKey());

			if (parameter == null) {
				continue;
			}

			result.put(parameter.getParameterName(), entry.getValue());
		}

		return invokeQueryMethod(repository, method, result, pageable, sort);
	}
	
	/*
	 * Adapted from org.springframework.data.repository.support.ReflectionRepositoryInvoker.invokeQueryMethod(Method, MultiValueMap<String, ? extends Object>, Pageable, Sort)
	 */
	public Object invokeQueryMethod(DSpaceRestRepository repository, Method method, MultiValueMap<String, ? extends Object> parameters, Pageable pageable,
			Sort sort) {

		Assert.notNull(method, "Method must not be null!");
		Assert.notNull(parameters, "Parameters must not be null!");

		ReflectionUtils.makeAccessible(method);

		return ReflectionUtils.invokeMethod(method, repository, prepareParameters(method, parameters, pageable, sort));
	}

	/*
	 * Taken from org.springframework.data.repository.support.ReflectionRepositoryInvoker.prepareParameters(Method, MultiValueMap<String, ? extends Object>, Pageable, Sort)
	 */
	private Object[] prepareParameters(Method method, MultiValueMap<String, ? extends Object> rawParameters,
			Pageable pageable, Sort sort) {

		List<MethodParameter> parameters = new MethodParameters(method, PARAM_ANNOTATION).getParameters();

		if (parameters.isEmpty()) {
			return new Object[0];
		}

		Object[] result = new Object[parameters.size()];
		Sort sortToUse = pageable == null ? sort : pageable.getSort();

		for (int i = 0; i < result.length; i++) {

			MethodParameter param = parameters.get(i);
			Class<?> targetType = param.getParameterType();

			if (Pageable.class.isAssignableFrom(targetType)) {
				result[i] = pageable;
			} else if (Sort.class.isAssignableFrom(targetType)) {
				result[i] = sortToUse;
			} else {

				String parameterName = param.getParameterName();

				if (!StringUtils.isNotBlank(parameterName)) {
					throw new IllegalArgumentException(String.format(NAME_NOT_FOUND, ClassUtils.getQualifiedMethodName(method)));
				}

				Object value = unwrapSingleElement(rawParameters.get(parameterName));

				result[i] = targetType.isInstance(value) ? value : convert(value, param);
			}
		}

		return result;
	}
	
	/**
	 * Unwraps the first item if the given source has exactly one element. Taken from
	 * org.springframework.data.repository.support.ReflectionRepositoryInvoker.unwrapSingleElement(List<? extends Object>)
	 * 
	 * @param source can be {@literal null}.
	 * @return
	 */
	private static Object unwrapSingleElement(List<? extends Object> source) {
		return source == null ? null : source.size() == 1 ? source.get(0) : source;
	}
	
	/**
	 * Taken from org.springframework.data.repository.support.ReflectionRepositoryInvoker.convert(Object, MethodParameter)
	 * @param value
	 * @param parameter
	 * @return
	 */
	private Object convert(Object value, MethodParameter parameter) {

		try {
			return conversionService.convert(value, TypeDescriptor.forObject(value), new TypeDescriptor(parameter));
		} catch (ConversionException o_O) {
			throw new QueryMethodParameterConversionException(value, parameter, o_O);
		}
	}
	
}
