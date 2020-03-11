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
import org.dspace.app.rest.model.hateoas.HALResource;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.springframework.hateoas.Link;
import org.springframework.stereotype.Component;

/**
 * Catch-all projection that allows embedding of all subresources.
 */
@Component
public class FullProjection extends AbstractProjection {

    public final static String NAME = "full";
    private final int maxEmbed = DSpaceServicesFactory.getInstance().getConfigurationService()
            .getIntProperty("rest.projections.full.max", 2);

    public String getName() {
        return NAME;
    }

    @Override
    public boolean allowEmbedding(HALResource<? extends RestAddressableModel> halResource, LinkRest linkRest,
                                  Link... oldLinks) {
        return halResource.getContent().getEmbedLevel() < maxEmbed;
    }

    @Override
    public boolean allowLinking(HALResource halResource, LinkRest linkRest) {
        return true;
    }
}
