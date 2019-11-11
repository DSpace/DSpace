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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.dspace.app.rest.model.MockObject;
import org.dspace.app.rest.model.MockObjectRest;
import org.dspace.app.rest.model.RestAddressableModel;
import org.dspace.app.rest.model.RestModel;
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

    private static final Long ORIG_ID = 0L;

    private static final String ORIG_VALUE = "value0";

    private final MockObject mockObject = mockObject(ORIG_ID);

    @Test
    public void toRestNoConverterFound() {
        try {
            converter.toRest(mockObjectWithNoConverter, Projection.DEFAULT);
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), startsWith("No converter found"));
        }
    }

    @Test(expected = ClassCastException.class)
    public void toRestWrongReturnType() {
        @SuppressWarnings("unused")
        String restObject = converter.toRest(mockObject(0), Projection.DEFAULT);
    }

    @Test
    public void toRestWithDefaultProjection() {
        MockObjectRest restObject = converter.toRest(mockObject(0), Projection.DEFAULT);
        assertEquals(ORIG_ID, restObject.getId());
        assertEquals(ORIG_VALUE, restObject.getValue());
    }

    @Test
    public void toRestWithMockProjection() {
        MockProjection mockProjection = new MockProjection(mockLink, mockEmbeddedResource);
        MockObjectRest restObject = converter.toRest(mockObject, mockProjection);
        assertThat(restObject.getId(), equalTo((ORIG_ID + 1) * 3));
        assertThat(restObject.getValue(), equalTo(ORIG_VALUE + "?!"));
    }

    @Test
    public void toResourceNoConstructorFound() {
        try {
            converter.toResource(mockObjectRestWithNoResource);
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), startsWith("No constructor found"));
        }
    }

    @Test(expected = ClassCastException.class)
    public void toResourceWrongReturnType() {
        @SuppressWarnings("unused")
        MockHalResource mockHalResource = converter.toResource(mockObjectRest(0));
    }

    @Test
    public void toResourceWithDefaultProjection() {
        MockObjectRest r0 = mockObjectRest(0);
        MockObjectRest r1 = mockObjectRest(1);
        r0.setRestProp1(r1);
        MockObjectResource resource = converter.toResource(r0);
        assertEquals(ORIG_ID, resource.getContent().getId());
        assertEquals(ORIG_VALUE, resource.getContent().getValue());
        assertEquals(5, resource.getLinks().size());
        assertEquals("restProp1", resource.getLinks().get(0).getRel());
        assertEquals("restProp2", resource.getLinks().get(1).getRel());
        assertEquals("restProp3", resource.getLinks().get(2).getRel());
        assertEquals("restPropFour", resource.getLinks().get(3).getRel());
        assertEquals("self", resource.getLinks().get(4).getRel());
        assertEquals(4, resource.getEmbeddedResources().size());
        assertTrue(resource.getEmbeddedResources().containsKey("restProp1"));
        assertTrue(resource.getEmbeddedResources().containsKey("restProp2"));
        assertTrue(resource.getEmbeddedResources().containsKey("restProp3"));
        assertTrue(resource.getEmbeddedResources().containsKey("restPropFour"));
        assertEquals(r1, ((Resource) resource.getEmbeddedResources().get("restProp1")).getContent());
        assertNull(resource.getEmbeddedResources().get("restProp2"));
        assertNull(resource.getEmbeddedResources().get("restProp3"));
        assertNull(resource.getEmbeddedResources().get("restPropFour"));
    }

    private static MockObject mockObject(long id) {
        MockObject mockObject = new MockObject();
        mockObject.setStoredId(id);
        mockObject.setStoredValue("value" + id);
        return mockObject;
    }

    private static MockObjectRest mockObjectRest(long id) {
        MockObjectRest mockObjectRest = new MockObjectRest();
        mockObjectRest.setProjection(Projection.DEFAULT);
        mockObjectRest.setId(id);
        mockObjectRest.setValue("value" + id);
        return mockObjectRest;
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
