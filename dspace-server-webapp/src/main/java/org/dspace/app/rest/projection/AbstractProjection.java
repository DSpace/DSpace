/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.projection;

import org.dspace.app.rest.model.LinkRest;
import org.dspace.app.rest.model.RestModel;
import org.dspace.app.rest.model.hateoas.HALResource;

/**
 * Abstract base class for projections. By default each method has no effect unless overridden by a subclass.
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
    public boolean allowOptionalEmbed(HALResource halResource, LinkRest linkRest) {
        return true;
    }

    @Override
    public boolean allowOptionalLink(HALResource halResource, LinkRest linkRest) {
        return true;
    }
}
