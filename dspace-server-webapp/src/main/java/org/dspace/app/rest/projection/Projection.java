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

public interface Projection {

    /**
     * The default projection.
     */
    Projection DEFAULT = new DefaultProjection();

    /**
     * Gets the projection name.
     *
     * @return the name, which is a unique alphanumeric string.
     */
    String getName();

    <T> T transformModel(T modelObject);

    <T extends RestModel> T transformRest(T restObject);

    <T extends HALResource> T transformResource(T halResource);

    /**
     * Tells whether this projection permits the embedding of a particular optionally-embeddable related resource.
     *
     * Optionally-embeddable related resources, discovered through {@link LinkRest} annotations, are normally
     * automatically embedded. This method gives the projection an opportunity to opt out of some or all such embeds,
     * by returning {@code false}.
     *
     * @param halResource the resource from which the embed may or may not be made.
     * @param linkRest the LinkRest annotation through which the related resource was discovered on the rest object.
     * @return true if allowed, false otherwise.
     */
    boolean allowOptionalEmbed(HALResource halResource, LinkRest linkRest);

    /**
     * Tells whether this projection permits the linking of a particular optionally-linkable related resource.
     *
     * Optionally-linkable related resources, discovered through {@link LinkRest} annotations, are normally
     * automatically linked. This method gives the projection an opportunity to opt out of some or all such links,
     * by returning {@code false}.
     *
     * @param halResource the resource from which the embed may or may not be made.
     * @param linkRest the LinkRest annotation through which the related resource was discovered on the rest object.
     * @return true if allowed, false otherwise.
     */
    boolean allowOptionalLink(HALResource halResource, LinkRest linkRest);
}
