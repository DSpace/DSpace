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
import org.dspace.app.rest.model.RestAddressableModel;
import org.dspace.app.rest.model.hateoas.HALResource;
import org.springframework.hateoas.Link;

/**
 * Projection that allows a given set of rels to be embedded.
 * A Rel refers to a Link Relation, this is an Embedded Object of the HalResource and the HalResource contains
 * a link to this
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
    public boolean allowEmbedding(HALResource<? extends RestAddressableModel> halResource, LinkRest linkRest,
                                  Link... oldLinks) {
        // If level 0, and the name is present, the link can be embedded (e.g. the logo on a collection page)
        if (halResource.getContent().getEmbedLevel() == 0 && embedRels.contains(linkRest.name())) {
            return true;
        }

        StringBuilder fullName = new StringBuilder();
        for (Link oldLink : oldLinks) {
            fullName.append(oldLink.getRel().value()).append("/");
        }
        fullName.append(linkRest.name());
        // If the full name matches, the link can be embedded (e.g. mappedItems/owningCollection on a collection page)
        if (embedRels.contains(fullName.toString())) {
            return true;
        }

        fullName.append("/");
        // If the full name starts with the allowed embed, but the embed goes deeper, the link can be embedded
        // (e.g. making sure mappedItems/owningCollection also embeds mappedItems on a collection page)
        for (String embedRel : embedRels) {
            if (embedRel.startsWith(fullName.toString())) {
                return true;
            }
        }
        return false;
    }
}
