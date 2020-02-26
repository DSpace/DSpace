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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dspace.app.rest.converter.ConverterService;
import org.dspace.app.rest.link.HalLinkService;
import org.dspace.app.rest.model.AuthorityRest;
import org.dspace.app.rest.model.LinkRest;
import org.dspace.app.rest.model.RestAddressableModel;
import org.dspace.app.rest.model.RestModel;
import org.dspace.app.rest.model.hateoas.HALResource;
import org.dspace.app.rest.repository.DSpaceRestRepository;
import org.dspace.app.rest.repository.LinkRestRepository;
import org.dspace.app.rest.utils.RestRepositoryUtils;
import org.dspace.app.rest.utils.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * This class provides a controller for OpenSearch support.
 * It creates a namespace /opensearch in the DSpace REST webapp.
 *
 * @author Giuseppe Digilio (giuseppe.digilio at 4science.it)
 */
@RestController
@RequestMapping("/api/" + AuthorityRest.CATEGORY + "/" + AuthorityEntrySearchRestController.MODEL)
public class AuthorityEntrySearchRestController {
    public static final String MODEL = "authorities";

    public static final String SEARCH = "search";

    /**
     * Regular expression in the request mapping to accept a string as identifier but not the other kind of
     * identifier (digits or uuid)
     */
    private static final String REGEX_REQUESTMAPPING_IDENTIFIER_AS_STRING_VERSION_STRONG = "/{id:^(?!^\\d+$)" +
        "(?!^[0-9a-fxA-FX]{8}-[0-9a-fxA-FX]{4}-[0-9a-fxA-FX]{4}-[0-9a-fxA-FX]{4}-[0-9a-fxA-FX]{12}$)[\\w+\\-]+$+}";

    @Autowired
    ConverterService converter;

    @Autowired
    HalLinkService linkService;

    @Autowired
    RestRepositoryUtils repositoryUtils;

    @Autowired
    Utils utils;

