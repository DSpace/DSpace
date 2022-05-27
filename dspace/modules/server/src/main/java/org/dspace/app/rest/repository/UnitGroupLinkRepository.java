/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.sql.SQLException;
import java.util.UUID;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;

import org.dspace.app.rest.model.UnitRest;
import org.dspace.app.rest.model.GroupRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Unit;
import org.dspace.eperson.service.EPersonService;
import org.dspace.eperson.service.GroupService;
import org.dspace.eperson.service.UnitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * Link repository for the direct "groups" subresource of an individual unit.
 */
@Component(UnitRest.CATEGORY + "." + UnitRest.NAME + "." + UnitRest.GROUPS)
public class UnitGroupLinkRepository extends AbstractDSpaceRestRepository
        implements LinkRestRepository {

    @Autowired
    UnitService unitService;

    @Autowired
    GroupService groupService;

    @PreAuthorize("hasPermission(#unitId, 'UNIT', 'READ')")
    public Page<GroupRest> getGroups(@Nullable HttpServletRequest request,
                                     UUID unitId,
                                     @Nullable Pageable optionalPageable,
                                     Projection projection) {
        try {
            Context context = obtainContext();
            Unit unit = unitService.find(context, unitId);
            if (unit == null) {
                throw new ResourceNotFoundException("No such unit: " + unitId);
            }
            return converter.toRestPage(unit.getGroups(), optionalPageable, projection);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}

