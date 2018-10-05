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
import org.dspace.app.rest.exception.RepositoryMethodNotImplementedException;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.GroupRest;
import org.dspace.app.rest.model.MetadataEntryRest;
import org.dspace.app.rest.model.hateoas.GroupResource;
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
public class GroupRestRepository extends DSpaceRestRepository<GroupRest, UUID> {
    @Autowired
    GroupService gs;

    @Autowired
    GroupConverter converter;

    @Override
    protected GroupRest createAndReturn(Context context)
            throws AuthorizeException, RepositoryMethodNotImplementedException {
        HttpServletRequest req = getRequestService().getCurrentRequest().getHttpServletRequest();
        ObjectMapper mapper = new ObjectMapper();
        GroupRest groupRest = null;

        try {
            groupRest = mapper.readValue(req.getInputStream(), GroupRest.class);
        } catch (IOException excIO) {
            throw new UnprocessableEntityException("error parsing the body... maybe this is not the right error code");
        }

        Group group = null;
        try {
            group = gs.create(context);
            gs.setName(group, groupRest.getName());
            gs.update(context, group);

            if (groupRest.getMetadata() != null) {
                for (MetadataEntryRest mer: groupRest.getMetadata()) {
                    String[] metadatakey = mer.getKey().split("\\.");
                    gs.addMetadata(context, group, metadatakey[0], metadatakey[1],
                            metadatakey.length == 3 ? metadatakey[2] : null, mer.getLanguage(), mer.getValue());
                }
            }
        } catch (SQLException excSQL) {
            throw new RuntimeException(excSQL.getMessage(), excSQL);
        }

        return converter.convert(group);
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
        return converter.fromModel(group);
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
        Page<GroupRest> page = new PageImpl<Group>(groups, pageable, total).map(converter);
        return page;
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
