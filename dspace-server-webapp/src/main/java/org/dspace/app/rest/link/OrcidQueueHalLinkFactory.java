/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.link;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;

import java.util.LinkedList;

import org.atteo.evo.inflector.English;
import org.dspace.app.rest.RestResourceController;
import org.dspace.app.rest.model.ItemRest;
import org.dspace.app.rest.model.OrcidQueueRest;
import org.dspace.app.rest.model.hateoas.OrcidQueueResource;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.Link;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * This class' purpose is to provide a factory to add links to the OrcidQueueResource.
 * The addLinks factory will be called from the HalLinkService class addLinks method.
 * 
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
@Component
public class OrcidQueueHalLinkFactory extends HalLinkFactory<OrcidQueueResource, RestResourceController> {

    @Override
    protected void addLinks(OrcidQueueResource halResource, Pageable pageable, LinkedList<Link> list)
            throws Exception {

        OrcidQueueRest orcidQueueRest = halResource.getContent();
        if (orcidQueueRest.getProfileItemId() != null) {
            UriComponentsBuilder uriComponentsBuilder = linkTo(getMethodOn(ItemRest.CATEGORY, ItemRest.NAME)
                                       .findRel(null, null, ItemRest.CATEGORY, English.plural(ItemRest.NAME),
                                        orcidQueueRest.getProfileItemId(), "", null, null)).toUriComponentsBuilder();
            String uribuilder = uriComponentsBuilder.build().toString();
            list.add(buildLink("profileItem", uribuilder.substring(0, uribuilder.lastIndexOf("/"))));
        }

        if (orcidQueueRest.getEntityId() != null) {
            UriComponentsBuilder uriComponentsBuilder = linkTo(getMethodOn(ItemRest.CATEGORY, ItemRest.NAME)
                                       .findRel(null, null, ItemRest.CATEGORY, English.plural(ItemRest.NAME),
                                        orcidQueueRest.getEntityId(), "", null, null)).toUriComponentsBuilder();
            String uribuilder = uriComponentsBuilder.build().toString();
            list.add(buildLink("entity", uribuilder.substring(0, uribuilder.lastIndexOf("/"))));
        }
    }

    @Override
    protected Class<RestResourceController> getControllerClass() {
        return RestResourceController.class;
    }

    @Override
    protected Class<OrcidQueueResource> getResourceClass() {
        return OrcidQueueResource.class;
    }
}
