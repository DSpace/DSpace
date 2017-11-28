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

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.atteo.evo.inflector.English;
import org.dspace.app.rest.exception.PaginationException;
import org.dspace.app.rest.exception.RepositoryMethodNotImplementedException;
import org.dspace.app.rest.exception.RepositoryNotFoundException;
import org.dspace.app.rest.exception.RepositorySearchMethodNotFoundException;
import org.dspace.app.rest.exception.RepositorySearchNotFoundException;
import org.dspace.app.rest.model.DirectlyAddressableRestModel;
import org.dspace.app.rest.model.LinkRest;
import org.dspace.app.rest.model.RestModel;
import org.dspace.app.rest.model.hateoas.DSpaceResource;
import org.dspace.app.rest.model.hateoas.EmbeddedPage;
import org.dspace.app.rest.model.step.UploadStatusResponse;
import org.dspace.app.rest.repository.DSpaceRestRepository;
import org.dspace.app.rest.repository.LinkRestRepository;
import org.dspace.app.rest.utils.RestRepositoryUtils;
import org.dspace.app.rest.utils.Utils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.rest.webmvc.ControllerUtils;
import org.springframework.data.rest.webmvc.PersistentEntityResourceAssembler;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.data.rest.webmvc.json.patch.JsonPatchPatchConverter;
import org.springframework.data.rest.webmvc.json.patch.Patch;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.hateoas.UriTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

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
	
	private static final Logger log = Logger.getLogger(RestResourceController.class);
	
	@Autowired
	DiscoverableEndpointsService discoverableEndpointsService;

	@Autowired
	Utils utils;

	@Autowired
	RestRepositoryUtils repositoryUtils;
	
	@Autowired
	private ObjectMapper mapper;
	 
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
	public DSpaceResource<DirectlyAddressableRestModel> findOne(@PathVariable String apiCategory, @PathVariable String model,
			@PathVariable Integer id, @RequestParam(required = false) String projection) {
		return findOneInternal(apiCategory, model, id, projection);
	}

	@RequestMapping(method = RequestMethod.GET, value = "/{id:^(?!^\\d+$)(?!^[0-9a-fxA-FX]{8}-[0-9a-fxA-FX]{4}-[0-9a-fxA-FX]{4}-[0-9a-fxA-FX]{4}-[0-9a-fxA-FX]{12}$)[\\w+\\-]+$+}")
	@SuppressWarnings("unchecked")
	public DSpaceResource<DirectlyAddressableRestModel> findOne(@PathVariable String apiCategory, @PathVariable String model,
			@PathVariable String id, @RequestParam(required = false) String projection) {
		return findOneInternal(apiCategory, model, id, projection);
	}

	@RequestMapping(method = RequestMethod.GET, value = "/{uuid:[0-9a-fxA-FX]{8}-[0-9a-fxA-FX]{4}-[0-9a-fxA-FX]{4}-[0-9a-fxA-FX]{4}-[0-9a-fxA-FX]{12}}")
	@SuppressWarnings("unchecked")
	public DSpaceResource<DirectlyAddressableRestModel> findOne(@PathVariable String apiCategory, @PathVariable String model,
			@PathVariable UUID uuid, @RequestParam(required = false) String projection) {
		return findOneInternal(apiCategory, model, uuid, projection);
	}

	private <ID extends Serializable> DSpaceResource<DirectlyAddressableRestModel> findOneInternal(String apiCategory, String model, ID id,
			String projection) {
		checkModelPluralForm(apiCategory, model);
		DSpaceRestRepository<DirectlyAddressableRestModel, ID> repository = utils.getResourceRepository(apiCategory, model);
		DirectlyAddressableRestModel modelObject = null;
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
	public ResourceSupport findRel(HttpServletRequest request, @PathVariable String apiCategory,
			@PathVariable String model, @PathVariable Integer id, @PathVariable String rel, Pageable page,
			PagedResourcesAssembler assembler, @RequestParam(required = false) String projection) {
		return findRelInternal(request, apiCategory, model, id, rel, page, assembler, projection);
	}

	@RequestMapping(method = RequestMethod.GET, value = "/{id:^(?!^\\d+$)(?!^[0-9a-fxA-FX]{8}-[0-9a-fxA-FX]{4}-[0-9a-fxA-FX]{4}-[0-9a-fxA-FX]{4}-[0-9a-fxA-FX]{12}$)[\\w+\\-]+$}/{rel}")
	public ResourceSupport findRel(HttpServletRequest request, @PathVariable String apiCategory,
			@PathVariable String model, @PathVariable String id, @PathVariable String rel, Pageable page,
			PagedResourcesAssembler assembler, @RequestParam(required = false) String projection) {
		return findRelInternal(request, apiCategory, model, id, rel, page, assembler, projection);
	}

	@RequestMapping(method = RequestMethod.GET, value = "/{uuid:[0-9a-fxA-FX]{8}-[0-9a-fxA-FX]{4}-[0-9a-fxA-FX]{4}-[0-9a-fxA-FX]{4}-[0-9a-fxA-FX]{12}}/{rel}")
	public ResourceSupport findRel(HttpServletRequest request, @PathVariable String apiCategory,
			@PathVariable String model, @PathVariable UUID uuid, @PathVariable String rel, Pageable page,
			PagedResourcesAssembler assembler, @RequestParam(required = false) String projection) {
		return findRelInternal(request, apiCategory, model, uuid, rel, page, assembler, projection);
	}

	@RequestMapping(method = RequestMethod.GET, value = "/{id:^(?!^\\d+$)(?!^[0-9a-fxA-FX]{8}-[0-9a-fxA-FX]{4}-[0-9a-fxA-FX]{4}-[0-9a-fxA-FX]{4}-[0-9a-fxA-FX]{12}$)[\\w+\\-]+$}/{rel}/{relid:[\\w+\\-]+}")
	public ResourceSupport findRel(HttpServletRequest request, @PathVariable String apiCategory,
			@PathVariable String model, @PathVariable String id, @PathVariable String rel, @PathVariable String relid,
			Pageable page, PagedResourcesAssembler assembler, @RequestParam(required = false) String projection) {
		return findRelEntryInternal(request, apiCategory, model, id, rel, relid, page, assembler, projection);		
	}

	
	@RequestMapping(method = RequestMethod.POST)
	public ResponseEntity<ResourceSupport> post(HttpServletRequest request, @PathVariable String apiCategory,
			@PathVariable String model) throws HttpRequestMethodNotSupportedException {		
		return postInternal(request, apiCategory, model);
	}
	
	public <ID extends Serializable> ResponseEntity<ResourceSupport> postInternal(HttpServletRequest request, String apiCategory,
			String model) throws HttpRequestMethodNotSupportedException {
		checkModelPluralForm(apiCategory, model);
		DSpaceRestRepository<DirectlyAddressableRestModel, ID> repository = utils.getResourceRepository(apiCategory, model);
		DirectlyAddressableRestModel modelObject = null;
		try {
			modelObject = repository.createAndReturn();
		} catch (ClassCastException e) {
			log.error(e.getMessage(), e);
			return ControllerUtils.toEmptyResponse(HttpStatus.INTERNAL_SERVER_ERROR);
		}
		if (modelObject == null) {
			throw new HttpRequestMethodNotSupportedException(RequestMethod.POST.toString());
		}
		DSpaceResource result = repository.wrapResource(modelObject);
		//TODO manage HTTPHeader
		return ControllerUtils.toResponseEntity(HttpStatus.CREATED, null, result);		
	}	

	@RequestMapping(method = RequestMethod.POST, value = "/{id:\\d+}" , headers = "content-type=multipart/form-data")
	public <ID extends Serializable> ResponseEntity<ResourceSupport> upload(HttpServletRequest request,
			@PathVariable String apiCategory, @PathVariable String model, @PathVariable Integer id,
			@RequestParam(required=false, value="extraField") String extraField,
		    @RequestParam("file") MultipartFile uploadfile) throws HttpRequestMethodNotSupportedException {
		return uploadInternal(request, apiCategory, model, id, extraField, uploadfile);
	}
	
	@RequestMapping(method = RequestMethod.POST, value = "/{uuid:[0-9a-fxA-FX]{8}-[0-9a-fxA-FX]{4}-[0-9a-fxA-FX]{4}-[0-9a-fxA-FX]{4}-[0-9a-fxA-FX]{12}}", headers = "content-type=multipart/form-data")	
	public <ID extends Serializable> ResponseEntity<ResourceSupport> upload(HttpServletRequest request,
			@PathVariable String apiCategory, @PathVariable String model, @PathVariable UUID id,
			@RequestParam(required=false, value="extraField") String extraField,
		    @RequestParam("file") MultipartFile uploadfile) throws HttpRequestMethodNotSupportedException {
		return uploadInternal(request, apiCategory, model, id, extraField, uploadfile);
	}
    
	private <ID extends Serializable, U extends UploadStatusResponse> ResponseEntity<ResourceSupport> uploadInternal(HttpServletRequest request, String apiCategory, String model, ID id,
			String extraField, MultipartFile uploadfile) {
		checkModelPluralForm(apiCategory, model);
		DSpaceRestRepository<DirectlyAddressableRestModel, ID> repository = utils.getResourceRepository(apiCategory, model);
		
		U result = null;
		try {
			result = repository.upload(request, apiCategory, model, id, extraField, uploadfile);
			if(result.isStatus()) {
				return ControllerUtils.toResponseEntity(HttpStatus.CREATED, null, new Resource(result));
			}
			else {
				return ControllerUtils.toResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR, null, new Resource(result));
			}				
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return ControllerUtils.toEmptyResponse(HttpStatus.INTERNAL_SERVER_ERROR);
	}	
	
	@RequestMapping(method = RequestMethod.PATCH, value = "/{id:\\d+}")
	public ResponseEntity<ResourceSupport> patch(HttpServletRequest request, @PathVariable String apiCategory,
			@PathVariable String model, @PathVariable Integer id, @RequestBody(required = true) JsonNode jsonNode) throws HttpRequestMethodNotSupportedException {
		return patchInternal(request, apiCategory, model, id, jsonNode);
	}

	@RequestMapping(method = RequestMethod.PATCH, value = "/{uuid:[0-9a-fxA-FX]{8}-[0-9a-fxA-FX]{4}-[0-9a-fxA-FX]{4}-[0-9a-fxA-FX]{4}-[0-9a-fxA-FX]{12}}")
	public ResponseEntity<ResourceSupport> patch(HttpServletRequest request, @PathVariable String apiCategory,
			@PathVariable String model, @PathVariable UUID id, @RequestBody(required = true) JsonNode jsonNode) throws HttpRequestMethodNotSupportedException {
		return patchInternal(request, apiCategory, model, id, jsonNode);
	}
	
	public <ID extends Serializable> ResponseEntity<ResourceSupport> patchInternal(HttpServletRequest request, String apiCategory,
			String model, ID id, JsonNode jsonNode) throws HttpRequestMethodNotSupportedException {
		
		checkModelPluralForm(apiCategory, model);
		DSpaceRestRepository<DirectlyAddressableRestModel, ID> repository = utils.getResourceRepository(apiCategory, model);
		DirectlyAddressableRestModel modelObject = null;
		try {
			JsonPatchPatchConverter patchConverter = new JsonPatchPatchConverter(mapper);
			Patch patch = patchConverter.convert(jsonNode);
			modelObject = repository.patch(request, apiCategory, model, id, patch);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return ControllerUtils.toEmptyResponse(HttpStatus.INTERNAL_SERVER_ERROR);
		}
		if (modelObject == null) {
			throw new HttpRequestMethodNotSupportedException(RequestMethod.PATCH.toString());
		}
		DSpaceResource result = repository.wrapResource(modelObject);
		//TODO manage HTTPHeader
		return ControllerUtils.toResponseEntity(HttpStatus.OK, null, result);		
		
	}
	
	private <ID extends Serializable> ResourceSupport findRelEntryInternal(HttpServletRequest request, String apiCategory, String model,
			String id, String rel, String relid, Pageable page, PagedResourcesAssembler assembler, String projection) {
		checkModelPluralForm(apiCategory, model);
		DSpaceRestRepository<DirectlyAddressableRestModel, ID> repository = utils.getResourceRepository(apiCategory, model);
		Class<DirectlyAddressableRestModel> domainClass = repository.getDomainClass();
		
		LinkRest linkRest = utils.getLinkRest(rel, domainClass);
		if (linkRest != null) {
			LinkRestRepository linkRepository = utils.getLinkResourceRepository(apiCategory, model, linkRest.name());
			Method linkMethod = repositoryUtils.getLinkMethod("getResource", linkRepository);
			
			try {
				Object object = linkMethod.invoke(linkRepository, request, id, relid, page, projection);
				Link link = linkTo(this.getClass(), apiCategory, English.plural(model)).slash(id)
						.slash(rel).withSelfRel();
				List result = new ArrayList();
				result.add(object);
				PageImpl<DirectlyAddressableRestModel> pageResult = new PageImpl(result, page, 1);
				return assembler.toResource(pageResult.map(linkRepository::wrapResource),link);
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				throw new RuntimeException(e.getMessage(), e);
			}
		}
		return null;
	}

	private <ID extends Serializable> ResourceSupport findRelInternal(HttpServletRequest request, String apiCategory,
			String model, ID uuid, String rel, Pageable page, PagedResourcesAssembler assembler, String projection) {
		checkModelPluralForm(apiCategory, model);
		DSpaceRestRepository<DirectlyAddressableRestModel, ID> repository = utils.getResourceRepository(apiCategory, model);
		Class<DirectlyAddressableRestModel> domainClass = repository.getDomainClass();
		
		LinkRest linkRest = utils.getLinkRest(rel, domainClass);

		if (linkRest != null) {
			LinkRestRepository linkRepository = utils.getLinkResourceRepository(apiCategory, model, linkRest.name());
			Method linkMethod = repositoryUtils.getLinkMethod(linkRest.method(), linkRepository);
			
			if (linkMethod == null) {
				// TODO custom exception
				throw new RuntimeException("Method for relation " + rel + " not found: " + linkRest.name() + ":" + linkRest.method());
			}
			else {
				try {
					if ( Page.class.isAssignableFrom( linkMethod.getReturnType()) ){
						Page<? extends RestModel> pageResult = (Page<? extends DirectlyAddressableRestModel>) linkMethod
								.invoke(linkRepository, request, uuid, page, projection);
						Link link = linkTo(this.getClass(), apiCategory, model).slash(uuid)
								.slash(rel).withSelfRel();
						PagedResources<? extends ResourceSupport> result = assembler
								.toResource(pageResult.map(linkRepository::wrapResource), link);
						return result;
					}
					else {
						RestModel object = (RestModel) linkMethod.invoke(linkRepository, request, uuid, page,
								projection);
						Link link = linkTo(this.getClass(), apiCategory, model).slash(uuid).slash(rel)
								.withSelfRel();
						ResourceSupport result = linkRepository.wrapResource(object);
						result.add(link);						
						return result;
					}					
				} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
					throw new RuntimeException(e.getMessage(), e);
				}
			}
		}
		DirectlyAddressableRestModel modelObject = repository.findOne(uuid);
		DSpaceResource result = repository.wrapResource(modelObject, rel);
		if (result.getLink(rel) == null) {
			// TODO create a custom exception
			throw new ResourceNotFoundException(rel + " undefined for " + model);
		} else if (result.getEmbedded().get(rel) instanceof EmbeddedPage) {
			// this is a very inefficient scenario. We have an embedded list
			// already fully retrieved that we need to limit with pagination
			// parameter. BTW change the default sorting is not implemented at
			// the current stage and could be overcompex to implement
			// if we really want to implement pagination we should implement a
			// link repository so to fall in the previous block code
			EmbeddedPage ep = (EmbeddedPage) result.getEmbedded().get(rel);
			List<? extends DirectlyAddressableRestModel> fullList = ep.getFullList();
			if (fullList == null || fullList.size() == 0)
				return null;
			int start = page.getOffset();
			int end = (start + page.getPageSize()) > fullList.size() ? fullList.size() : (start + page.getPageSize());
			DSpaceRestRepository<DirectlyAddressableRestModel, ?> resourceRepository = utils
					.getResourceRepository(fullList.get(0).getCategory(), fullList.get(0).getType());
			PageImpl<DirectlyAddressableRestModel> pageResult = new PageImpl(fullList.subList(start, end), page, fullList.size());
			return assembler.toResource(pageResult.map(resourceRepository::wrapResource));
		} else {
			ResourceSupport resu = (ResourceSupport) result.getEmbedded().get(rel);
			return resu;
		}
	}

	@RequestMapping(method = RequestMethod.GET)
	@SuppressWarnings("unchecked")
	public <T extends DirectlyAddressableRestModel> PagedResources<DSpaceResource<T>> findAll(@PathVariable String apiCategory,
			@PathVariable String model, Pageable page, PagedResourcesAssembler assembler,
			@RequestParam(required = false) String projection) {
		DSpaceRestRepository<T, ?> repository = utils.getResourceRepository(apiCategory, model);
		Link link = linkTo(methodOn(this.getClass(), apiCategory, model).findAll(apiCategory, model,
				page, assembler, projection)).withSelfRel();

		Page<DSpaceResource<T>> resources;
		try {
			resources = repository.findAll(page).map(repository::wrapResource);
		} catch (PaginationException pe) {
			resources = new PageImpl<DSpaceResource<T>>(new ArrayList<DSpaceResource<T>>(), page, pe.getTotal());
		} catch (RepositoryMethodNotImplementedException mne) {
			throw mne;
		}
		PagedResources<DSpaceResource<T>> result = assembler.toResource(resources, link);
		if (repositoryUtils.haveSearchMethods(repository)) {
			result.add(linkTo(this.getClass(), apiCategory, model).slash("search").withRel("search"));
		}
		return result;
	}

	/**
	 * Check that the model is specified in its plural form, otherwise throw a
	 * RepositoryNotFound exception
	 * 
	 * @param model
	 */
	private void checkModelPluralForm(String apiCategory, String model) {
		if (StringUtils.equals(utils.makeSingular(model), model)) {
			throw new RepositoryNotFoundException(apiCategory, model);
		}
	}

	@RequestMapping(method = RequestMethod.GET, value = "/search")
	public ResourceSupport listSearchMethods(@PathVariable String apiCategory, @PathVariable String model) {
		ResourceSupport root = new ResourceSupport();
		DSpaceRestRepository repository = utils.getResourceRepository(apiCategory, model);

		List<String> searchMethods = repositoryUtils.listSearchMethods(repository);

		if (CollectionUtils.isEmpty(searchMethods)) {
			throw new RepositorySearchNotFoundException(model);
		}

		for (String name : searchMethods) {
			Link link = linkTo(this.getClass(), apiCategory, model).slash("search").slash(name).withRel(name);
			root.add(link);
		}
		return root;
	}

	@RequestMapping(method = RequestMethod.GET, value = "/search/{searchMethodName}")
	@SuppressWarnings("unchecked")
	public <T extends DirectlyAddressableRestModel> ResourceSupport executeSearchMethods(@PathVariable String apiCategory,
			@PathVariable String model, @PathVariable String searchMethodName, Pageable pageable, Sort sort,
			PagedResourcesAssembler assembler, @RequestParam MultiValueMap<String, Object> parameters)
			throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {

		Link link = linkTo(this.getClass(), apiCategory, model).slash("search").slash(searchMethodName).withSelfRel();
		DSpaceRestRepository repository = utils.getResourceRepository(apiCategory, model);
		boolean returnPage = false;
		Object searchResult = null;
		
		Method searchMethod = repositoryUtils.getSearchMethod(searchMethodName, repository);
		
		if (searchMethod == null) {
			if (repositoryUtils.haveSearchMethods(repository)) {
				throw new RepositorySearchMethodNotFoundException(model, searchMethodName);	
			}
			else {
				throw new RepositorySearchNotFoundException(model);	
			}
		}
		
		searchResult = repositoryUtils.executeQueryMethod(repository, parameters, searchMethod, pageable, sort, assembler);
		
		returnPage = searchMethod.getReturnType().isAssignableFrom(Page.class);
		ResourceSupport result = null;
		if (returnPage) {
			Page<DSpaceResource<T>> resources = ((Page<T>) searchResult).map(repository::wrapResource);
			result = assembler.toResource(resources, link);
		} else {
			result = repository.wrapResource((T) searchResult);
		}
		return result;
	}
	
	/**
	 * Sets the location header pointing to the resource representing the given instance. Will make sure we properly
	 * expand the URI template potentially created as self link.
	 * 
	 * @param headers must not be {@literal null}.
	 * @param assembler must not be {@literal null}.
	 * @param source must not be {@literal null}.
	 */
	private void addLocationHeader(HttpHeaders headers, PersistentEntityResourceAssembler assembler, Object source) {

		String selfLink = assembler.getSelfLinkFor(source).getHref();
		headers.setLocation(new UriTemplate(selfLink).expand());
	}
}
