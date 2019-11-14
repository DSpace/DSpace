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
                linkClass = MockObjectRest.class,
                method = "getMockObjectChildren",
                embedOptional = true,
                linkOptional = true
        ),
        @LinkRest(
                name = MockObjectRest.A_CHILDREN,
                linkClass = MockObjectRest.class,
                method = "getMockObjectChildren",
                linkOptional = true
        ),
        @LinkRest(
                name = MockObjectRest.N_CHILDREN,
                linkClass = MockObjectRest.class,
                method = "getMockObjectChildren"
        )
})
public class MockObjectRest extends BaseObjectRest<Long> {

    public static final String CATEGORY = "test";

    public static final String NAME = "testobject";

    public static final String O_CHILDREN = "oChildren";

    public static final String A_CHILDREN = "aChildren";

    public static final String N_CHILDREN = "nChildren";

    private String value;

    private MockObjectRest restProp1;

    private MockObjectRest restProp2;

    private MockObjectRest restProp3;

    private MockObjectRest restProp4;

    private MockObjectRest restProp5;

    private MockObjectRest restProp6;

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

    @LinkRest(linkClass = MockObjectRest.class, linkOptional = true)
    public MockObjectRest getRestProp1() {
        return restProp1;
    }

    public void setRestProp1(MockObjectRest restProp1) {
        this.restProp1 = restProp1;
    }

    @LinkRest(linkClass = MockObjectRest.class, embedOptional = true, linkOptional = true)
    public MockObjectRest getRestProp2() {
        return restProp2;
    }

    public void setRestProp2(MockObjectRest restProp2) {
        this.restProp2 = restProp2;
    }

    @LinkRest(linkClass = MockObjectRest.class, embedOptional = true, linkOptional = true)
    public MockObjectRest getRestProp3() {
        return restProp3;
    }

    public void setRestProp3(MockObjectRest restProp3) {
        this.restProp3 = restProp3;
    }

    @LinkRest(linkClass = MockObjectRest.class, embedOptional = true)
    public MockObjectRest getRestProp4() {
        return restProp4;
    }

    public void setRestProp4(MockObjectRest restProp4) {
        this.restProp4 = restProp4;
    }

    @LinkRest(linkClass = MockObjectRest.class, name = "restPropFive")
    public MockObjectRest getRestProp5() {
        return restProp5;
    }

    public void setRestProp5(MockObjectRest restProp5) {
        this.restProp5 = restProp5;
    }

    public MockObjectRest getRestProp6() {
        return restProp6;
    }

    public void setRestProp6(MockObjectRest restProp6) {
        this.restProp6 = restProp6;
    }
}
