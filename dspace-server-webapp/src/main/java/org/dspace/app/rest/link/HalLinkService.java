/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.link;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.model.hateoas.EmbeddedPage;
import org.dspace.app.rest.model.hateoas.HALResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.Link;
import org.springframework.stereotype.Component;

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

    private Map<String, List<HalLinkFactory>> cachedMappings = new ConcurrentHashMap<>();

    public void addLinks(HALResource halResource, Pageable pageable) throws Exception {
        LinkedList<Link> links = new LinkedList<>();

        List<HalLinkFactory> supportedFactories = getSupportedFactories(halResource);
        for (HalLinkFactory halLinkFactory : supportedFactories) {
            links.addAll(halLinkFactory.getLinksFor(halResource, pageable));
        }

        links.sort((Link l1, Link l2) -> ObjectUtils.compare(l1.getRel(), l2.getRel()));

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
