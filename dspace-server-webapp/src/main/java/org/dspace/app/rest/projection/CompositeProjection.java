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
import org.dspace.content.security.service.MetadataSecurityService;
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

    /**
     * Determines whether metadata security evaluation should be bypassed for this composite projection.
     * <p>
     * This method returns {@code true} if <strong>any</strong> of the combined projections requests
     * that metadata security checks be prevented. This follows an "opt-in" pattern where a single
     * projection can disable security evaluation for the entire composite.
     * <p>
     * <strong>When this returns {@code true}:</strong>
     * <ul>
     *   <li>No metadata security level evaluation is performed during DSpaceObject conversion</li>
     *   <li>Only metadata fields configured as public via the {@code metadata.publicField} property
     *       are included in the REST response</li>
     *   <li>Security levels (0, 1, 2) assigned to individual metadata values are ignored</li>
     *   <li>No permission checks are performed for metadata visibility</li>
     * </ul>
     * <p>
     * <strong>Primary Use Case:</strong> The {@link PreventMetadataSecurityProjection} is the main
     * projection that returns {@code true}. It is used in shared workspace discovery scenarios where:
     * <ul>
     *   <li>Users browse workspace items they don't own (via {@code otherWorkspace} configuration)</li>
     *   <li>Only basic public metadata should be visible to avoid exposing sensitive information</li>
     *   <li>REST clients explicitly request the projection via {@code ?projection=preventMetadataSecurity}</li>
     * </ul>
     * <p>
     * <strong>Composite Behavior:</strong> Uses {@code anyMatch()} aggregation logic, meaning if this
     * composite contains 3 projections and only 1 of them returns {@code true}, the entire composite
     * will prevent metadata security checks.
     *
     * @return {@code true} if any of the combined projections requests to prevent metadata security;
     *         {@code false} if all projections allow normal security evaluation
     * @see PreventMetadataSecurityProjection
     * @see MetadataSecurityService
     */
    @Override
    public boolean preventMetadataLevelSecurity() {
        return projections.stream()
                          .anyMatch(prj -> prj.preventMetadataLevelSecurity());
    }

    @Override
    public boolean isAllLanguages() {
        return projections.stream()
                          .anyMatch(Projection::isAllLanguages);
    }
}
