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

@Component(MockObjectRest.CATEGORY + "." + MockObjectRest.NAME)
public class MockObjectRestRepository extends DSpaceRestRepository<MockObjectRest, Long> {

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
