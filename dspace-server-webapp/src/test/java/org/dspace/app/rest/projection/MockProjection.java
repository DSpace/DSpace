/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.projection;

import javax.annotation.Nullable;

import org.dspace.app.rest.model.LinkRest;
import org.dspace.app.rest.model.MockObject;
import org.dspace.app.rest.model.MockObjectRest;
import org.dspace.app.rest.model.RestModel;
import org.dspace.app.rest.model.hateoas.HALResource;
import org.springframework.hateoas.Link;

/**
 * A projection for use in tests.
 */
public class MockProjection implements Projection {

    public static final String NAME = "mock";

    private final Link linkToAdd;

    private final Object resourceToEmbed;

    public MockProjection(@Nullable Link linkToAdd, @Nullable Object resourceToEmbed) {
        this.linkToAdd = linkToAdd;
        this.resourceToEmbed = resourceToEmbed;
    }

    public String getName() {
        return NAME;
    }

    /**
     * When given a {@link MockObject}, adds one to the id and appends "?" to the value, if not null.
     * Otherwise, returns the original object.
     */
    @Override
    public <T> T transformModel(T modelObject) {
        if (modelObject instanceof MockObject) {
            MockObject mockObject = (MockObject) modelObject;
            if (mockObject.getStoredId() != null) {
                mockObject.setStoredId(mockObject.getStoredId() + 1);
            }
            if (mockObject.getStoredValue() != null) {
                mockObject.setStoredValue(mockObject.getStoredValue() + "?");
            }
        }
        return modelObject;
    }

    /**
     * When given a {@link MockObjectRest}, multiplies the id by 3 and appends "!" to the value, if not null.
     * Otherwise, returns the original object.
     */
    @Override
    public <T extends RestModel> T transformRest(T restObject) {
        if (restObject instanceof MockObjectRest) {
            MockObjectRest mockObjectRest = (MockObjectRest) restObject;
            if (mockObjectRest.getId() != null) {
                mockObjectRest.setId(mockObjectRest.getId() * 3);
            }
            if (mockObjectRest.getValue() != null) {
                mockObjectRest.setValue(mockObjectRest.getValue() + "!");
            }
        }
        return restObject;
    }

    /**
     * Adds link: {@code linkToAdd} if given as non-null in the constructor and adds "resource" embed:
     * {@code resourceToEmbed} if given as non-null in the constructor.
     */
    @Override
    public <T extends HALResource> T transformResource(T halResource) {
        if (linkToAdd != null) {
            halResource.add(linkToAdd);
        }
        if (resourceToEmbed != null) {
            halResource.embedResource("resource", resourceToEmbed);
        }
        return halResource;
    }

    public boolean allowEmbedding(HALResource halResource, LinkRest linkRest) {
        return true;
    }

    public boolean allowLinking(HALResource halResource, LinkRest linkRest) {
        return false;
    }
}
