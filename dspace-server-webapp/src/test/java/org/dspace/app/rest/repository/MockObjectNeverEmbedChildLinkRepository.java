/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import org.dspace.app.rest.model.MockObjectRest;
import org.springframework.stereotype.Component;

/**
 * Link repository used by {@link MockObjectRest} to test that never-embedded subresources work correctly.
 */
@Component(MockObjectRest.CATEGORY + "." + MockObjectRest.NAME + "." + MockObjectRest.N_CHILDREN)
public class MockObjectNeverEmbedChildLinkRepository extends AbstractMockObjectChildLinkRepository {
    @Override
    public boolean isEmbeddableRelation(Object data, String name) {
        return false;
    }
}
