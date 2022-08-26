/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.projection;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.dspace.app.rest.model.LinkRest;
import org.dspace.app.rest.model.RestAddressableModel;
import org.dspace.app.rest.model.hateoas.HALResource;
import org.springframework.data.domain.PageRequest;
import org.springframework.hateoas.Link;

/**
 * Projection that allows a given set of rels to be embedded.
 * A Rel refers to a Link Relation, this is an Embedded Object of the HalResource and the HalResource contains
 * a link to this
 */
public class EmbedRelsProjection extends AbstractProjection {

    public final static String NAME = "embedrels";

    private final Set<String> embedRels;

    /**
     * This variable contains a map with the key being the rel as a String and the value being the size of the embed
     * as an Integer
     */
    private Map<String, Integer> embedSizes;

    /**
     * The EmbedRelsProjection will take a set of embed relations and embed sizes as a parameter that will be added to
     * the projection.
     * The set of embedRels contains strings representing the embedded relation.
     * The set of embedSizes contains a string containing the embedded relation followed by a "=" and the size of the
     * embedded relation.
     * Example: embed=collections&embed.size=collections=5 - These parameters will ensure that the embedded collections
     * size will be limited to 5
     * @param embedRels     The embedded relations
     * @param embedSizes    The sizes of the embedded relations defined as {relation}={size}
     */
    public EmbedRelsProjection(Set<String> embedRels, Set<String> embedSizes) {
        this.embedRels = embedRels;
        this.embedSizes = embedSizes.stream()
                                    .filter(embedSize -> StringUtils.contains(embedSize, "="))
                                    .map(embedSize -> embedSize.split("="))
                                    .collect(Collectors.toMap(
                                        split -> split[0],
                                        split -> NumberUtils.toInt(split[1], 0)
                                    ));
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

    @Override
    public PageRequest getPagingOptions(String rel, HALResource<? extends RestAddressableModel> resource,
                                        Link... oldLinks) {
        Integer size = getPaginationSize(rel, resource, oldLinks);
        if (size != null && size > 0) {
            return PageRequest.of(0, size);
        }
        return null;
    }

    private Integer getPaginationSize(String rel, HALResource<? extends RestAddressableModel> resource,
                                      Link... oldLinks) {
        if (resource.getContent().getEmbedLevel() == 0 && embedSizes.containsKey(rel)) {
            return embedSizes.get(rel);
        }
        StringBuilder fullName = new StringBuilder();
        for (Link oldLink : oldLinks) {
            fullName.append(oldLink.getRel().value()).append("/");
        }
        fullName.append(rel);

        // If the full name matches, the link can be embedded (e.g. mappedItems/owningCollection on a collection page)
        if (embedSizes.containsKey(fullName.toString())) {
            return embedSizes.get(fullName.toString());
        }
        return null;
    }

    public Map<String, Integer> getEmbedSizes() {
        return embedSizes;
    }

    public void setEmbedSizes(final Map<String, Integer> embedSizes) {
        this.embedSizes = embedSizes;
    }
}
