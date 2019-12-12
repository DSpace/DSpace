/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;

import org.dspace.app.rest.model.MockObject;
import org.dspace.app.rest.model.MockObjectRest;
import org.dspace.app.rest.projection.Projection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Abstract link repository for use with tests.
 */
abstract class AbstractMockObjectChildLinkRepository
        extends AbstractDSpaceRestRepository implements LinkRestRepository {

    public Page<MockObjectRest> getMockObjectChildren(@Nullable HttpServletRequest request,
                                                      Long itemId,
                                                      @Nullable Pageable optionalPageable,
                                                      Projection projection) {
        List<MockObject> children = new ArrayList<>();
        if (itemId == 0) {
            children.add(MockObject.create(101));
            children.add(MockObject.create(102));
        }
        Pageable pageable = utils.getPageable(optionalPageable);
        return converter.toRestPage(children, pageable, children.size(), projection);
    }
}
