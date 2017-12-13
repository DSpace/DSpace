/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.link;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

import java.util.LinkedList;
import java.util.List;

import org.dspace.app.rest.model.hateoas.HALResource;
import org.dspace.app.rest.utils.URLUtils;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.Link;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Created by raf on 25/09/2017.
 */
@Component
public abstract class HalLinkFactory<RESOURCE, CONTROLLER> {

    public boolean supports(Class clazz) {
        if(getResourceClass().isAssignableFrom(clazz)){
            return true;
        }
        return false;
    }

    public List<Link> getLinksFor(HALResource halResource, Pageable pageable) {
        LinkedList<Link> list = new LinkedList<>();

        if(halResource != null && supports(halResource.getClass())){
            addLinks((RESOURCE) halResource, pageable, list);
        }

        return list;
    }


    protected  <T> Link buildLink(String rel, T data) {
        UriComponentsBuilder uriComponentsBuilder = uriBuilder(data);

        return buildLink(rel, uriComponentsBuilder.build().toUriString());
    }

    protected <T> UriComponentsBuilder uriBuilder(T data) {
        return linkTo(data)
                    .toUriComponentsBuilder();
    }

    protected Link buildLink(String rel, String href) {
        Link link = new Link(URLUtils.decode(href), rel);

        return link;
    }

    protected CONTROLLER getMethodOn() {
        return methodOn(getControllerClass());
    }

    protected <C> C getMethodOn(Class<C> clazz) {
        return methodOn(clazz);
    }

    protected abstract void addLinks(RESOURCE halResource, Pageable pageable, LinkedList<Link> list);

    protected abstract Class<CONTROLLER> getControllerClass();

    protected abstract Class<RESOURCE> getResourceClass();

}
