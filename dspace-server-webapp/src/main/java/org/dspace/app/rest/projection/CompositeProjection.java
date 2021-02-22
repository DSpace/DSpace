/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.projection;

import java.util.List;

import org.dspace.app.rest.model.LinkRest;
import org.dspace.app.rest.model.RestAddressableModel;
import org.dspace.app.rest.model.RestModel;
import org.dspace.app.rest.model.hateoas.HALResource;
import org.springframework.hateoas.Link;

/**
 * A projection that combines the behavior of multiple projections.
 *
 * Model, rest, and resource transformations will be performed in the order of the projections given in
 * the constructor. Embedding will be allowed if any of the given projections allow them. Linking will
 * be allowed if all of the given projections allow them.
 */
public class CompositeProjection extends AbstractProjection {

    public final static String NAME = "composite";

    private final List<Projection> projections;

    public CompositeProjection(List<Projection> projections) {
        this.projections = projections;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public <T> T transformModel(T modelObject) {
        for (Projection projection : projections) {
            modelObject = projection.transformModel(modelObject);
        }
        return modelObject;
    }

    @Override
    public <T extends RestModel> T transformRest(T restObject) {
        for (Projection projection : projections) {
            restObject = projection.transformRest(restObject);
        }
        return restObject;
    }

    @Override
    public <T extends HALResource> T transformResource(T halResource) {
        for (Projection projection : projections) {
            halResource = projection.transformResource(halResource);
        }
        return halResource;
    }

    @Override
    public boolean allowEmbedding(HALResource<? extends RestAddressableModel> halResource, LinkRest linkRest,
                                  Link... oldLinks) {
        for (Projection projection : projections) {
            if (projection.allowEmbedding(halResource, linkRest, oldLinks)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean allowLinking(HALResource halResource, LinkRest linkRest) {
        for (Projection projection : projections) {
            if (!projection.allowLinking(halResource, linkRest)) {
                return false;
            }
        }
        return true;
    }
}
