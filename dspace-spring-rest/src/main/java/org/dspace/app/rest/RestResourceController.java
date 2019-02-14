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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.atteo.evo.inflector.English;
import org.dspace.app.rest.converter.JsonPatchConverter;
import org.dspace.app.rest.exception.PaginationException;
import org.dspace.app.rest.exception.PatchBadRequestException;
import org.dspace.app.rest.exception.RepositoryMethodNotImplementedException;
import org.dspace.app.rest.exception.RepositoryNotFoundException;
import org.dspace.app.rest.exception.RepositorySearchMethodNotFoundException;
import org.dspace.app.rest.exception.RepositorySearchNotFoundException;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.link.HalLinkService;
import org.dspace.app.rest.model.LinkRest;
import org.dspace.app.rest.model.RestAddressableModel;
import org.dspace.app.rest.model.RestModel;
import org.dspace.app.rest.model.hateoas.DSpaceResource;
import org.dspace.app.rest.model.hateoas.EmbeddedPage;
import org.dspace.app.rest.model.hateoas.HALResource;
import org.dspace.app.rest.model.patch.Patch;
import org.dspace.app.rest.repository.DSpaceRestRepository;
import org.dspace.app.rest.repository.LinkRestRepository;
import org.dspace.app.rest.utils.RestRepositoryUtils;
import org.dspace.app.rest.utils.Utils;
import org.dspace.authorize.AuthorizeException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.rest.webmvc.ControllerUtils;
import org.springframework.data.rest.webmvc.PersistentEntityResourceAssembler;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.hateoas.Resources;
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

