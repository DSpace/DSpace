/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.dspace.app.rest.utils.RegexUtils.REGEX_REQUESTMAPPING_IDENTIFIER_AS_DIGIT;
import static org.dspace.app.rest.utils.RegexUtils.REGEX_REQUESTMAPPING_IDENTIFIER_AS_HEX32;
import static org.dspace.app.rest.utils.RegexUtils.REGEX_REQUESTMAPPING_IDENTIFIER_AS_STRING_VERSION_STRONG;
import static org.dspace.app.rest.utils.RegexUtils.REGEX_REQUESTMAPPING_IDENTIFIER_AS_UUID;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.atteo.evo.inflector.English;
import org.dspace.app.rest.converter.ConverterService;
import org.dspace.app.rest.converter.JsonPatchConverter;
import org.dspace.app.rest.exception.DSpaceBadRequestException;
import org.dspace.app.rest.exception.PaginationException;
import org.dspace.app.rest.exception.RESTAuthorizationException;
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
import org.dspace.util.UUIDUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.rest.webmvc.ControllerUtils;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.util.MultiValueMap;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

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

    @Autowired
    ConverterService converter;

    @Override
    public void afterPropertiesSet() {
        List<Link> links = new ArrayList<>();
        for (String r : utils.getRepositories()) {
            // this doesn't work as we don't have an active http request
            // see https://github.com/spring-projects/spring-hateoas/issues/408
            // Link l = linkTo(this.getClass(), r).withRel(r);
            String[] split = r.split("\\.", 2);
            String plural = English.plural(split[1]);
            Link l = Link.of("/api/" + split[0] + "/" + plural, plural);
            links.add(l);
            log.debug(l.getRel().value() + " " + l.getHref());
        }
        discoverableEndpointsService.register(this, links);
    }


    /**
     * Called in GET is used to retrieve the single resource by identifier;
     *
     * Note that the regular expression in the request mapping accept a number as identifier;
     *
     * Please see {@link RestResourceController#findOne(String, String, String)} for findOne with string as
     * identifier
     * and see {@link RestResourceController#findOne(String, String, UUID)} for uuid as identifier
     *
     * @param apiCategory category from request
     * @param model model from request
     * @param id Identifier from request
     * @return single DSpaceResource
     */
    @RequestMapping(method = RequestMethod.GET, value = REGEX_REQUESTMAPPING_IDENTIFIER_AS_DIGIT)
    public HALResource<RestAddressableModel> findOne(@PathVariable String apiCategory, @PathVariable String model,
                                                        @PathVariable Integer id) {
        return findOneInternal(apiCategory, model, id);
    }

    /**
     * Called in GET is used to retrieve the single resource by identifier;
     *
     * Note that the regular expression in the request mapping accept a string as identifier but not the other kind
     * of identifier;
     *
     * http://<dspace.server.url>/api/{apiCategory}/{model}/{id}
     *
     * Example:
     * <pre>
     * {@code
     *    http://<dspace.server.url>/api/config/submissionsections/collection
     * }
     * </pre>
     *
     *
     * Please see {@link RestResourceController#findOne(String, String, Integer)} for findOne with number as
     * identifier
     * and see {@link RestResourceController#findOne(String, String, UUID)} for uuid as identifier
     *
     * @param apiCategory category from request
     * @param model model from request
     * @param id Identifier from request
     * @return single DSpaceResource
     */
    @RequestMapping(method = RequestMethod.GET, value = REGEX_REQUESTMAPPING_IDENTIFIER_AS_STRING_VERSION_STRONG)
    public HALResource<RestAddressableModel> findOne(@PathVariable String apiCategory, @PathVariable String model,
                                                        @PathVariable String id) {
        return findOneInternal(apiCategory, model, id);
    }

    /**
     * Called in GET is used to retrieve the single resource by identifier;
     *
     * Note that the regular expression in the request mapping accept a UUID as identifier;
     *
     * Please see {@link RestResourceController#findOne(String, String, Integer)} for findOne with number as
     * identifier
     * and see {@link RestResourceController#findOne(String, String, String)} for string as identifier
     *
     * @param apiCategory category from request
     * @param model model from request
     * @param uuid Identifier from request
     * @return single DSpaceResource
     */
    @RequestMapping(method = RequestMethod.GET, value = REGEX_REQUESTMAPPING_IDENTIFIER_AS_UUID)
    public HALResource<RestAddressableModel> findOne(@PathVariable String apiCategory, @PathVariable String model,
                                                        @PathVariable UUID uuid) {
        return findOneInternal(apiCategory, model, uuid);
    }

    /**
     * Internal method to retrieve single resource from an identifier of generic type
     *
     * @param apiCategory category from request
     * @param model model from request
     * @param id Identifier from request
     * @return single DSpaceResource
     */
    private <ID extends Serializable> HALResource<RestAddressableModel> findOneInternal(String apiCategory,
                                                                                           String model, ID id) {
        DSpaceRestRepository<RestAddressableModel, ID> repository = utils.getResourceRepository(apiCategory, model);
        Optional<RestAddressableModel> modelObject = Optional.empty();
        try {
            modelObject = repository.findById(id);
        } catch (ClassCastException e) {
            // ignore, as handled below
        }
        if (!modelObject.isPresent()) {
            throw new ResourceNotFoundException(apiCategory + "." + model + " with id: " + id + " not found");
        }
        return converter.toResource(modelObject.get());
    }

    /**
     * Called in GET is used to retrieve the relation resources;
     *
     * Note that the regular expression in the request mapping accept a number;
     *
     * @param request current HTTPServletRequest
     * @param apiCategory category from request
     * @param model model from request
     * @param id identifier from request
     * @param rel relation from request
     * @param page pagination information
     * @param assembler PagedResourcesAssembler
     * @return single RepresentationModel
     */
    @RequestMapping(method = RequestMethod.GET, value = REGEX_REQUESTMAPPING_IDENTIFIER_AS_DIGIT + "/{rel}")
    public RepresentationModel findRel(HttpServletRequest request, HttpServletResponse response,
                                   @PathVariable String apiCategory,
                                   @PathVariable String model, @PathVariable Integer id, @PathVariable String rel,
                                   Pageable page,
                                   PagedResourcesAssembler assembler) {
        return findRelInternal(request, response, apiCategory, model, id, rel, page, assembler);
    }

    /**
     * Called in GET is used to retrieve the relation resources;
     *
     * Note that the regular expression in the request mapping accept a string as identifier but not the other kind
     * of identifier;
     *
     * @param request current HTTPServletRequest
     * @param response HTTPServletResponse
     * @param apiCategory category from request
     * @param model model from request
     * @param id identifier from request
     * @param rel relation from request
     * @param page pagination information
     * @param assembler PagedResourcesAssembler
     * @return single RepresentationModel
     */
    @RequestMapping(method = RequestMethod.GET, value = REGEX_REQUESTMAPPING_IDENTIFIER_AS_STRING_VERSION_STRONG +
        "/{rel}")
    public RepresentationModel findRel(HttpServletRequest request, HttpServletResponse response,
                                   @PathVariable String apiCategory,
                                   @PathVariable String model, @PathVariable String id, @PathVariable String rel,
                                   Pageable page,
                                   PagedResourcesAssembler assembler) {
        return findRelInternal(request, response, apiCategory, model, id, rel, page, assembler);
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
     * @return
     */
    @RequestMapping(method = RequestMethod.GET, value = REGEX_REQUESTMAPPING_IDENTIFIER_AS_UUID + "/{rel}")
    public RepresentationModel findRel(HttpServletRequest request, HttpServletResponse response,
                                   @PathVariable String apiCategory,
                                   @PathVariable String model, @PathVariable UUID uuid, @PathVariable String rel,
                                   Pageable page,
                                   PagedResourcesAssembler assembler) {
        return findRelInternal(request, response, apiCategory, model, uuid, rel, page, assembler);
    }

    /**
     * Called in GET, try to retrieve the requested linked resource.
     *
     * Note that the regular expression in the request mapping accept a string as identifier but not the other kind
     * of identifier;
     *
     * http://<dspace.server.url>/api/{apiCategory}/{model}/{id}/{rel}/{relid}
     *
     * Example:
     * <pre>
     * {@code
     *      http://<dspace.server.url>/api/integration/authorities/SRJournalTitle/entryValues/1479-9995
     * }
     * </pre>
     *
     * Example:
     * <pre>
     * {@code
     *      http://<dspace.server.url>/api/integration/authorities/srsc/entries/VR110111
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
     * @return
     */
    @RequestMapping(method = RequestMethod.GET, value = REGEX_REQUESTMAPPING_IDENTIFIER_AS_STRING_VERSION_STRONG +
        "/{rel}/{relid}")
    public RepresentationModel findRel(HttpServletRequest request, HttpServletResponse response,
                                   @PathVariable String apiCategory,
                                   @PathVariable String model, @PathVariable String id, @PathVariable String rel,
                                   @PathVariable String relid,
                                   Pageable page, PagedResourcesAssembler assembler) throws Throwable {
        return findRelEntryInternal(request, response, apiCategory, model, id, rel, relid, page, assembler);
    }

    @RequestMapping(method = RequestMethod.GET, value = REGEX_REQUESTMAPPING_IDENTIFIER_AS_DIGIT +
        "/{rel}/{relid}")
    public RepresentationModel findRel(HttpServletRequest request, HttpServletResponse response,
                                       @PathVariable String apiCategory,
                                       @PathVariable String model, @PathVariable Integer id, @PathVariable String rel,
                                       @PathVariable String relid,
                                       Pageable page, PagedResourcesAssembler assembler) throws Throwable {
        return findRelEntryInternal(request, response, apiCategory, model, id.toString(), rel, relid, page, assembler);
    }
    /**
     * Execute a POST request;
     *
     * curl -X POST -H "Content-Type:application/json" http://<dspace.server.url>/api/{apiCategory}/{model}
     *
     * Example:
     * <pre>
     * {@code
     *      curl -X POST -H "Content-Type:application/json" http://<dspace.server.url>/api/submission/workspaceitems
     * }
     * </pre>
     *
     * @param request       The relevant request
     * @param apiCategory   The apiCategory to be used
     * @param model         The model to be used
     * @param parent        Optional parent identifier
     * @return              The relevant ResponseEntity for this request
     * @throws HttpRequestMethodNotSupportedException   If something goes wrong
     */
    @RequestMapping(method = RequestMethod.POST, consumes = {"application/json", "application/hal+json"})
    public ResponseEntity<RepresentationModel<?>> post(HttpServletRequest request,
                                                       @PathVariable String apiCategory,
                                                       @PathVariable String model,
                                                       @RequestParam(required = false) String parent)
        throws HttpRequestMethodNotSupportedException {
        return postJsonInternal(request, apiCategory, model, parent);
    }

    /**
     * Execute a POST request;
     *
     * curl -X POST -H "Content-Type:text/uri-list" http://<dspace.server.url>/api/{apiCategory}/{model}
     *
     * Example:
     * <pre>
     * {@code
     *      curl -X POST -H "Content-Type:text/uri-list" http://<dspace.server.url>/api/submission/workspaceitems
     * }
     * </pre>
     *
     * @param request       The relevant request
     * @param apiCategory   The apiCategory to be used
     * @param model         The model to be used
     * @return              The relevant ResponseEntity for this request
     * @throws HttpRequestMethodNotSupportedException   If something goes wrong
     */
    @RequestMapping(method = RequestMethod.POST, consumes = {"text/uri-list"})
    public ResponseEntity<RepresentationModel<?>> postWithUriListContentType(HttpServletRequest request,
                                                                             @PathVariable String apiCategory,
                                                                             @PathVariable String model)
        throws HttpRequestMethodNotSupportedException {
        return postUriListInternal(request, apiCategory, model);
    }

    /**
     * Internal method to execute POST with application/json MediaType;
     *
     * @param request       The relevant request
     * @param apiCategory   The apiCategory to be used
     * @param model         The model to be used
     * @param parent        The parent object id (optional)
     * @return              The relevant ResponseEntity for this request
     * @throws HttpRequestMethodNotSupportedException   If something goes wrong
     */
    public <ID extends Serializable> ResponseEntity<RepresentationModel<?>> postJsonInternal(HttpServletRequest request,
                                                                                             String apiCategory,
                                                                                             String model,
                                                                                             String parent)
        throws HttpRequestMethodNotSupportedException {
        checkModelPluralForm(apiCategory, model);
        DSpaceRestRepository<RestAddressableModel, ID> repository = utils.getResourceRepository(apiCategory, model);

        RestAddressableModel modelObject;
        if (parent != null) {
            UUID parentUuid = UUIDUtils.fromString(parent);
            modelObject = repository.createAndReturn(parentUuid);
        } else {
            modelObject = repository.createAndReturn();
        }
        if (modelObject == null) {
            return ControllerUtils.toEmptyResponse(HttpStatus.CREATED);
        }
        DSpaceResource result = converter.toResource(modelObject);
        //TODO manage HTTPHeader
        return ControllerUtils.toResponseEntity(HttpStatus.CREATED, new HttpHeaders(), result);
    }

    /**
     * Internal method to execute POST with text/uri-list MediaType;
     *
     * @param request       The relevant request
     * @param apiCategory   The apiCategory to be used
     * @param model         The model to be used
     * @return              The relevant ResponseEntity for this request
     * @throws HttpRequestMethodNotSupportedException   If something goes wrong
     */
    public <ID extends Serializable> ResponseEntity<RepresentationModel<?>> postUriListInternal(
            HttpServletRequest request,
            String apiCategory,
            String model)
        throws HttpRequestMethodNotSupportedException {
        checkModelPluralForm(apiCategory, model);
        DSpaceRestRepository<RestAddressableModel, ID> repository = utils.getResourceRepository(apiCategory, model);
        RestAddressableModel modelObject = null;
        List<String> stringListFromRequest = utils.getStringListFromRequest(request);
        try {
            modelObject = repository.createAndReturn(stringListFromRequest);
        } catch (ClassCastException e) {
            log.error("Something went wrong whilst creating the object for apiCategory: " + apiCategory +
                          " and model: " + model, e);
            return ControllerUtils.toEmptyResponse(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        if (modelObject == null) {
            return ControllerUtils.toEmptyResponse(HttpStatus.CREATED);
        }
        DSpaceResource result = converter.toResource(modelObject);
        //TODO manage HTTPHeader
        return ControllerUtils.toResponseEntity(HttpStatus.CREATED, new HttpHeaders(), result);
    }


    /**
     * Called in POST, with a x-www-form-urlencoded, execute an action on a resource
     *
     * Note that the regular expression in the request mapping accept a number as identifier;
     *
     * @param request
     * @param apiCategory
     * @param model
     * @param id
     * @return
     * @throws HttpRequestMethodNotSupportedException
     * @throws IOException
     * @throws SQLException
     */
    @RequestMapping(method = RequestMethod.POST, value = REGEX_REQUESTMAPPING_IDENTIFIER_AS_DIGIT, headers =
        "content-type=application/x-www-form-urlencoded")
    public ResponseEntity<RepresentationModel<?>> action(HttpServletRequest request, @PathVariable String apiCategory,
                                                         @PathVariable String model, @PathVariable Integer id)
        throws HttpRequestMethodNotSupportedException, SQLException, IOException {
        checkModelPluralForm(apiCategory, model);
        DSpaceRestRepository<RestAddressableModel, Integer> repository =
            utils.getResourceRepository(apiCategory, model);

        RestAddressableModel modelObject = null;
        try {
            modelObject = repository.action(request, id);
        } catch (UnprocessableEntityException e) {
            log.error(e.getMessage(), e);
            return ControllerUtils.toEmptyResponse(HttpStatus.UNPROCESSABLE_ENTITY);
        }

        if (modelObject != null) {
            DSpaceResource result = converter.toResource(modelObject);
            return ControllerUtils.toResponseEntity(HttpStatus.CREATED, new HttpHeaders(), result);
        } else {
            return ControllerUtils.toEmptyResponse(HttpStatus.NO_CONTENT);
        }
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
    public <ID extends Serializable> ResponseEntity<RepresentationModel<?>> upload(HttpServletRequest request,
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
     * @param uuid
     *            the uuid of the specific rest resource
     * @param uploadfile
     *            the file to upload
     * @return the created resource
     * @throws HttpRequestMethodNotSupportedException
     */
    @RequestMapping(method = RequestMethod.POST, value = REGEX_REQUESTMAPPING_IDENTIFIER_AS_UUID, headers =
        "content-type=multipart/form-data")
    public <ID extends Serializable> ResponseEntity<RepresentationModel<?>> upload(HttpServletRequest request,
                                                                                   @PathVariable String apiCategory,
                                                                                   @PathVariable String model,
                                                                                   @PathVariable UUID uuid,
                                                                                   @RequestParam("file") MultipartFile
                                                                                uploadfile)
        throws HttpRequestMethodNotSupportedException {
        return uploadInternal(request, apiCategory, model, uuid, uploadfile);
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
    private <ID extends Serializable> ResponseEntity<RepresentationModel<?>> uploadInternal(HttpServletRequest request,
                                                                                            String apiCategory,
                                                                                            String model,
                                                                                            ID id,
                                                                                            MultipartFile uploadfile) {
        checkModelPluralForm(apiCategory, model);
        DSpaceRestRepository<RestAddressableModel, ID> repository = utils.getResourceRepository(apiCategory, model);
        RestAddressableModel modelObject = null;
        try {
            modelObject = repository.upload(request, apiCategory, model, id, uploadfile);
        } catch (SQLException | IOException e) {
            throw new RuntimeException("Error " + e.getMessage() +
                                       " uploading file to " + model + " with ID= " + id, e);
        } catch ( AuthorizeException ae) {
            throw new RESTAuthorizationException(ae);
        }
        DSpaceResource result = converter.toResource(modelObject);
        return ControllerUtils.toResponseEntity(HttpStatus.CREATED, new HttpHeaders(), result);
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
    public <T extends RestAddressableModel> ResponseEntity<RepresentationModel<?>> upload(
            HttpServletRequest request,
            @PathVariable String apiCategory,
            @PathVariable String model,
            @RequestParam("file") List<MultipartFile> uploadfile)
        throws SQLException, FileNotFoundException, IOException, AuthorizeException {

        checkModelPluralForm(apiCategory, model);
        DSpaceRestRepository repository = utils.getResourceRepository(apiCategory, model);

        Iterable<T> content = repository.upload(request, uploadfile);

        List<DSpaceResource> resources = new ArrayList<>();
        for (T modelObject : content) {
            DSpaceResource result = converter.toResource(modelObject);
            resources.add(result);
        }
        return ControllerUtils.toResponseEntity(HttpStatus.OK, new HttpHeaders(), CollectionModel.wrap(resources));
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
    public ResponseEntity<RepresentationModel<?>> patch(HttpServletRequest request, @PathVariable String apiCategory,
                                                        @PathVariable String model, @PathVariable Integer id,
                                                        @RequestBody(required = true) JsonNode jsonNode) {
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
    public ResponseEntity<RepresentationModel<?>> patch(HttpServletRequest request, @PathVariable String apiCategory,
                                                        @PathVariable String model,
                                                        @PathVariable(name = "uuid") UUID id,
                                                        @RequestBody(required = true) JsonNode jsonNode) {
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
    public <ID extends Serializable> ResponseEntity<RepresentationModel<?>> patchInternal(HttpServletRequest request,
                                                                                          String apiCategory,
                                                                                          String model, ID id,
                                                                                          JsonNode jsonNode) {
        checkModelPluralForm(apiCategory, model);
        DSpaceRestRepository<RestAddressableModel, ID> repository = utils.getResourceRepository(apiCategory, model);
        RestAddressableModel modelObject = null;
        try {
            JsonPatchConverter patchConverter = new JsonPatchConverter(mapper);
            Patch patch = patchConverter.convert(jsonNode);
            modelObject = repository.patch(request, apiCategory, model, id, patch);
        } catch (RepositoryMethodNotImplementedException | UnprocessableEntityException |
            DSpaceBadRequestException | ResourceNotFoundException e) {
            log.error(e.getMessage(), e);
            throw e;
        }
        DSpaceResource result = converter.toResource(modelObject);
        //TODO manage HTTPHeader
        return ControllerUtils.toResponseEntity(HttpStatus.OK, new HttpHeaders(), result);

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
     * @return
     */
    private <ID extends Serializable> RepresentationModel findRelEntryInternal(HttpServletRequest request,
                                                                           HttpServletResponse response,
                                                                           String apiCategory, String model,
                                                                           String id, String rel, String relid,
                                                                           Pageable page,
                                                                           PagedResourcesAssembler assembler)
            throws Throwable {
        checkModelPluralForm(apiCategory, model);
        DSpaceRestRepository<RestAddressableModel, ID> repository = utils.getResourceRepository(apiCategory, model);
        Class<RestAddressableModel> domainClass = repository.getDomainClass();

        LinkRest linkRest = utils.getClassLevelLinkRest(rel, domainClass);
        if (linkRest != null) {
            LinkRestRepository linkRepository = utils.getLinkResourceRepository(apiCategory, model, linkRest.name());
            Method linkMethod = utils.requireMethod(linkRepository.getClass(), "getResource");
            try {
                Object object = linkMethod.invoke(linkRepository, request, id, relid, page, utils.obtainProjection());
                Link link = linkTo(this.getClass(), apiCategory, model).slash(id).slash(rel).slash(relid).withSelfRel();

                List result = new ArrayList();
                result.add(object);
                PageImpl<RestAddressableModel> pageResult = new PageImpl(result, page, 1);
                Page<HALResource> halResources = pageResult.map(restObject -> converter.toResource(restObject));
                return assembler.toModel(halResources, link);
            } catch (InvocationTargetException e) {
                // This catch has been made to resolve the issue that caused AuthorizeDenied exceptions for the methods
                // on the repository defined by the @PreAuthorize etc annotation to be absorbed by the reflection's
                // InvocationTargetException and thrown as a RunTimeException when it was actually an AccessDenied
                // Exception and it should be returned/shown as one
                if (e.getTargetException() instanceof AccessDeniedException ||
                    e.getTargetException() instanceof ResourceNotFoundException) {
                    throw e.getTargetException();
                } else {
                    throw new RuntimeException(e.getMessage(), e);
                }
            } catch (IllegalAccessException | IllegalArgumentException e) {
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
     * @param page
     * @param assembler
     * @return
     */
    private <ID extends Serializable> RepresentationModel findRelInternal(HttpServletRequest request,
                                                                      HttpServletResponse response, String apiCategory,
                                                                      String model, ID uuid, String subpath,
                                                                      Pageable page,
                                                                      PagedResourcesAssembler assembler) {
        checkModelPluralForm(apiCategory, model);
        DSpaceRestRepository<RestAddressableModel, ID> repository = utils.getResourceRepository(apiCategory, model);
        Class<RestAddressableModel> domainClass = repository.getDomainClass();

        LinkRest linkRest = utils.getClassLevelLinkRest(subpath, domainClass);
        PagedModel<? extends HALResource> result;

        if (linkRest != null) {
            LinkRestRepository linkRepository = utils.getLinkResourceRepository(apiCategory, model, linkRest.name());
            Method linkMethod = utils.requireMethod(linkRepository.getClass(), linkRest.method());
            try {
                if (Page.class.isAssignableFrom(linkMethod.getReturnType())) {
                    Page<? extends RestModel> pageResult = (Page<? extends RestModel>) linkMethod
                            .invoke(linkRepository, request, uuid, page, utils.obtainProjection());

                    if (pageResult == null) {
                        // Link repositories may throw an exception or return an empty page,
                        // but must never return null for a paged subresource.
                        log.error("Paged subresource link repository " + linkRepository.getClass()
                                + " incorrectly returned null for request with id " + uuid);
                        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                        return null;
                    }

                    Link link = null;
                    String querystring = request.getQueryString();
                    if (querystring != null && querystring.length() > 0) {
                        link = linkTo(this.getClass(), apiCategory, model).slash(uuid)
                            .slash(subpath + '?' + querystring).withSelfRel();
                    } else {
                        link = linkTo(this.getClass(), apiCategory, model).slash(uuid).slash(subpath).withSelfRel();
                    }

                    return EntityModel.of(new EmbeddedPage(link.getHref(),
                            pageResult.map(converter::toResource), null, subpath));
                } else {
                    RestModel object = (RestModel) linkMethod.invoke(linkRepository, request,
                            uuid, page, utils.obtainProjection());
                    if (object == null) {
                        response.setStatus(HttpServletResponse.SC_NO_CONTENT);
                        return null;
                    } else {
                        Link link = linkTo(this.getClass(), apiCategory, model).slash(uuid).slash(subpath)
                                .withSelfRel();
                        HALResource tmpresult = converter.toResource(object);
                        tmpresult.add(link);
                        return tmpresult;
                    }
                }
            } catch (InvocationTargetException e) {
                if (e.getTargetException() instanceof RuntimeException) {
                    throw (RuntimeException) e.getTargetException();
                } else {
                    throw new RuntimeException(e);
                }
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
        RestModel modelObject = repository.findById(uuid).orElse(null);

        if (modelObject == null) {
            throw new ResourceNotFoundException(apiCategory + "." + model + " with id: " + uuid + " not found");
        }

        DSpaceResource resource = converter.toResource(modelObject);

        String rel = null;

        for (Link l : resource.getLinks()) {
            if (l.isTemplated()) {
                if (l.getHref().substring(0, l.getHref().indexOf("?")).contentEquals(request.getRequestURL())) {
                    rel = l.getRel().value();
                }
            } else if (l.getHref().contentEquals(request.getRequestURL())) {
                rel = l.getRel().value();
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
                result = assembler.toModel(pageResult);
                return result;
            }
            int start = Math.toIntExact(page.getOffset());
            int end = (start + page.getPageSize()) > fullList.size() ? fullList.size() : (start + page.getPageSize());
            DSpaceRestRepository<RestAddressableModel, ?> resourceRepository = utils
                .getResourceRepository(fullList.get(0).getCategory(), fullList.get(0).getType());
            PageImpl<RestAddressableModel> pageResult = new PageImpl(fullList.subList(start, end), page,
                                                                     fullList.size());
            return assembler.toModel(pageResult.map(converter::toResource));
        } else {
            if (resource.getEmbeddedResources().get(rel) == null) {
                response.setStatus(HttpServletResponse.SC_NO_CONTENT);
            }
            return (RepresentationModel) resource.getEmbeddedResources().get(rel);
        }

    }

    /**
     * Find all
     *
     * @param apiCategory
     * @param model
     * @param page
     * @param assembler
     * @return
     */
    @RequestMapping(method = RequestMethod.GET)
    @SuppressWarnings("unchecked")
    public <T extends RestAddressableModel> PagedModel<DSpaceResource<T>> findAll(@PathVariable String apiCategory,
            @PathVariable String model, Pageable page, PagedResourcesAssembler assembler, HttpServletResponse response,
            @RequestParam MultiValueMap<String, Object> parameters) {

        String encodedParameterString = getEncodedParameterStringFromRequestParams(parameters);
        DSpaceRestRepository<T, ?> repository = utils.getResourceRepository(apiCategory, model);
        Link link = linkTo(this.getClass(), apiCategory, model).slash(encodedParameterString).withSelfRel();

        Page<DSpaceResource<T>> resources;
        try {
            resources = repository.findAll(page).map(converter::toResource);
        } catch (PaginationException pe) {
            resources = new PageImpl<>(new ArrayList<>(), page, pe.getTotal());
        }
        PagedModel<DSpaceResource<T>> result = assembler.toModel(resources, link);
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
    public RepresentationModel listSearchMethods(@PathVariable String apiCategory, @PathVariable String model) {
        checkModelPluralForm(apiCategory, model);
        RepresentationModel root = new RepresentationModel();
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
    public <T extends RestAddressableModel> RepresentationModel executeSearchMethods(
            @PathVariable String apiCategory,
            @PathVariable String model,
            @PathVariable String searchMethodName,
            HttpServletResponse response,
            Pageable pageable, Sort sort,
            PagedResourcesAssembler assembler,
            @RequestParam MultiValueMap<String, Object> parameters)
        throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        String encodedParameterString = getEncodedParameterStringFromRequestParams(parameters);

        Link link = linkTo(this.getClass(), apiCategory, model).slash("search").slash(searchMethodName)
                                                               .slash(encodedParameterString).withSelfRel();
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
        RepresentationModel result = null;
        if (returnPage) {
            Page<DSpaceResource<T>> resources;
            if (searchResult == null) {
                resources = new PageImpl(new ArrayList(), pageable, 0);
            } else {
                resources = ((Page<T>) searchResult).map(converter::toResource);
            }
            result = assembler.toModel(resources, link);
        } else {
            if (searchResult == null) {
                response.setStatus(HttpServletResponse.SC_NO_CONTENT);
                return null;
            }
            return converter.toResource((T) searchResult);
        }
        return result;
    }

    /**
     * Internal method to convert the parameters provided as a MultivalueMap as a string to use in the self-link.
     * This function will exclude all "embed" parameters and parameters starting with "embed."
     * @param parameters
     * @return encoded uriString containing request parameters without embed parameter
     */
    private String getEncodedParameterStringFromRequestParams(
            @RequestParam MultiValueMap<String, Object> parameters) {
        UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.newInstance();

        for (String key : parameters.keySet()) {
            if (!StringUtils.equals(key, "embed") && !StringUtils.startsWith(key, "embed.")) {
                uriComponentsBuilder.queryParam(key, parameters.get(key));
            }
        }
        return uriComponentsBuilder.encode().build().toString();
    }

    @RequestMapping(method = RequestMethod.DELETE, value = REGEX_REQUESTMAPPING_IDENTIFIER_AS_DIGIT)
    public ResponseEntity<RepresentationModel<?>> delete(HttpServletRequest request, @PathVariable String apiCategory,
                                                         @PathVariable String model, @PathVariable Integer id)
        throws HttpRequestMethodNotSupportedException {
        return deleteInternal(apiCategory, model, id);
    }

    @RequestMapping(method = RequestMethod.DELETE, value = REGEX_REQUESTMAPPING_IDENTIFIER_AS_UUID)
    public ResponseEntity<RepresentationModel<?>> delete(HttpServletRequest request, @PathVariable String apiCategory,
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
    private <ID extends Serializable> ResponseEntity<RepresentationModel<?>> deleteInternal(String apiCategory,
                                                                                            String model,
                                                                                            ID id) {
        checkModelPluralForm(apiCategory, model);
        DSpaceRestRepository<RestAddressableModel, ID> repository = utils.getResourceRepository(apiCategory, model);
        repository.deleteById(id);
        return ControllerUtils.toEmptyResponse(HttpStatus.NO_CONTENT);
    }

    /**
     * Execute a PUT request for an entity with id of type UUID;
     *
     * curl -X PUT http://<dspace.server.url>/api/{apiCategory}/{model}/{uuid}
     *
     * Example:
     * <pre>
     * {@code
     *      curl -X PUT http://<dspace.server.url>/api/core/collection/8b632938-77c2-487c-81f0-e804f63e68e6
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
        return putOneJsonInternal(request, apiCategory, model, uuid, jsonNode);
    }

    /**
     * Execute a PUT request for an entity with id of type Integer;
     *
     * curl -X PUT -H "Content-Type:application/json" http://<dspace.server.url>/api/{apiCategory}/{model}/{id}
     *
     * Example:
     * <pre>
     * {@code
     *      curl -X PUT -H "Content-Type:application/json" http://<dspace.server.url>/api/core/metadatafield/1
     * }
     * </pre>
     *
     * @param request     the http request
     * @param apiCategory the API category e.g. "api"
     * @param model       the DSpace model e.g. "collection"
     * @param id        the ID of the target REST object
     * @param jsonNode    the part of the request body representing the updated rest object
     * @return the relevant REST resource
     */
    @RequestMapping(method = RequestMethod.PUT, value = REGEX_REQUESTMAPPING_IDENTIFIER_AS_DIGIT,
        consumes = {"application/json", "application/hal+json"})
    public DSpaceResource<RestAddressableModel> put(HttpServletRequest request,
                                                    @PathVariable String apiCategory, @PathVariable String model,
                                                    @PathVariable Integer id,
                                                    @RequestBody(required = true) JsonNode jsonNode) {
        return putOneJsonInternal(request, apiCategory, model, id, jsonNode);
    }

    /**
     * Execute a PUT request for an entity with id of type Integer;
     *
     * curl -X PUT -H "Content-Type:text/uri-list" http://<dspace.server.url>/api/{apiCategory}/{model}/{id}
     *
     * Example:
     * <pre>
     * {@code
     *      curl -X PUT -H "Content-Type:text/uri-list" http://<dspace.server.url>/api/core/metadatafield/1
     * }
     * </pre>
     *
     * @param request     the http request
     * @param apiCategory the API category e.g. "api"
     * @param model       the DSpace model e.g. "collection"
     * @param id        the ID of the target REST object
     * @return the relevant REST resource
     */
    @RequestMapping(method = RequestMethod.PUT, value = REGEX_REQUESTMAPPING_IDENTIFIER_AS_DIGIT,
        consumes = {"text/uri-list"})
    public DSpaceResource<RestAddressableModel> put(HttpServletRequest request,
                                                    @PathVariable String apiCategory, @PathVariable String model,
                                                    @PathVariable Integer id) throws IOException {
        return putOneUriListInternal(request, apiCategory, model, id);
    }

    /**
     * Execute a PUT request for an entity with id of type String and containing
     * 32 hexadecimal digits.
     *
     * <p>
     * curl -X PUT -H "Content-Type:application/json" http://<dspace.server.url>/api/{apiCategory}/{model}/{id}
     *
     * <p>Example:
     * <pre>
     * {@code
     *      curl -X PUT -H "Content-Type:application/json" http://<dspace.server.url>/api/core/metadatafield/d41d8cd98f00b204e9800998ecf8427e
     * }
     * </pre>
     *
     * @param request     the http request
     * @param apiCategory the API category e.g. "api"
     * @param model       the DSpace model e.g. "collection"
     * @param id        the ID of the target REST object
     * @param jsonNode    the part of the request body representing the updated rest object
     * @return the relevant REST resource
     */
    @RequestMapping(method = RequestMethod.PUT, value = REGEX_REQUESTMAPPING_IDENTIFIER_AS_HEX32,
        consumes = {"application/json", "application/hal+json"})
    public DSpaceResource<RestAddressableModel> put(HttpServletRequest request,
                                                    @PathVariable String apiCategory, @PathVariable String model,
                                                    @PathVariable String id,
                                                    @RequestBody(required = true) JsonNode jsonNode) {
        return putOneJsonInternal(request, apiCategory, model, id, jsonNode);
    }

    /**
     * Internal method to execute PUT with application/json MediaType;
     *
     * @param request       The relevant request
     * @param apiCategory   The apiCategory to be used
     * @param model         The model to be used
     * @param id            The ID for the resource to be altered by the PUT
     * @param jsonNode      The relevant JsonNode to be used by the PUT
     * @return              The relevant DSpaceResource for this request
     */
    private <ID extends Serializable> DSpaceResource<RestAddressableModel> putOneJsonInternal(
        HttpServletRequest request, String apiCategory, String model, ID id, JsonNode jsonNode) {
        checkModelPluralForm(apiCategory, model);
        DSpaceRestRepository<RestAddressableModel, ID> repository = utils.getResourceRepository(apiCategory, model);
        RestAddressableModel modelObject = null;
        modelObject = repository.put(request, apiCategory, model, id, jsonNode);
        if (modelObject == null) {
            throw new ResourceNotFoundException(apiCategory + "." + model + " with id: " + id + " not found");
        }
        return converter.toResource(modelObject);

    }
    /**
     * Internal method to execute PUT with text/uri-list MediaType;
     *
     * @param request       The relevant request
     * @param apiCategory   The apiCategory to be used
     * @param model         The model to be used
     * @param id            The ID for the resource to be altered by the PUT
     * @return              The relevant DSpaceResource for this request
     * @throws IOException  If something goes wrong
     */
    private <ID extends Serializable> DSpaceResource<RestAddressableModel> putOneUriListInternal(
        HttpServletRequest request, String apiCategory, String model, ID id) throws IOException {
        checkModelPluralForm(apiCategory, model);
        DSpaceRestRepository<RestAddressableModel, ID> repository = utils.getResourceRepository(apiCategory, model);
        RestAddressableModel modelObject = null;
        List<String> stringList = utils.getStringListFromRequest(request);
        modelObject = repository.put(request, apiCategory, model, id, stringList);
        if (modelObject == null) {
            throw new ResourceNotFoundException(apiCategory + "." + model + " with id: " + id + " not found");
        }
        return converter.toResource(modelObject);
    }
}
