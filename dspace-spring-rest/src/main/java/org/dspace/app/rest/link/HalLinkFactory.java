package org.dspace.app.rest.link;

import org.apache.poi.ss.formula.functions.T;
import org.dspace.app.rest.DiscoveryRestController;
import org.dspace.app.rest.model.hateoas.HALResource;
import org.dspace.app.rest.model.hateoas.SearchResultsResource;
import org.springframework.hateoas.Link;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

/**
 * Created by raf on 25/09/2017.
 */
@Component
public abstract class HalLinkFactory<RESOURCE, CONTROLLER> {

    public boolean supports(Class clazz) {
        if(Objects.equals(clazz, getResourceClass())){
            return true;
        }
        return false;
    }

    public List<Link> getLinksFor(HALResource halResource) {
        LinkedList<Link> list = new LinkedList<>();

        if(halResource != null && supports(halResource.getClass())){
            addLinks((RESOURCE) halResource, list);
        }

        return list;
    }


    protected <T> Link buildLink(String rel, T data) {
        UriComponentsBuilder uriComponentsBuilder = linkTo(data)
                .toUriComponentsBuilder();

        return buildLink(rel, uriComponentsBuilder.build().toString());
    }

    protected Link buildLink(String rel, String href) {
        Link link = new Link(href, rel);

        return link;
    }

    protected CONTROLLER getMethodOn() {
        return methodOn(getControllerClass());
    }

    protected <C> C getMethodOn(Class<C> clazz) {
        return methodOn(clazz);
    }

    protected abstract void addLinks(RESOURCE halResource, LinkedList<Link> list);

    protected abstract Class<CONTROLLER> getControllerClass();

    protected abstract Class<RESOURCE> getResourceClass();

}
