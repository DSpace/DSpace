/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import org.dspace.app.rest.model.MockObjectRest;
import org.dspace.core.Context;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * This class has been added to allow the MockObjectRest to act as an actual BaseObjectRest since they're
 * expected to have a RestRepository
 */
@Component(MockObjectRest.CATEGORY + "." + MockObjectRest.PLURAL_NAME)
public class MockObjectRestRepository extends DSpaceRestRepository<MockObjectRest, Long> {

    // Added a permitAll preAuthorize annotation to allow the object to be used in tests by every user
    @Override
    @PreAuthorize("permitAll()")
    public MockObjectRest findOne(Context context, Long aLong) {
        return null;
    }

    @Override
    @PreAuthorize("permitAll()")
    public Page<MockObjectRest> findAll(Context context, Pageable pageable) {
        return null;
    }

    @Override
    public Class<MockObjectRest> getDomainClass() {
        return MockObjectRest.class;
    }
}
