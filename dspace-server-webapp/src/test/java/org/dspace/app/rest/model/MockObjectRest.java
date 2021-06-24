/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import org.dspace.app.rest.RestResourceController;
import org.dspace.app.rest.projection.Projection;

/**
 * A simple rest object for use with tests.
 */
@LinksRest(links = {
        @LinkRest(
                name = MockObjectRest.O_CHILDREN,
                method = "getMockObjectChildren"
        ),
        @LinkRest(
                name = MockObjectRest.N_CHILDREN,
                method = "getMockObjectChildren"
        )
})
public class MockObjectRest extends BaseObjectRest<Long> {

    public static final String CATEGORY = "test";

    public static final String NAME = "testobject";

    public static final String O_CHILDREN = "optionallyEmbeddedChildren";

    public static final String N_CHILDREN = "neverEmbeddedChildren";

    private String value;

    private MockObjectRest restPropNotNull;

    private MockObjectRest restPropNull;

    private MockObjectRest restPropRenamed;

    private MockObjectRest restPropUnannotated;

    public static MockObjectRest create(long id) {
        MockObjectRest mockObjectRest = new MockObjectRest();
        mockObjectRest.setProjection(Projection.DEFAULT);
        mockObjectRest.setId(id);
        mockObjectRest.setValue("value" + id);
        return mockObjectRest;
    }

    @Override
    public String getCategory() {
        return CATEGORY;
    }

    @Override
    public Class getController() {
        return RestResourceController.class;
    }

    @Override
    public String getType() {
        return NAME;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @LinkRest
    public MockObjectRest getRestPropNotNull() {
        return restPropNotNull;
    }

    public void setRestPropNotNull(MockObjectRest restPropNotNull) {
        this.restPropNotNull = restPropNotNull;
    }

    @LinkRest
    public MockObjectRest getRestPropNull() {
        return restPropNull;
    }

    public void setRestPropNull(MockObjectRest restPropNull) {
        this.restPropNull = restPropNull;
    }

    @LinkRest(name = "restPropRenamedWithSuffix")
    public MockObjectRest getRestPropRenamed() {
        return restPropRenamed;
    }

    public void setRestPropRenamed(MockObjectRest restPropRenamed) {
        this.restPropRenamed = restPropRenamed;
    }

    public MockObjectRest getRestPropUnannotated() {
        return restPropUnannotated;
    }

    public void setRestPropUnannotated(MockObjectRest restPropUnannotated) {
        this.restPropUnannotated = restPropUnannotated;
    }
}
