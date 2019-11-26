/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.projection;

import javax.persistence.Entity;

import org.dspace.app.rest.model.LinkRest;
import org.dspace.app.rest.model.RestModel;
import org.dspace.app.rest.model.hateoas.HALResource;
import org.dspace.app.rest.repository.DSpaceRestRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RestController;

/**
 * A pluggable, uniquely-named {@link Component} that provides a way to change how a domain object is represented,
 * at one or more points in its lifecycle on the way to its being exposed via the REST API.
 *
 * <h2>The object lifecycle</h2>
 *
 * <p>
 * While fulfilling a typical REST request, a DSpace domain object takes three major forms, in order:
 * </p>
 *
 * <ul>
 *   <li> A model object provided by some service. This is typically a JPA {@link Entity}.</li>
 *   <li> A {@link RestModel} object provided by a {@link DSpaceRestRepository}.</li>
 *   <li> A {@link HALResource} object provided by the {@link RestController} to Spring, which then
 *        serializes it as JSON for the client to consume.</li>
 * </ul>
 *
 * <h2>What a projection can modify, and when</h2>
 *
 * A {@code Projection} implementation is capable of adding to or omitting information from the object
 * in any of these forms, at the following points in time:
 *
 * <ul>
 *   <li> Before it is converted to a {@link RestModel}, the projection may modify it
 *        via {@link #transformModel(Object)}.</li>
 *   <li> After it is converted to a {@link RestModel}, the projection may modify it
 *        via {@link #transformRest(RestModel)}.</li>
 *   <li> During conversion to a {@link HALResource}, the projection may opt out of certain annotation-discovered
 *        HAL embeds and links via {@link #allowOptionalEmbed(HALResource, LinkRest)}
 *        and {@link #allowOptionalLink(HALResource, LinkRest)}.</li>
 *   <li> After conversion to a {@link HALResource}, the projection may modify it
 *        via {@link #transformResource(HALResource)}.</li>
 * </ul>
 *
 * <h2>How a projection is chosen</h2>
 *
 * When a REST request is made, the use of a projection may be explicit, as when it is provided as an argument
 * to the request, e.g. {@code /items/{uuid}?projection={projectionName}}. It may also be implicit, as when the
 * {@link ListProjection} is used automatically, in order to provide an abbreviated representation when serving
 * a collection of resources.
 */
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

    /**
     * Transforms the original model object (e.g. JPA entity) before conversion to a {@link RestModel}.
     *
     * This is a good place to omit data for certain properties that should not be included in the object's
     * representation as a {@link HALResource}. Omitting these properties early helps to prevent unnecessary
     * database calls for lazy-loaded properties that are unwanted for the projection.
     *
     * @param modelObject the original model object, which may be of any type.
     * @param <T> the return type, which must be the same type as the given model object.
     * @return the transformed model object, or the original, if the projection does not modify it.
     */
    <T> T transformModel(T modelObject);

    /**
     * Transforms the rest object after it was converted from a model object.
     *
     * This may add data to, or omit data from the rest representation of the object.
     *
     * @param restObject the rest object.
     * @param <T> the return type, which must be of the same type as the given rest object.
     * @return the transformed rest object, or the original, if the projection does not modify it.
     */
    <T extends RestModel> T transformRest(T restObject);

    /**
     * Transforms the resource object after it has been constructed and any constructor or annotation-based
     * links and embeds have been added.
     *
     * This may add data to, or omit data from the HAL resource representation of the object.
     *
     * @param halResource the resource object.
     * @param <T> the return type, which must be of the same type as the given resource object.
     * @return the transformed resource object, or the original, if the projection does not modify it.
     */
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
