/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.projection;

import org.dspace.app.rest.model.LinkRest;
import org.dspace.app.rest.model.RestAddressableModel;
import org.dspace.app.rest.model.RestModel;
import org.dspace.app.rest.model.hateoas.HALResource;
import org.springframework.data.domain.PageRequest;
import org.springframework.hateoas.Link;

/**
 * Abstract base class for projections.
 *
 * By default, this does no transformation, and allows linking but not embedding of all subresources.
 */
public abstract class AbstractProjection implements Projection {

    @Override
    public <T> T transformModel(T modelObject) {
        return modelObject;
    }

    @Override
    public <T extends RestModel> T transformRest(T restObject) {
        return restObject;
    }

    @Override
    public <T extends HALResource> T transformResource(T halResource) {
        return halResource;
    }

    @Override
    public boolean allowEmbedding(HALResource<? extends RestAddressableModel> halResource, LinkRest linkRest,
                                  Link... oldLinks) {
        return false;
    }

    @Override
    public boolean allowLinking(HALResource halResource, LinkRest linkRest) {
        return true;
    }

    @Override
    public PageRequest getPagingOptions(String rel, HALResource<? extends RestAddressableModel> resource,
                                        Link... oldLinks) {
        return null;
    }

}
