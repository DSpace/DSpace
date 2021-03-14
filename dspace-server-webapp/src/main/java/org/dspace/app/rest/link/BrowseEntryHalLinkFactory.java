/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.link;

import java.util.LinkedList;

import org.atteo.evo.inflector.English;
import org.dspace.app.rest.RestResourceController;
import org.dspace.app.rest.model.BrowseEntryRest;
import org.dspace.app.rest.model.BrowseIndexRest;
import org.dspace.app.rest.model.hateoas.BrowseEntryResource;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.Link;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * This class' purpose is to provide a factory to add links to the BrowseEntryResource. The addLinks factory will be
 * called
 * from the HalLinkService class addLinks method.
 */
@Component
public class BrowseEntryHalLinkFactory extends HalLinkFactory<BrowseEntryResource, RestResourceController> {

    protected void addLinks(final BrowseEntryResource halResource, final Pageable pageable, final LinkedList<Link> list)
        throws Exception {
        BrowseEntryRest data = halResource.getContent();

        if (data != null) {
            BrowseIndexRest bix = data.getBrowseIndex();

            UriComponentsBuilder baseLink = uriBuilder(
                getMethodOn(bix.getCategory(), bix.getType()).findRel(null, null, bix.getCategory(),
                                                                      English.plural(bix.getType()), bix.getId(),
                                                                      BrowseIndexRest.ITEMS, null, null));

            addFilterParams(baseLink, data);

            list.add(buildLink(BrowseIndexRest.ITEMS,
                               baseLink.build().toUriString()));
        }
    }

    protected Class<RestResourceController> getControllerClass() {
        return RestResourceController.class;
    }

    protected Class<BrowseEntryResource> getResourceClass() {
        return BrowseEntryResource.class;
    }

    // TODO use the reflection to discover the link repository and additional information on the link annotation to
    // build the parameters?
    private UriComponentsBuilder addFilterParams(UriComponentsBuilder uriComponentsBuilder,
                                                 final BrowseEntryRest data) {
        UriComponentsBuilder result;
        if (data.getAuthority() != null) {
            result = uriComponentsBuilder.queryParam("filterAuthority", data.getAuthority());
        } else {
            result = uriComponentsBuilder.queryParam("filterValue", data.getValue());
        }
        return result;
    }

}
