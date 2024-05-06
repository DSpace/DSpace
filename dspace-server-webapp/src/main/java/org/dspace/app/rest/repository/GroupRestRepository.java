/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import static org.apache.commons.lang3.StringUtils.isBlank;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.dspace.app.rest.Parameter;
import org.dspace.app.rest.SearchRestMethod;
import org.dspace.app.rest.converter.MetadataConverter;
import org.dspace.app.rest.exception.GroupNameNotProvidedException;
import org.dspace.app.rest.exception.RepositoryMethodNotImplementedException;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.GroupRest;
import org.dspace.app.rest.model.patch.Patch;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.eperson.Group;
import org.dspace.eperson.service.GroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * This is the repository responsible to manage Group Rest object
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */

@Component(GroupRest.CATEGORY + "." + GroupRest.PLURAL_NAME)
public class GroupRestRepository extends DSpaceObjectRestRepository<Group, GroupRest> {
    @Autowired
    GroupService gs;

    @Autowired
    GroupRestRepository(GroupService dsoService) {
        super(dsoService);
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
        GroupRest groupRest;

        try {
            groupRest = mapper.readValue(req.getInputStream(), GroupRest.class);
        } catch (IOException excIO) {
            throw new UnprocessableEntityException("error parsing the body ..." + excIO.getMessage());
        }

        if (isBlank(groupRest.getName())) {
            throw new GroupNameNotProvidedException();
        }

        Group group;
        try {
            group = gs.create(context);
            gs.setName(group, groupRest.getName());
            gs.update(context, group);
            metadataConverter.setMetadata(context, group, groupRest.getMetadata());
        } catch (SQLException excSQL) {
            throw new RuntimeException(excSQL.getMessage(), excSQL);
        }

        return converter.toRest(group, utils.obtainProjection());
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
        return converter.toRest(group, utils.obtainProjection());
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @Override
    public Page<GroupRest> findAll(Context context, Pageable pageable) {
        try {
            long total = gs.countTotal(context);
            List<Group> groups = gs.findAll(context, null, pageable.getPageSize(),
                                            Math.toIntExact(pageable.getOffset()));
            return converter.toRestPage(groups, pageable, total, utils.obtainProjection());
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    @PreAuthorize("hasPermission(#id, 'GROUP', 'WRITE')")
    protected void patch(Context context, HttpServletRequest request, String apiCategory, String model, UUID id,
                         Patch patch) throws AuthorizeException, SQLException {
        patchDSpaceObject(apiCategory, model, id, patch);
    }


    /**
     * Find the groups matching the query parameter. The search is delegated to the
     * {@link GroupService#search(Context, String, int, int)} method
     *
     * @param query    is the *required* query string
     * @param pageable contains the pagination information
     * @return a Page of GroupRest instances matching the user query
     */
    @PreAuthorize("hasAuthority('ADMIN') || hasAuthority('MANAGE_ACCESS_GROUP')")
    @SearchRestMethod(name = "byMetadata")
    public Page<GroupRest> findByMetadata(@Parameter(value = "query", required = true) String query,
                                          Pageable pageable) {

        try {
            Context context = obtainContext();
            long total = gs.searchResultCount(context, query);
            List<Group> groups = gs.search(context, query, Math.toIntExact(pageable.getOffset()),
                                                           Math.toIntExact(pageable.getPageSize()));
            return converter.toRestPage(groups, pageable, total, utils.obtainProjection());
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    /**
     * Find the Groups matching the query parameter which are NOT a member of the given parent Group.
     * The search is delegated to the
     * {@link GroupService#searchNonMembers(Context, String, Group, int, int)} method
     *
     * @param groupUUID the parent group UUID
     * @param query    is the *required* query string
     * @param pageable contains the pagination information
     * @return a Page of GroupRest instances matching the user query
     */
    @PreAuthorize("hasAuthority('ADMIN') || hasAuthority('MANAGE_ACCESS_GROUP')")
    @SearchRestMethod(name = "isNotMemberOf")
    public Page<GroupRest> findIsNotMemberOf(@Parameter(value = "group", required = true) UUID groupUUID,
                                             @Parameter(value = "query", required = true) String query,
                                             Pageable pageable) {

        try {
            Context context = obtainContext();
            Group excludeParentGroup = gs.find(context, groupUUID);
            long total = gs.searchNonMembersCount(context, query, excludeParentGroup);
            List<Group> groups = gs.searchNonMembers(context, query, excludeParentGroup,
                                                     Math.toIntExact(pageable.getOffset()),
                                                     Math.toIntExact(pageable.getPageSize()));
            return converter.toRestPage(groups, pageable, total, utils.obtainProjection());
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public Class<GroupRest> getDomainClass() {
        return GroupRest.class;
    }

    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    protected void delete(Context context, UUID uuid) throws AuthorizeException {
        Group group = null;
        try {
            group = gs.find(context, uuid);
            if (group == null) {
                throw new ResourceNotFoundException(
                        GroupRest.CATEGORY + "." + GroupRest.NAME
                                + " with id: " + uuid + " not found"
                );
            }
            try {
                if (group.isPermanent()) {
                    throw new UnprocessableEntityException("A permanent group cannot be deleted");
                }
                final DSpaceObject parentObject = gs.getParentObject(context, group);
                if (parentObject != null) {
                    throw new UnprocessableEntityException(
                            "This group cannot be deleted"
                                    + " as it has a parent " + parentObject.getType()
                                    + " with id " + parentObject.getID());
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            gs.delete(context, group);
        } catch (SQLException | IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

}
