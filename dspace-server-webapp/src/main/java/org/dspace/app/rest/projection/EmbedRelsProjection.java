/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.projection;

import java.util.Set;

import org.dspace.app.rest.model.LinkRest;
import org.dspace.app.rest.model.hateoas.HALResource;

/**
 * Projection that allows a given set of rels to be embedded.
 */
public class EmbedRelsProjection extends AbstractProjection {

    public final static String NAME = "embedrels";

    private final Set<String> embedRels;

    public EmbedRelsProjection(Set<String> embedRels) {
        this.embedRels = embedRels;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public boolean allowEmbedding(HALResource halResource, LinkRest linkRest) {
        return embedRels.contains(linkRest.name());
    }
}
