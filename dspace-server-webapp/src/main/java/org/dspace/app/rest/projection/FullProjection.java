/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.projection;

import org.dspace.app.rest.model.LinkRest;
import org.dspace.app.rest.model.hateoas.HALResource;
import org.springframework.stereotype.Component;

/**
 * Catch-all projection that allows embedding of all subresources.
 */
@Component
public class FullProjection extends AbstractProjection {

    public final static String NAME = "full";

    public String getName() {
        return NAME;
    }

    @Override
    public boolean allowEmbedding(HALResource halResource, LinkRest linkRest) {
        return true;
    }

    @Override
    public boolean allowLinking(HALResource halResource, LinkRest linkRest) {
        return true;
    }
}
