/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.dspace.app.rest.converter.GroupConverter;
import org.dspace.app.rest.converter.MetadataConverter;
import org.dspace.app.rest.exception.RepositoryMethodNotImplementedException;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.GroupRest;
import org.dspace.app.rest.model.hateoas.GroupResource;
import org.dspace.app.rest.model.patch.Patch;
import org.dspace.app.rest.repository.patch.DSpaceObjectPatch;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.eperson.Group;
import org.dspace.eperson.service.GroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * This is the repository responsible to manage Group Rest object
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */

@Component(GroupRest.CATEGORY + "." + GroupRest.NAME)
public class GroupRestRepository extends DSpaceObjectRestRepository<Group, GroupRest> {
    @Autowired
    GroupService gs;

    @Autowired
    GroupRestRepository(GroupService dsoService,
                        GroupConverter dsoConverter) {
        super(dsoService, dsoConverter, new DSpaceObjectPatch<GroupRest>() {});
        this.gs = dsoService;
    }

    @Autowired
    MetadataConverter metadataConverter;

    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    protected GroupRest createAndReturn(Context context)
            throws AuthorizeException, RepositoryMethodNotImplementedException {
        HttpServletRequest req = getRequestService().getCurrentRequest().getHttpServletRequest();
        ObjectMapper mapper = new ObjectMapper();
        GroupRest groupRest = null;

        try {
            groupRest = mapper.readValue(req.getInputStream(), GroupRest.class);
        } catch (IOException excIO) {
            throw new UnprocessableEntityException("error parsing the body ..." + excIO.getMessage());
        }

        Group group = null;
        try {
            group = gs.create(context);
            gs.setName(group, groupRest.getName());
            gs.update(context, group);
            metadataConverter.setMetadata(context, group, groupRest.getMetadata());
        } catch (SQLException excSQL) {
            throw new RuntimeException(excSQL.getMessage(), excSQL);
        }

        return dsoConverter.convert(group);
    }

    @Override
    @PreAuthorize("hasPermission(#id, 'GROUP', 'READ')")
    public GroupRest findOne(Context context, UUID id) {
        Group group = null;
        try {
            group = gs.find(context, id);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        if (group == null) {
            return null;
        }
        return dsoConverter.fromModel(group);
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @Override
    public Page<GroupRest> findAll(Context context, Pageable pageable) {
        List<Group> groups = null;
        int total = 0;
        try {
            total = gs.countTotal(context);
            groups = gs.findAll(context, null, pageable.getPageSize(), pageable.getOffset());
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        Page<GroupRest> page = new PageImpl<Group>(groups, pageable, total).map(dsoConverter);
        return page;
    }

    @Override
    @PreAuthorize("hasPermission(#id, 'GROUP', 'WRITE')")
    protected void patch(Context context, HttpServletRequest request, String apiCategory, String model, UUID id,
                         Patch patch) throws AuthorizeException, SQLException {
        patchDSpaceObject(apiCategory, model, id, patch);
    }

    @Override
    public Class<GroupRest> getDomainClass() {
        return GroupRest.class;
    }

    @Override
    public GroupResource wrapResource(GroupRest eperson, String... rels) {
        return new GroupResource(eperson, utils, rels);
    }

}
