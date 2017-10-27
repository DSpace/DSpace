/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.link;

import org.dspace.app.rest.model.hateoas.HALResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.Link;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created by raf on 25/09/2017.
 */
@Component
@ComponentScan
public class HalLinkService {

    @Autowired
    List<HalLinkFactory> halLinkFactoryLinkedList;

    public void addLinks(HALResource halResource, Pageable pageable){
        LinkedList<Link> links = new LinkedList<>();

        for(HalLinkFactory halLinkFactory : halLinkFactoryLinkedList){
            if(halLinkFactory.supports(halResource.getClass())){
                links.addAll(halLinkFactory.getLinksFor(halResource, pageable));
            }
        }

        halResource.add(links);

        for (Object obj : halResource.getEmbeddedResources().values()) {
            if(obj instanceof Collection) {
                for (Object subObj : (Collection) obj) {
                    if(subObj instanceof HALResource) {
                        addLinks((HALResource) subObj);
                    }
                }
            } else if(obj instanceof Map) {
                for (Object subObj : ((Map) obj).values()) {
                    if(subObj instanceof HALResource) {
                        addLinks((HALResource) subObj);
                    }
                }
            } else if(obj instanceof HALResource) {
                addLinks((HALResource) obj);
            }
        }
    }

    public void addLinks(HALResource halResource) {
        addLinks(halResource, null);
    }

}
