/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import org.dspace.app.rest.model.MockObject;
import org.dspace.app.rest.model.MockObjectRest;
import org.dspace.app.rest.projection.Projection;
import org.springframework.stereotype.Component;

/**
 * A simple {@link DSpaceConverter} for use with tests.
 */
@Component
public class MockObjectConverter implements DSpaceConverter<MockObject, MockObjectRest> {

    @Override
    public MockObjectRest convert(MockObject modelObject, Projection projection) {
        MockObjectRest restObject = new MockObjectRest();
        restObject.setProjection(projection);
        restObject.setId(modelObject.getStoredId());
        restObject.setValue(modelObject.getStoredValue());
        return restObject;
    }

    @Override
    public Class<MockObject> getModelClass() {
        return MockObject.class;
    }
}
