/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import org.dspace.app.rest.RestResourceController;

/**
 * A simple rest object for use with tests.
 */
public class MockObjectRest extends BaseObjectRest<Long> {

    public static final String CATEGORY = "test";

    public static final String NAME = "testobject";

    private String value;

    private MockObjectRest restProp1;

    private MockObjectRest restProp2;

    private MockObjectRest restProp3;

    private MockObjectRest restProp4;

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

    public MockObjectRest getRestProp1() {
        return restProp1;
    }

    public void setRestProp1(MockObjectRest restProp1) {
        this.restProp1 = restProp1;
    }

    @LinkRest(linkClass = MockObjectRest.class)
    public MockObjectRest getRestProp2() {
        return restProp2;
    }

    public void setRestProp2(MockObjectRest restProp2) {
        this.restProp2 = restProp2;
    }

    @LinkRest(linkClass = MockObjectRest.class)
    public MockObjectRest getRestProp3() {
        return restProp3;
    }

    public void setRestProp3(MockObjectRest restProp3) {
        this.restProp3 = restProp3;
    }

    @LinkRest(linkClass = MockObjectRest.class, name = "restPropFour")
    public MockObjectRest getRestProp4() {
        return restProp4;
    }

    public void setRestProp4(MockObjectRest restProp4) {
        this.restProp4 = restProp4;
    }
}