    @RequestMapping(method = RequestMethod.GET, value = REGEX_REQUESTMAPPING_IDENTIFIER_AS_STRING_VERSION_STRONG +
            "/" + AuthorityRest.ENTRIES + "/" + AuthorityEntrySearchRestController.SEARCH)
    public ResourceSupport listSearchMethods(@PathVariable String id) {
        ResourceSupport root = new ResourceSupport();
        List<String> searchMethods = new ArrayList<String>(Arrays.asList("top", "byParent"));

        for (String name : searchMethods) {
            Link link = linkTo(this.getClass(), AuthorityRest.CATEGORY, AuthorityEntrySearchRestController.MODEL)
                    .slash(id).slash(AuthorityRest.ENTRIES).slash("search").slash(name).withRel(name);

            root.add(link);
        }
        return root;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @RequestMapping(method = RequestMethod.GET, value = REGEX_REQUESTMAPPING_IDENTIFIER_AS_STRING_VERSION_STRONG +
            "/" + AuthorityRest.ENTRIES + "/" + AuthorityEntrySearchRestController.SEARCH + "/top")
    public <ID extends Serializable> ResourceSupport findAlltop(@PathVariable String id,
                                                                HttpServletRequest request,
                                                                HttpServletResponse response,
                                                                Pageable pageable, Sort sort,
                                                                PagedResourcesAssembler assembler,
                                                                @RequestParam MultiValueMap<String,
                                                                Object> parameters)
        throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {

        PagedResources<? extends HALResource> result = null;
        DSpaceRestRepository<RestAddressableModel, ID> repository = utils.getResourceRepository(
                AuthorityRest.CATEGORY, AuthorityEntrySearchRestController.MODEL);
        Class<RestAddressableModel> domainClass = repository.getDomainClass();
        String searchMethodName = "top";

        LinkRest linkRest = utils.getClassLevelLinkRest(AuthorityRest.ENTRIES, domainClass);

        if (linkRest != null) {
            LinkRestRepository linkRepository = utils.getLinkResourceRepository(AuthorityRest.CATEGORY,
                    AuthorityEntrySearchRestController.MODEL, linkRest.name());
            Method linkMethod = repositoryUtils.getSearchMethod(searchMethodName, linkRepository);
            String querystring = request.getQueryString();
            Link link;
            if (querystring != null && querystring.length() > 0) {
                link = linkTo(this.getClass(), AuthorityRest.CATEGORY, AuthorityEntrySearchRestController.MODEL)
                        .slash(id).slash(AuthorityRest.ENTRIES).slash("search")
                        .slash(searchMethodName + '?' + querystring).withSelfRel();
            } else {
                link = linkTo(this.getClass(), AuthorityRest.CATEGORY, AuthorityEntrySearchRestController.MODEL)
                        .slash(id).slash(AuthorityRest.ENTRIES).slash("search").slash(searchMethodName).withSelfRel();
            }

            Page<? extends RestModel> pageResult = (Page<? extends RestAddressableModel>) linkMethod
                    .invoke(linkRepository, id, pageable, utils.obtainProjection());

            Page<HALResource> halResources = pageResult.map(restObject -> converter.toResource(restObject));

            return assembler.toResource(halResources, link);
        } else {
            throw new ResourceNotFoundException(AuthorityRest.ENTRIES + " undefined for "
                    + AuthorityEntrySearchRestController.MODEL);
        }

    }

    @RequestMapping(method = RequestMethod.GET, value = REGEX_REQUESTMAPPING_IDENTIFIER_AS_STRING_VERSION_STRONG +
            "/" + AuthorityRest.ENTRIES + "/" + AuthorityEntrySearchRestController.SEARCH + "/byParent")
    public <ID extends Serializable> ResourceSupport findByParent(@PathVariable String id,
                                                                  HttpServletRequest request,
                                                                  HttpServletResponse response,
                                                                  Pageable pageable, Sort sort,
                                                                  PagedResourcesAssembler assembler,
                                                                  @RequestParam MultiValueMap<String,
                                                                  Object> parameters)
        throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {

        PagedResources<? extends HALResource> result = null;
        DSpaceRestRepository<RestAddressableModel, ID> repository = utils.getResourceRepository(
                AuthorityRest.CATEGORY, AuthorityEntrySearchRestController.MODEL);
        Class<RestAddressableModel> domainClass = repository.getDomainClass();
        String searchMethodName = "byParent";

        LinkRest linkRest = utils.getClassLevelLinkRest(AuthorityRest.ENTRIES, domainClass);

        if (linkRest != null) {
            LinkRestRepository linkRepository = utils.getLinkResourceRepository(AuthorityRest.CATEGORY,
                    AuthorityEntrySearchRestController.MODEL, linkRest.name());
            Method linkMethod = repositoryUtils.getSearchMethod(searchMethodName, linkRepository);

            String querystring = request.getQueryString();
            Link link;
            if (querystring != null && querystring.length() > 0) {
                link = linkTo(this.getClass(), AuthorityRest.CATEGORY, AuthorityEntrySearchRestController.MODEL)
                        .slash(id).slash(AuthorityRest.ENTRIES).slash("search")
                        .slash(searchMethodName + '?' + querystring).withSelfRel();
            } else {
                link = linkTo(this.getClass(), AuthorityRest.CATEGORY, AuthorityEntrySearchRestController.MODEL)
                        .slash(id).slash(AuthorityRest.ENTRIES).slash("search").slash(searchMethodName).withSelfRel();
            }

            Page<? extends RestModel> pageResult = (Page<? extends RestAddressableModel>) linkMethod
                    .invoke(linkRepository, request, id, pageable, utils.obtainProjection());

            Page<HALResource> halResources = pageResult.map(restObject -> converter.toResource(restObject));

            return assembler.toResource(halResources, link);
        } else {
            throw new ResourceNotFoundException(AuthorityRest.ENTRIES + " undefined for "
                    + AuthorityEntrySearchRestController.MODEL);
        }

    }
}
