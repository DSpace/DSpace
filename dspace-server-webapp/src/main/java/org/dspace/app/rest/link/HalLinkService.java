/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.link;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.exception.RepositoryNotFoundException;
import org.dspace.app.rest.model.LinkRest;
import org.dspace.app.rest.model.RestAddressableModel;
import org.dspace.app.rest.model.hateoas.EmbeddedPage;
import org.dspace.app.rest.model.hateoas.HALResource;
import org.dspace.app.rest.projection.Projection;
import org.dspace.app.rest.repository.DSpaceRestRepository;
import org.dspace.app.rest.repository.LinkRestRepository;
import org.dspace.app.rest.utils.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Links;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * This service will try to add links to the given HAL resource by iterating over all the configured factories
 * The links will only be added if the factories are allowed to do so by checking the resource's type.
 */
@Component
@ComponentScan
public class HalLinkService {

    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(HalLinkService.class);

    @Autowired
    private List<HalLinkFactory> halLinkFactories;

    @Autowired
    Utils utils;

    private Map<String, List<HalLinkFactory>> cachedMappings = new ConcurrentHashMap<>();

    public void addLinks(HALResource halResource, Pageable pageable) throws Exception {
        LinkedList<Link> links = new LinkedList<>();

        List<HalLinkFactory> supportedFactories = getSupportedFactories(halResource);
        for (HalLinkFactory halLinkFactory : supportedFactories) {
            links.addAll(halLinkFactory.getLinksFor(halResource, pageable));
        }

        links.sort((Link l1, Link l2) -> ObjectUtils.compare(l1.getRel().value(), l2.getRel().value()));

        halResource.add(links);

        for (Object obj : halResource.getEmbeddedResources().values()) {
            if (obj instanceof Collection) {
                for (Object subObj : (Collection) obj) {
                    if (subObj instanceof HALResource) {
                        addLinks((HALResource) subObj);
                    }
                }
            } else if (obj instanceof Map) {
                for (Object subObj : ((Map) obj).values()) {
                    if (subObj instanceof HALResource) {
                        addLinks((HALResource) subObj);
                    }
                }
            } else if (obj instanceof EmbeddedPage) {
                for (Map.Entry<String, List> pageContent : ((EmbeddedPage) obj).getPageContent().entrySet()) {
                    for (Object subObj : CollectionUtils.emptyIfNull(pageContent.getValue())) {
                        if (subObj instanceof HALResource) {
                            addLinks((HALResource) subObj);
                        }
                    }
                }
            } else if (obj instanceof HALResource) {
                addLinks((HALResource) obj);
            }
        }

        this.addProjectionParamsToLinks(halResource);
    }

    /**
     * - Links of the give HALResource are removed
     * - For each link the query parameters (if any) linked to the projection of this request are added to the link href
     * and the link is added back to the resource
     * - If anything goes wrong the original link is added back to the resource and error is logged
     *
     * @param halResource Resource to update the links of with the projection query parameters
     */
    private void addProjectionParamsToLinks(HALResource halResource) {
        // Removed original links and replace them with the links with the added projection query params
        Links originalLinks = halResource.getLinks();
        halResource.removeLinks();
        for (Link link : originalLinks) {
            try {
                HALResource<? extends RestAddressableModel> resource = (HALResource<RestAddressableModel>) halResource;
                Projection projection = resource.getContent().getProjection();
                LinkRestRepository linkRepository = null;
                Method method = null;
                try {
                    linkRepository = utils.getLinkResourceRepository(resource.getContent().getCategory(),
                        resource.getContent().getType(), link.getRel().value());
                    DSpaceRestRepository repository =
                        utils.getResourceRepository(resource.getContent().getCategory(),
                            resource.getContent().getType());
                    Class<RestAddressableModel> domainClass = repository.getDomainClass();
                    LinkRest linkRest = utils.getClassLevelLinkRest(link.getRel().value(), domainClass);
                    method = utils.requireMethod(linkRepository.getClass(), linkRest.method());
                } catch (RepositoryNotFoundException e) {
                    log.debug("Couldn't find the LinkRestRepository or DSpaceRestRepository corresponding to this " +
                              "link ({}) \n {}", link, e.getMessage());
                }
                Map<String, List<String>> projectionParameters =
                    projection.getProjectionParametersForHalLink(linkRepository, method);
                halResource.add(this.buildNewHrefWithProjectionQueryParams(link, projectionParameters));
            } catch (Exception e) {
                log.error("Something went wrong trying to add projection query params to this link ({}) \n {}",
                    link, e.getMessage(), e);
                // If anything else goes wrong, add the original link back in
                halResource.add(link);
            }
        }
    }

    /**
     * Adds the given projectionParameters as query parameters to the given link, if they are not already present
     *
     * @param link                 The link to add projection query parameters to
     * @param projectionParameters The query parameters map related to the projection
     * @return The original link, with possible projection query parameters added (if not already present)
     */
    private Link buildNewHrefWithProjectionQueryParams(Link link, Map<String, List<String>> projectionParameters) {
        UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.fromUri(link.toUri());

        // Check existing query params & Convert param values to set to prevent duplicates in the links
        MultiValueMap<String, String> existingQueryParams = uriComponentsBuilder.build().getQueryParams();
        for (Map.Entry<String, List<String>> projectionParamEntry : projectionParameters.entrySet()) {
            String projectionParamKey = projectionParamEntry.getKey();
            Set<String> paramValuesSet = Set.copyOf(projectionParameters.get(projectionParamKey));
            if (existingQueryParams.containsKey(projectionParamKey)) {
                Set<String> paramValuesSetWithoutDuplicates = new HashSet<>();
                for (String value : paramValuesSet) {
                    if (!existingQueryParams.get(projectionParamKey).contains(value)) {
                        paramValuesSetWithoutDuplicates.add(value);
                    }
                }
                if (!paramValuesSetWithoutDuplicates.isEmpty()) {
                    uriComponentsBuilder.queryParam(projectionParamKey, paramValuesSetWithoutDuplicates);
                }
            } else {
                uriComponentsBuilder.queryParam(projectionParamKey, paramValuesSet);
            }
        }

        String href = uriComponentsBuilder.build().toString();
        return new Link(href, link.getRel());
    }

    private List<HalLinkFactory> getSupportedFactories(HALResource halResource) {
        List<HalLinkFactory> factories = cachedMappings.get(getKey(halResource));

        if (factories == null) {
            //Go over all factories and collect the ones that support the current resource
            factories = halLinkFactories.stream()
                                        .filter(halLinkFactory -> halLinkFactory.supports(halResource.getClass()))
                                        .collect(Collectors.toList());

            cachedMappings.put(getKey(halResource), factories);
        }

        return factories;
    }

    private String getKey(final HALResource halResource) {
        return halResource.getClass().getCanonicalName();
    }

    public HALResource addLinks(HALResource halResource) {
        try {
            addLinks(halResource, null);
        } catch (Exception ex) {
            log.warn("Unable to add links to HAL resource " + halResource, ex);
        }
        return halResource;
    }

}