/**
 * This is the main entry point of the new REST API. Its responsibility is to
 * provide a consistent behaviors for all the exposed resources in terms of
 * returned HTTP codes, endpoint URLs, HTTP verbs to methods translation, etc.
 * It delegates to the repository the business logic
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
@RestController
@RequestMapping("/api/{apiCategory}/{model}")
@SuppressWarnings("rawtypes")
public class RestResourceController implements InitializingBean {

    /**
     * Regular expression in the request mapping to accept UUID as identifier
     */
    private static final String REGEX_REQUESTMAPPING_IDENTIFIER_AS_UUID =
        "/{uuid:[0-9a-fxA-FX]{8}-[0-9a-fxA-FX]{4}-[0-9a-fxA-FX]{4}-[0-9a-fxA-FX]{4}-[0-9a-fxA-FX]{12}}";

    /**
     * Regular expression in the request mapping to accept a string as identifier but not the other kind of
     * identifier (digits or uuid)
     */
    private static final String REGEX_REQUESTMAPPING_IDENTIFIER_AS_STRING_VERSION_STRONG = "/{id:^(?!^\\d+$)" +
        "(?!^[0-9a-fxA-FX]{8}-[0-9a-fxA-FX]{4}-[0-9a-fxA-FX]{4}-[0-9a-fxA-FX]{4}-[0-9a-fxA-FX]{12}$)[\\w+\\-]+$+}";

    /**
     * Regular expression in the request mapping to accept number as identifier
     */
    private static final String REGEX_REQUESTMAPPING_IDENTIFIER_AS_DIGIT = "/{id:\\d+}";

    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(RestResourceController.class);

    @Autowired
    DiscoverableEndpointsService discoverableEndpointsService;

    @Autowired
    Utils utils;

    @Autowired
    RestRepositoryUtils repositoryUtils;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    HalLinkService linkService;

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


    /**
     * Called in GET is used to retrieve the single resource by identifier;
     *
     * Note that the regular expression in the request mapping accept a number as identifier;
     *
     * Please see {@link RestResourceController#findOne(String, String, String, String)} for findOne with string as
     * identifier
     * and see {@link RestResourceController#findOne(String, String, UUID, String)} for uuid as identifier
     *
     * @param apiCategory
     * @param model
     * @param id
     * @param projection
     * @return
     */
    @RequestMapping(method = RequestMethod.GET, value = REGEX_REQUESTMAPPING_IDENTIFIER_AS_DIGIT)
    @SuppressWarnings("unchecked")
    public DSpaceResource<RestAddressableModel> findOne(@PathVariable String apiCategory, @PathVariable String model,
                                                        @PathVariable Integer id,
                                                        @RequestParam(required = false) String projection) {
        return findOneInternal(apiCategory, model, id, projection);
    }

    /**
     * Called in GET is used to retrieve the single resource by identifier;
     *
     * Note that the regular expression in the request mapping accept a string as identifier but not the other kind
     * of identifier;
     *
     * http://<dspace.url>/dspace-spring-rest/api/{apiCategory}/{model}/{id}
     *
     * Example:
     * <pre>
     * {@code
     *    http://<dspace.url>/dspace-spring-rest/api/config/submissionsections/collection
     * }
     * </pre>
     *
     *
     * Please see {@link RestResourceController#findOne(String, String, Integer, String)} for findOne with number as
     * identifier
     * and see {@link RestResourceController#findOne(String, String, UUID, String)} for uuid as identifier
     *
     * @param apiCategory
     * @param model
     * @param id
     * @param projection
     * @return
     */
    @RequestMapping(method = RequestMethod.GET, value = REGEX_REQUESTMAPPING_IDENTIFIER_AS_STRING_VERSION_STRONG)
    @SuppressWarnings("unchecked")
    public DSpaceResource<RestAddressableModel> findOne(@PathVariable String apiCategory, @PathVariable String model,
                                                        @PathVariable String id,
                                                        @RequestParam(required = false) String projection) {
        return findOneInternal(apiCategory, model, id, projection);
    }

    /**
     * Called in GET is used to retrieve the single resource by identifier;
     *
     * Note that the regular expression in the request mapping accept a UUID as identifier;
     *
     * Please see {@link RestResourceController#findOne(String, String, Integer, String)} for findOne with number as
     * identifier
     * and see {@link RestResourceController#findOne(String, String, String, String)} for string as identifier
     *
     * @param apiCategory
     * @param model
     * @param uuid
     * @param projection
     * @return
     */
    @RequestMapping(method = RequestMethod.GET, value = REGEX_REQUESTMAPPING_IDENTIFIER_AS_UUID)
    @SuppressWarnings("unchecked")
    public DSpaceResource<RestAddressableModel> findOne(@PathVariable String apiCategory, @PathVariable String model,
                                                        @PathVariable UUID uuid,
                                                        @RequestParam(required = false) String projection) {
        return findOneInternal(apiCategory, model, uuid, projection);
    }

    /**
     * Internal method to retrieve single resource from an identifier of generic type
     *
     * @param apiCategory
     * @param model
     * @param id
     * @param projection
     * @return
     */
    private <ID extends Serializable> DSpaceResource<RestAddressableModel> findOneInternal(String apiCategory,
                                                                                           String model, ID id,
                                                                                           String projection) {
        checkModelPluralForm(apiCategory, model);
        DSpaceRestRepository<RestAddressableModel, ID> repository = utils.getResourceRepository(apiCategory, model);
        RestAddressableModel modelObject = null;
        try {
            modelObject = repository.findOne(id);
        } catch (ClassCastException e) {
            // ignore, as handled below
        }
        if (modelObject == null) {
            throw new ResourceNotFoundException(apiCategory + "." + model + " with id: " + id + " not found");
        }
        DSpaceResource result = repository.wrapResource(modelObject);
        linkService.addLinks(result);
        return result;
    }

    /**
     * Called in GET is used to retrieve the relation resources;
     *
     * Note that the regular expression in the request mapping accept a number;
     *
     * @param request
     * @param apiCategory
     * @param model
     * @param id
     * @param rel
     * @param page
     * @param assembler
     * @param projection
     * @return
     */
    @RequestMapping(method = RequestMethod.GET, value = REGEX_REQUESTMAPPING_IDENTIFIER_AS_DIGIT + "/{rel}")
    public ResourceSupport findRel(HttpServletRequest request, HttpServletResponse response,
                                   @PathVariable String apiCategory,
                                   @PathVariable String model, @PathVariable Integer id, @PathVariable String rel,
                                   Pageable page,
                                   PagedResourcesAssembler assembler,
                                   @RequestParam(required = false) String projection) {
        return findRelInternal(request, response, apiCategory, model, id, rel, page, assembler, projection);
    }

    /**
     * Called in GET is used to retrieve the relation resources;
     *
     * Note that the regular expression in the request mapping accept a string as identifier but not the other kind
     * of identifier;
     *
     * @param request
     * @param apiCategory
     * @param model
     * @param id
     * @param rel
     * @param page
     * @param assembler
     * @param projection
     * @return
     */
    @RequestMapping(method = RequestMethod.GET, value = REGEX_REQUESTMAPPING_IDENTIFIER_AS_STRING_VERSION_STRONG +
        "/{rel}")
    public ResourceSupport findRel(HttpServletRequest request, HttpServletResponse response,
                                   @PathVariable String apiCategory,
                                   @PathVariable String model, @PathVariable String id, @PathVariable String rel,
                                   Pageable page,
                                   PagedResourcesAssembler assembler,
                                   @RequestParam(required = false) String projection) {
        return findRelInternal(request, response, apiCategory, model, id, rel, page, assembler, projection);
    }

    /**
     * Called in GET is used to retrieve the relation resources;
     *
     * Note that the regular expression in the request mapping accept a UUID as identifier;
     *
     * @param request
     * @param apiCategory
     * @param model
     * @param uuid
     * @param rel
     * @param page
     * @param assembler
     * @param projection
     * @return
     */
    @RequestMapping(method = RequestMethod.GET, value = REGEX_REQUESTMAPPING_IDENTIFIER_AS_UUID + "/{rel}")
    public ResourceSupport findRel(HttpServletRequest request, HttpServletResponse response,
                                   @PathVariable String apiCategory,
                                   @PathVariable String model, @PathVariable UUID uuid, @PathVariable String rel,
                                   Pageable page,
                                   PagedResourcesAssembler assembler,
                                   @RequestParam(required = false) String projection) {
        return findRelInternal(request, response, apiCategory, model, uuid, rel, page, assembler, projection);
    }

    /**
     * Called in GET, try to retrieve the requested linked resource.
     *
     * Note that the regular expression in the request mapping accept a string as identifier but not the other kind
     * of identifier;
     *
     * http://<dspace.url>/dspace-spring-rest/api/{apiCategory}/{model}/{id}/{rel}/{relid}
     *
     * Example:
     * <pre>
     * {@code
     *      http://<dspace.url>/dspace-spring-rest/api/integration/authorities/SRJournalTitle/entryValues/1479-9995
     * }
     * </pre>
     *
     * Example:
     * <pre>
     * {@code
     *      http://<dspace.url>/dspace-spring-rest/api/integration/authorities/srsc/entries/VR110111
     * }
     * </pre>
     *
     * @param request
     * @param apiCategory
     * @param model
     * @param id
     * @param rel
     * @param relid
     * @param page
     * @param assembler
     * @param projection
     * @return
     */
    @RequestMapping(method = RequestMethod.GET, value = REGEX_REQUESTMAPPING_IDENTIFIER_AS_STRING_VERSION_STRONG +
        "/{rel}/{relid:[\\w+\\-]+}")
    public ResourceSupport findRel(HttpServletRequest request, HttpServletResponse response,
                                   @PathVariable String apiCategory,
                                   @PathVariable String model, @PathVariable String id, @PathVariable String rel,
                                   @PathVariable String relid,
                                   Pageable page, PagedResourcesAssembler assembler,
                                   @RequestParam(required = false) String projection) {
        return findRelEntryInternal(request, response, apiCategory, model, id, rel, relid, page, assembler, projection);
    }


    /**
     * Execute a POST request;
     *
     * curl -X POST http://<dspace.url>/dspace-spring-rest/api/{apiCategory}/{model}
     *
     * Example:
     * <pre>
     * {@code
     *      curl -X POST http://<dspace.url>/dspace-spring-rest/api/submission/workspaceitems
     * }
     * </pre>
     *
     * @param request
     * @param apiCategory
     * @param model
     * @return
     * @throws HttpRequestMethodNotSupportedException
     */
    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<ResourceSupport> post(HttpServletRequest request, @PathVariable String apiCategory,
                                                @PathVariable String model)
        throws HttpRequestMethodNotSupportedException {
        return postInternal(request, apiCategory, model);
    }

    /**
     * Internal method to execute POST;
     *
     * @param request
     * @param apiCategory
     * @param model
     * @return
     * @throws HttpRequestMethodNotSupportedException
     */
    public <ID extends Serializable> ResponseEntity<ResourceSupport> postInternal(HttpServletRequest request,
                                                                                  String apiCategory,
                                                                                  String model)
        throws HttpRequestMethodNotSupportedException {
        checkModelPluralForm(apiCategory, model);
        DSpaceRestRepository<RestAddressableModel, ID> repository = utils.getResourceRepository(apiCategory, model);
        RestAddressableModel modelObject = null;
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
        linkService.addLinks(result);
        //TODO manage HTTPHeader
        return ControllerUtils.toResponseEntity(HttpStatus.CREATED, null, result);
    }

    /**
     *  Called in POST, multipart, upload to a specific rest resource the file passed as "file" request parameter
     *
     * Note that the regular expression in the request mapping accept a number as identifier;
     *
     * @param request
     *            the http request
     * @param apiCategory
     *            the api category
     * @param model
     *            the rest model that identify the REST resource collection
     * @param id
     *            the id of the specific rest resource
     * @param uploadfile
     *            the file to upload
     * @return the created resource
     * @throws HttpRequestMethodNotSupportedException
     */
    @RequestMapping(method = RequestMethod.POST, value = REGEX_REQUESTMAPPING_IDENTIFIER_AS_DIGIT, headers =
        "content-type=multipart/form-data")
    public <ID extends Serializable> ResponseEntity<ResourceSupport> upload(HttpServletRequest request,
                                                                            @PathVariable String apiCategory,
                                                                            @PathVariable String model,
                                                                            @PathVariable Integer id,
                                                                            @RequestParam("file") MultipartFile
                                                                                uploadfile)
        throws HttpRequestMethodNotSupportedException {
        return uploadInternal(request, apiCategory, model, id, uploadfile);
    }

    /**
     * Called in POST, multipart, upload to a specific rest resource the file passed as "file" request parameter
     *
     * Note that the regular expression in the request mapping accept a UUID as identifier;
     *
     * @param request
     *            the http request
     * @param apiCategory
     *            the api category
     * @param model
     *            the rest model that identify the REST resource collection
     * @param id
     *            the id of the specific rest resource
     * @param uploadfile
     *            the file to upload
     * @return the created resource
     * @throws HttpRequestMethodNotSupportedException
     */
    @RequestMapping(method = RequestMethod.POST, value = REGEX_REQUESTMAPPING_IDENTIFIER_AS_UUID, headers =
        "content-type=multipart/form-data")
    public <ID extends Serializable> ResponseEntity<ResourceSupport> upload(HttpServletRequest request,
                                                                            @PathVariable String apiCategory,
                                                                            @PathVariable String model,
                                                                            @PathVariable UUID id,
                                                                            @RequestParam("file") MultipartFile
                                                                                uploadfile)
        throws HttpRequestMethodNotSupportedException {
        return uploadInternal(request, apiCategory, model, id, uploadfile);
    }

    /**
     * Internal upload method.
     *
     * @param request
     * @param apiCategory
     * @param model
     * @param id
     * @param uploadfile
     * @return
     */
    private <ID extends Serializable> ResponseEntity<ResourceSupport> uploadInternal(HttpServletRequest request,
                                                                                     String apiCategory, String model,
                                                                                     ID id,
                                                                                     MultipartFile uploadfile) {
        checkModelPluralForm(apiCategory, model);
        DSpaceRestRepository<RestAddressableModel, ID> repository = utils.getResourceRepository(apiCategory, model);

        RestAddressableModel modelObject = null;
        try {
            modelObject = repository.upload(request, apiCategory, model, id, uploadfile);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return ControllerUtils.toEmptyResponse(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        DSpaceResource result = repository.wrapResource(modelObject);
        linkService.addLinks(result);
        return ControllerUtils.toResponseEntity(HttpStatus.CREATED, null, result);
    }

    /**
     * Upload a file against the collection resource endpoint. This is typically used for bulk creation of resources
     * such for instance multiple workspaceitems from a CSV or bibliographic file
     *
     * @param request
     *            the http request
     * @param apiCategory
     *            the api category
     * @param model
     *            the rest model that identify the REST resource collection
     * @param uploadfile
     *            the bulk file
     * @return the list of generated resources
     * @throws SQLException
     * @throws FileNotFoundException
     * @throws IOException
     * @throws AuthorizeException
     */
    @RequestMapping(method = { RequestMethod.POST }, headers = "content-type=multipart/form-data")
    public <T extends RestAddressableModel> ResponseEntity<ResourceSupport> upload(HttpServletRequest request,
                                                                                   @PathVariable String apiCategory,
                                                                                   @PathVariable String model,
                                                                                   @RequestParam("file") MultipartFile
                                                                                       uploadfile)
        throws SQLException, FileNotFoundException, IOException, AuthorizeException {

        checkModelPluralForm(apiCategory, model);
        DSpaceRestRepository repository = utils.getResourceRepository(apiCategory, model);

        Iterable<T> content = repository.upload(request, uploadfile);

        List<DSpaceResource> resources = new ArrayList<>();
        for (T modelObject : content) {
            DSpaceResource result = repository.wrapResource(modelObject);
            linkService.addLinks(result);
            resources.add(result);
        }
        return ControllerUtils.toResponseEntity(HttpStatus.OK, null, Resources.wrap(resources));
    }

    /**
     * PATCH method, using operation on the resources following (JSON) Patch notation (https://tools.ietf
     * .org/html/rfc6902)
     *
     * Note that the regular expression in the request mapping accept a number as identifier;
     *
     * @param request
     * @param apiCategory
     * @param model
     * @param id
     * @param jsonNode
     * @return
     * @throws HttpRequestMethodNotSupportedException
     */
    @RequestMapping(method = RequestMethod.PATCH, value = REGEX_REQUESTMAPPING_IDENTIFIER_AS_DIGIT)
    public ResponseEntity<ResourceSupport> patch(HttpServletRequest request, @PathVariable String apiCategory,
                                                 @PathVariable String model, @PathVariable Integer id,
                                                 @RequestBody(required = true) JsonNode jsonNode)
        throws HttpRequestMethodNotSupportedException {
        return patchInternal(request, apiCategory, model, id, jsonNode);
    }

    /**
     * PATCH method, using operation on the resources following (JSON) Patch notation (https://tools.ietf
     * .org/html/rfc6902)
     *
     * Note that the regular expression in the request mapping accept a UUID as identifier;
     *
     * @param request
     * @param apiCategory
     * @param model
     * @param id
     * @param jsonNode
     * @return
     * @throws HttpRequestMethodNotSupportedException
     */
    @RequestMapping(method = RequestMethod.PATCH, value = REGEX_REQUESTMAPPING_IDENTIFIER_AS_UUID)
    public ResponseEntity<ResourceSupport> patch(HttpServletRequest request, @PathVariable String apiCategory,
                                                 @PathVariable String model,
                                                 @PathVariable(name = "uuid") UUID id,
                                                 @RequestBody(required = true) JsonNode jsonNode)
        throws HttpRequestMethodNotSupportedException {
        return patchInternal(request, apiCategory, model, id, jsonNode);
    }

    /**
     * Internal patch method
     *
     * @param request
     * @param apiCategory
     * @param model
     * @param id
     * @param jsonNode
     * @return
     * @throws HttpRequestMethodNotSupportedException
     */
    public <ID extends Serializable> ResponseEntity<ResourceSupport> patchInternal(HttpServletRequest request,
                                                                                   String apiCategory,
                                                                                   String model, ID id,
                                                                                   JsonNode jsonNode)
        throws HttpRequestMethodNotSupportedException {
        checkModelPluralForm(apiCategory, model);
        DSpaceRestRepository<RestAddressableModel, ID> repository = utils.getResourceRepository(apiCategory, model);
        RestAddressableModel modelObject = null;
        try {
            JsonPatchConverter patchConverter = new JsonPatchConverter(mapper);
            Patch patch = patchConverter.convert(jsonNode);
            modelObject = repository.patch(request, apiCategory, model, id, patch);
        } catch (RepositoryMethodNotImplementedException | UnprocessableEntityException |
            PatchBadRequestException | ResourceNotFoundException e) {
            log.error(e.getMessage(), e);
            throw e;
        }
        DSpaceResource result = repository.wrapResource(modelObject);
        linkService.addLinks(result);
        //TODO manage HTTPHeader
        return ControllerUtils.toResponseEntity(HttpStatus.OK, null, result);

    }

    /**
     * Internal method to retrieve linked entry resource.
     *
     * @param request
     * @param apiCategory
     * @param model
     * @param id
     * @param rel
     * @param relid
     * @param page
     * @param assembler
     * @param projection
     * @return
     */
    private <ID extends Serializable> ResourceSupport findRelEntryInternal(HttpServletRequest request,
                                                                           HttpServletResponse response,
                                                                           String apiCategory, String model,
                                                                           String id, String rel, String relid,
                                                                           Pageable page,
                                                                           PagedResourcesAssembler assembler,
                                                                           String projection) {
        checkModelPluralForm(apiCategory, model);
        DSpaceRestRepository<RestAddressableModel, ID> repository = utils.getResourceRepository(apiCategory, model);
        Class<RestAddressableModel> domainClass = repository.getDomainClass();

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
                PageImpl<RestAddressableModel> pageResult = new PageImpl(result, page, 1);
                Page<HALResource> halResources = pageResult.map(linkRepository::wrapResource);
                halResources.forEach(linkService::addLinks);
                return assembler.toResource(halResources, link);
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        }
        return null;
    }

    /**
     * Internal method to retrieve linked resource.
     *
     * @param request
     * @param apiCategory
     * @param model
     * @param uuid
     * @param rel
     * @param page
     * @param assembler
     * @param projection
     * @return
     */
    private <ID extends Serializable> ResourceSupport findRelInternal(HttpServletRequest request,
                                                                      HttpServletResponse response, String apiCategory,
                                                                      String model, ID uuid, String subpath,
                                                                      Pageable page, PagedResourcesAssembler assembler,
                                                                      String projection) {
        checkModelPluralForm(apiCategory, model);
        DSpaceRestRepository<RestAddressableModel, ID> repository = utils.getResourceRepository(apiCategory, model);
        Class<RestAddressableModel> domainClass = repository.getDomainClass();

        LinkRest linkRest = utils.getLinkRest(subpath, domainClass);
        PagedResources<? extends HALResource> result;

        if (linkRest != null) {
            LinkRestRepository linkRepository = utils.getLinkResourceRepository(apiCategory, model, linkRest.name());
            Method linkMethod = repositoryUtils.getLinkMethod(linkRest.method(), linkRepository);

            if (linkMethod == null) {
                // TODO custom exception
                throw new RuntimeException(
                        "Method for relation " + subpath + " not found: " + linkRest.name() + ":" + linkRest.method());
            } else {
                try {
                    if (Page.class.isAssignableFrom(linkMethod.getReturnType())) {
                        Page<? extends RestModel> pageResult = (Page<? extends RestAddressableModel>) linkMethod
                                .invoke(linkRepository, request, uuid, page, projection);

                        Link link = null;
                        String querystring = request.getQueryString();
                        if (querystring != null && querystring.length() > 0) {
                            link = linkTo(this.getClass(), apiCategory, model).slash(uuid)
                                .slash(subpath + '?' + querystring).withSelfRel();
                        } else {
                            link = linkTo(this.getClass(), apiCategory, model).slash(uuid).withSelfRel();
                        }

                        Page<HALResource> halResources = pageResult.map(linkRepository::wrapResource);
                        halResources.forEach(linkService::addLinks);

                        return assembler.toResource(halResources, link);
                    } else {
                        RestModel object = (RestModel) linkMethod.invoke(linkRepository, request, uuid, page,
                                projection);
                        Link link = linkTo(this.getClass(), apiCategory, model).slash(uuid).slash(subpath)
                                .withSelfRel();
                        HALResource tmpresult = linkRepository.wrapResource(object);
                        tmpresult.add(link);
                        return tmpresult;
                    }
                } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                    throw new RuntimeException(e.getMessage(), e);
                }
            }
        }
        RestAddressableModel modelObject = repository.findOne(uuid);

        if (modelObject == null) {
            throw new ResourceNotFoundException(apiCategory + "." + model + " with id: " + uuid + " not found");
        }

        DSpaceResource resource = repository.wrapResource(modelObject, subpath);
        linkService.addLinks(resource);

        String rel = null;

        for (Link l : resource.getLinks()) {
            if (l.isTemplated()) {
                if (l.getHref().substring(0, l.getHref().indexOf("?")).contentEquals(request.getRequestURL())) {
                    rel = l.getRel();
                }
            } else if (l.getHref().contentEquals(request.getRequestURL())) {
                rel = l.getRel();
            }
        }

        if (rel == null) {
            throw new ResourceNotFoundException(rel + " undefined for " + model);
        }

        if (resource.getLink(rel) == null) {
            // TODO create a custom exception
            throw new ResourceNotFoundException(rel + " undefined for " + model);
        } else if (resource.getEmbeddedResources().get(rel) instanceof EmbeddedPage) {
            // this is a very inefficient scenario. We have an embedded list
            // already fully retrieved that we need to limit with pagination
            // parameter. BTW change the default sorting is not implemented at
            // the current stage and could be overcompex to implement
            // if we really want to implement pagination we should implement a
            // link repository so to fall in the previous block code
            EmbeddedPage ep = (EmbeddedPage) resource.getEmbeddedResources().get(rel);
            List<? extends RestAddressableModel> fullList = ep.getFullList();
            if (fullList == null || fullList.size() == 0) {
                PageImpl<RestAddressableModel> pageResult = new PageImpl(fullList, page, 0);
                result = assembler.toResource(pageResult);
                return result;
            }
            int start = page.getOffset();
            int end = (start + page.getPageSize()) > fullList.size() ? fullList.size() : (start + page.getPageSize());
            DSpaceRestRepository<RestAddressableModel, ?> resourceRepository = utils
                .getResourceRepository(fullList.get(0).getCategory(), fullList.get(0).getType());
            PageImpl<RestAddressableModel> pageResult = new PageImpl(fullList.subList(start, end), page,
                                                                     fullList.size());
            result = assembler.toResource(pageResult.map(resourceRepository::wrapResource));

            for (Resource subObj : result) {
                if (subObj.getContent() instanceof HALResource) {
                    linkService.addLinks((HALResource) subObj.getContent());
                }
            }
            return result;


        } else {
            if (resource.getEmbeddedResources().get(rel) == null) {
                response.setStatus(HttpServletResponse.SC_NO_CONTENT);
            }
            return (ResourceSupport) resource.getEmbeddedResources().get(rel);
        }

    }

    /**
     * Find all
     *
     * @param apiCategory
     * @param model
     * @param page
     * @param assembler
     * @param projection
     * @return
     */
    @RequestMapping(method = RequestMethod.GET)
    @SuppressWarnings("unchecked")
    public <T extends RestAddressableModel> PagedResources<DSpaceResource<T>> findAll(@PathVariable String apiCategory,
                                                                                      @PathVariable String model,
                                                                                      Pageable page,
                                                                                      PagedResourcesAssembler assembler,
                                                                                      @RequestParam(required = false)
                                                                                              String projection,
                                                                                      HttpServletResponse response) {
        DSpaceRestRepository<T, ?> repository = utils.getResourceRepository(apiCategory, model);
        Link link = linkTo(methodOn(this.getClass(), apiCategory, model).findAll(apiCategory, model,
                                                                                 page, assembler, projection, response))
            .withSelfRel();

        Page<DSpaceResource<T>> resources;
        try {
            resources = repository.findAll(page).map(repository::wrapResource);
            resources.forEach(linkService::addLinks);
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
     * Check that the model is specified in its plural form
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
    public <T extends RestAddressableModel> ResourceSupport executeSearchMethods(@PathVariable String apiCategory,
                                                                                 @PathVariable String model,
                                                                                 @PathVariable String searchMethodName,
                                                                                 HttpServletResponse response,
                                                                                 Pageable pageable, Sort sort,
                                                                                 PagedResourcesAssembler assembler,
                                                                                 @RequestParam MultiValueMap<String,
                                                                                     Object> parameters)
        throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {

        Link link = linkTo(this.getClass(), apiCategory, model).slash("search").slash(searchMethodName).withSelfRel();
        DSpaceRestRepository repository = utils.getResourceRepository(apiCategory, model);
        boolean returnPage = false;
        Object searchResult = null;

        Method searchMethod = repositoryUtils.getSearchMethod(searchMethodName, repository);

        if (searchMethod == null) {
            if (repositoryUtils.haveSearchMethods(repository)) {
                throw new RepositorySearchMethodNotFoundException(model, searchMethodName);
            } else {
                throw new RepositorySearchNotFoundException(model);
            }
        }

        searchResult = repositoryUtils
            .executeQueryMethod(repository, parameters, searchMethod, pageable, sort, assembler);

        returnPage = searchMethod.getReturnType().isAssignableFrom(Page.class);
        ResourceSupport result = null;
        if (returnPage) {
            Page<DSpaceResource<T>> resources;
            if (searchResult == null) {
                resources = new PageImpl(new ArrayList(), pageable, 0);
            } else {
                resources = ((Page<T>) searchResult).map(repository::wrapResource);
            }
            resources.forEach(linkService::addLinks);
            result = assembler.toResource(resources, link);
        } else {
            if (searchResult == null) {
                response.setStatus(HttpServletResponse.SC_NO_CONTENT);
                return null;
            }
            DSpaceResource<T> dsResource = repository.wrapResource((T) searchResult);
            linkService.addLinks(dsResource);
            result = dsResource;
        }
        return result;
    }

    /**
     * Sets the location header pointing to the resource representing the given instance. Will make sure we properly
     * expand the URI template potentially created as self link.
     *
     * @param headers   must not be {@literal null}.
     * @param assembler must not be {@literal null}.
     * @param source    must not be {@literal null}.
     */
    private void addLocationHeader(HttpHeaders headers, PersistentEntityResourceAssembler assembler, Object source) {

        String selfLink = assembler.getSelfLinkFor(source).getHref();
        headers.setLocation(new UriTemplate(selfLink).expand());
    }

    @RequestMapping(method = RequestMethod.DELETE, value = REGEX_REQUESTMAPPING_IDENTIFIER_AS_DIGIT)
    public ResponseEntity<ResourceSupport> delete(HttpServletRequest request, @PathVariable String apiCategory,
                                                  @PathVariable String model, @PathVariable Integer id)
        throws HttpRequestMethodNotSupportedException {
        return deleteInternal(apiCategory, model, id);
    }

    @RequestMapping(method = RequestMethod.DELETE, value = REGEX_REQUESTMAPPING_IDENTIFIER_AS_UUID)
    public ResponseEntity<ResourceSupport> delete(HttpServletRequest request, @PathVariable String apiCategory,
                                                  @PathVariable String model, @PathVariable UUID uuid)
        throws HttpRequestMethodNotSupportedException {
        return deleteInternal(apiCategory, model, uuid);
    }

    /**
     * Internal method to delete resource.
     *
     * @param apiCategory
     * @param model
     * @param id
     * @return
     */
    private <ID extends Serializable> ResponseEntity<ResourceSupport> deleteInternal(String apiCategory, String model,
                                                                                     ID id) {
        checkModelPluralForm(apiCategory, model);
        DSpaceRestRepository<RestAddressableModel, ID> repository = utils.getResourceRepository(apiCategory, model);
        repository.delete(id);
        return ControllerUtils.toEmptyResponse(HttpStatus.NO_CONTENT);
    }



    /**
     * Execute a PUT request for an entity with id of type Integer;
     *
     * curl -X PUT http://<dspace.restUrl>/api/{apiCategory}/{model}/{id}
     *
     * Example:
     * <pre>
     * {@code
     *      curl -X PUT http://<dspace.restUrl>/api/core/metadatafield/1
     * }
     * </pre>
     *
     * @param request     the http request
     * @param apiCategory the API category e.g. "core"
     * @param model       the DSpace model e.g. "metadatafield"
     * @param id          the ID of the target REST object
     * @param jsonNode    the part of the request body representing the updated rest object
     * @return the relevant REST resource
     */
    @RequestMapping(method = RequestMethod.PUT, value = REGEX_REQUESTMAPPING_IDENTIFIER_AS_DIGIT)
    public DSpaceResource<RestAddressableModel> put(HttpServletRequest request,
                                                    @PathVariable String apiCategory, @PathVariable String model,
                                                    @PathVariable Integer id,
                                                    @RequestBody JsonNode jsonNode) {
        return putOneInternal(request, apiCategory, model, id, jsonNode);
    }

    /**
     * Execute a PUT request for an entity with id of type UUID;
     *
     * curl -X PUT http://<dspace.restUrl>/api/{apiCategory}/{model}/{uuid}
     *
     * Example:
     * <pre>
     * {@code
     *      curl -X PUT http://<dspace.restUrl>/api/core/collection/8b632938-77c2-487c-81f0-e804f63e68e6
     * }
     * </pre>
     *
     * @param request     the http request
     * @param apiCategory the API category e.g. "core"
     * @param model       the DSpace model e.g. "collection"
     * @param uuid        the ID of the target REST object
     * @param jsonNode    the part of the request body representing the updated rest object
     * @return the relevant REST resource
     */
    @RequestMapping(method = RequestMethod.PUT, value = REGEX_REQUESTMAPPING_IDENTIFIER_AS_UUID)
    public DSpaceResource<RestAddressableModel> put(HttpServletRequest request,
                                                    @PathVariable String apiCategory, @PathVariable String model,
                                                    @PathVariable UUID uuid,
                                                    @RequestBody JsonNode jsonNode) {
        return putOneInternal(request, apiCategory, model, uuid, jsonNode);
    }

    /**
     * Internal method to update a single entity
     *
     * @param request     the http request
     * @param apiCategory the API category e.g. "api"
     * @param model       the DSpace model e.g. "metadatafield"
     * @param uuid        the ID of the target REST object
     * @param jsonNode    the part of the request body representing the updated rest object
     * @return the relevant REST resource
     */
    private <ID extends Serializable> DSpaceResource<RestAddressableModel> putOneInternal(HttpServletRequest request,
                                                                                          String apiCategory,
                                                                                          String model, ID uuid,
                                                                                          JsonNode jsonNode) {
        checkModelPluralForm(apiCategory, model);
        DSpaceRestRepository<RestAddressableModel, ID> repository = utils.getResourceRepository(apiCategory, model);
        RestAddressableModel modelObject = null;
        modelObject = repository.put(request, apiCategory, model, uuid, jsonNode);
        if (modelObject == null) {
            throw new ResourceNotFoundException(apiCategory + "." + model + " with id: " + uuid + " not found");
        }
        DSpaceResource result = repository.wrapResource(modelObject);
        linkService.addLinks(result);
        return result;
    }
}