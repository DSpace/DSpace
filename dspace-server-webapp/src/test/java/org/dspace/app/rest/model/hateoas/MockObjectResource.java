/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model.hateoas;

import org.dspace.app.rest.model.MockObjectRest;
import org.dspace.app.rest.utils.Utils;

public class MockObjectResource extends DSpaceResource<MockObjectRest> {

    public MockObjectResource(MockObjectRest data, Utils utils) {
        super(data, utils);
    }
}
