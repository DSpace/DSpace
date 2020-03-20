/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;
import org.dspace.app.rest.model.MockObject;
import org.dspace.app.rest.model.MockObjectRest;
import org.dspace.app.rest.model.RestAddressableModel;
import org.dspace.app.rest.model.RestModel;
import org.dspace.app.rest.model.hateoas.EmbeddedPage;
import org.dspace.app.rest.model.hateoas.HALResource;
import org.dspace.app.rest.model.hateoas.MockObjectResource;
import org.dspace.app.rest.projection.MockProjection;
import org.dspace.app.rest.projection.Projection;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;

/**
 * Tests functionality of {@link ConverterService}.
 */
public class ConverterServiceIT extends AbstractControllerIntegrationTest {

    @Autowired
    private ConverterService converter;

    @Mock
    private Object mockObjectWithNoConverter;

    @Mock
    private RestModel mockObjectRestWithNoResource;

    @Mock
    private Link mockLink;

    @Mock
    private Object mockEmbeddedResource;

    /**
     * When calling {@code toRest} with an object for which an appropriate {@link DSpaceConverter} can't be found,
     * it should throw an {@link IllegalArgumentException}.
     */
    @Test
    public void toRestNoConverterFound() {
        try {
            converter.toRest(mockObjectWithNoConverter, Projection.DEFAULT);
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), startsWith("No converter found"));
        }
    }

    /**
     * When calling {@code toRest} and the inferred return type is incompatible with the converter's output,
     * it should throw a {@link ClassCastException}.
     */
    @Test(expected = ClassCastException.class)
    public void toRestWrongReturnType() {
        @SuppressWarnings("unused")
        String restObject = converter.toRest(MockObject.create(0), Projection.DEFAULT);
    }

    /**
     * When calling {@code toRest} with the default projection, the converter should run and no changes should be made.
     */
    @Test
    public void toRestWithDefaultProjection() {
        long id = 0;
        MockObjectRest restObject = converter.toRest(MockObject.create(id), Projection.DEFAULT);
        assertThat(restObject.getId(), equalTo(id));
        assertThat(restObject.getValue(), equalTo("value" + id));
    }

    /**
     * When calling {@code toRest} with a custom projection, {@link Projection#transformModel(Object)} should
     * be called before conversion, then {@link Projection#transformRest(RestModel)} should be called before
     * returning.
     */
    @Test
    public void toRestWithMockProjection() {
        long id = 0;
        MockProjection mockProjection = new MockProjection(mockLink, mockEmbeddedResource);
        MockObjectRest restObject = converter.toRest(MockObject.create(id), mockProjection);
        assertThat(restObject.getId(), equalTo((id + 1) * 3));
        assertThat(restObject.getValue(), equalTo("value" + id + "?!"));
    }

    /**
     * When calling {@code toResource}, if an appropriate constructor could not be found,
     * it should throw an {@link IllegalArgumentException}.
     */
    @Test
    public void toResourceNoConstructorFound() {
        try {
            converter.toResource(mockObjectRestWithNoResource);
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), startsWith("No constructor found"));
        }
    }

    /**
     * When calling {@code toResource} and the inferred return type is incompatible with the resource constructor,
     * it should throw a {@link ClassCastException}.
     */
    @Test(expected = ClassCastException.class)
    public void toResourceWrongReturnType() {
        @SuppressWarnings("unused")
        MockHalResource mockHalResource = converter.toResource(MockObjectRest.create(0));
    }

    /**
     * When calling {@code toResource} with the default projection, the result should have the unannotated
     * linkable property as the only embed, all expected links, and no changes introduced by the projection.
     */
    @Test
    public void toResourceWithDefaultProjection() throws Exception {
        MockObjectRest r0 = MockObjectRest.create(0);
        MockObjectRest restPropNotNullValue = MockObjectRest.create(1);
        MockObjectRest restPropRenamedValue = MockObjectRest.create(2);
        MockObjectRest restPropUnannotatedValue = MockObjectRest.create(3);
        r0.setRestPropNotNull(restPropNotNullValue);
        r0.setRestPropRenamed(restPropRenamedValue);
        r0.setRestPropUnannotated(restPropUnannotatedValue);
        String r0json = new ObjectMapper().writeValueAsString(r0);

        MockObjectResource resource = converter.toResource(r0);

        // The default projection should not modify the wrapped object
        assertThat(new ObjectMapper().writeValueAsString(r0), equalTo(r0json));

        assertHasEmbeds(resource, new String[] {
                "restPropUnannotated" // embedded; unannotated properties can't be omitted by projections
        }, new Class[] {
                Resource.class
        });

        assertHasLinks(resource, new String[] {
                "self",
                "restPropNotNull",
                "restPropNull",
                "restPropRenamedWithSuffix",
                "restPropUnannotated",
                "optionallyEmbeddedChildren",
                "neverEmbeddedChildren"
        });
    }

    /**
     * When calling {@code toResource} with a custom projection, the result should have all
     * the expected links and embeds, including/excluding any changes introduced by the projection.
     */
    @Test
    public void toResourceWithMockProjection() throws Exception {
        MockObjectRest r0 = MockObjectRest.create(0);
        MockObjectRest restPropNotNullValue = MockObjectRest.create(1);
        MockObjectRest restPropRenamedValue = MockObjectRest.create(2);
        MockObjectRest restPropUnannotatedValue = MockObjectRest.create(3);
        r0.setRestPropNotNull(restPropNotNullValue);
        r0.setRestPropRenamed(restPropRenamedValue);
        r0.setRestPropUnannotated(restPropUnannotatedValue);
        String r0json = new ObjectMapper().writeValueAsString(r0);

        when(mockLink.getRel()).thenReturn("mockLink");
        r0.setProjection(new MockProjection(mockLink, mockEmbeddedResource));

        MockObjectResource resource = converter.toResource(r0);

        // The mock projection should not modify the wrapped object
        assertThat(new ObjectMapper().writeValueAsString(r0), equalTo(r0json));

        assertHasEmbeds(resource, new String[] {
                "restPropNotNull",
                "restPropNull",
                "restPropRenamedWithSuffix",
                "restPropUnannotated",
                "optionallyEmbeddedChildren",
                "resource" // added by MockProjection
        }, new Class[] {
                Resource.class,
                null,
                Resource.class,
                Resource.class,
                EmbeddedPage.class,
                Object.class
        });

        assertEmbeddedPageSize(resource, "optionallyEmbeddedChildren", 2);

        assertHasLinks(resource, new String[] {
                "self", // always linked
                "restPropUnannotated", // linked; unannotated properties can't be omitted by projections
                "optionallyEmbeddedChildren", // linked; embedded due to projection, and embed forces link
                "neverEmbeddedChildren", // linked; embedded due to projection, and embed forces link
                "mockLink" // added by MockProjection
        });
    }

    private void assertHasLinks(Resource resource, String[] rels) {
        Map<String, Link> map = new HashMap<>();
        resource.getLinks().stream().forEach((link) -> map.put(link.getRel(), link));
        assertThat(new TreeSet(map.keySet()), equalTo(new TreeSet(Sets.newHashSet(rels))));
    }

    private void assertHasEmbeds(HALResource resource, String[] rels, Class[] classes) {
        assertThat(new TreeSet(resource.getEmbeddedResources().keySet()), equalTo(new TreeSet(Sets.newHashSet(rels))));
        for (int i = 0; i < rels.length; i++) {
            String rel = rels[i];
            Class expectedClass = classes[i];
            Object value = resource.getEmbeddedResources().get(rel);
            if (expectedClass == null) {
                if (value != null) {
                    fail("expected null value for embed: " + rel);
                }
            } else if (value == null) {
                fail("got null value, but expected a " + expectedClass + " for embed: " + rel);
            } else {
                assertTrue("got a " + value.getClass() + " value, but expected a "
                        + expectedClass + " for embed: " + rel, expectedClass.isAssignableFrom(value.getClass()));
            }
        }
    }

    private void assertEmbeddedPageSize(HALResource resource, String rel, int expectedSize) {
        assertEquals(expectedSize, ((EmbeddedPage) resource.getEmbeddedResources()
                .get(rel)).getPageContent().get(rel).size());
    }

    class MockRestAddressableModel extends RestAddressableModel {
        @Override
        public String getCategory() {
            return null;
        }

        @Override
        public Class getController() {
            return null;
        }

        @Override
        public String getType() {
            return null;
        }
    }

    class MockHalResource extends HALResource<MockRestAddressableModel> {
        public MockHalResource(MockRestAddressableModel content) {
            super(content);
        }
    }
}
