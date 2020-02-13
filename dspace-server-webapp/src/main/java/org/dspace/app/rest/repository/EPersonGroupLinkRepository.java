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

import org.dspace.app.rest.model.EPersonRest;
import org.dspace.app.rest.model.GroupRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.service.EPersonService;
import org.dspace.eperson.service.GroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * Link repository for "groups" subresource of an individual eperson.
 */
@Component(EPersonRest.CATEGORY + "." + EPersonRest.NAME + "." + EPersonRest.GROUPS)
public class EPersonGroupLinkRepository extends AbstractDSpaceRestRepository
        implements LinkRestRepository {

    @Autowired
    EPersonService epersonService;

    @Autowired
    GroupService groupService;

    @PreAuthorize("hasPermission(#epersonId, 'EPERSON', 'READ')")
    public Page<GroupRest> getGroups(@Nullable HttpServletRequest request,
                                     UUID epersonId,
                                     @Nullable Pageable optionalPageable,
                                     Projection projection) {
        try {
            Context context = obtainContext();
            EPerson eperson = epersonService.find(context, epersonId);
            if (eperson == null) {
                throw new ResourceNotFoundException("No such eperson: " + epersonId);
            }
            Page<Group> groups = utils.getPage(groupService.allMemberGroups(context, eperson), optionalPageable);
            return converter.toRestPage(groups, projection);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
