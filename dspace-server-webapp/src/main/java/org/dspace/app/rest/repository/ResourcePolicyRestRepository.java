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

import org.dspace.app.rest.Parameter;
import org.dspace.app.rest.SearchRestMethod;
import org.dspace.app.rest.exception.MissingParameterException;
import org.dspace.app.rest.exception.RepositoryMethodNotImplementedException;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.ResourcePolicyRest;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.app.rest.model.patch.Patch;
import org.dspace.app.rest.projection.Projection;
import org.dspace.app.rest.repository.patch.factories.ResourcePolicyOperationFactory;
import org.dspace.app.rest.utils.DSpaceObjectUtils;
import org.dspace.app.rest.utils.Utils;
import org.dspace.app.util.DCInputsReaderException;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.authorize.service.ResourcePolicyService;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Constants;
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
 * Controller for exposition of default access condition
 *
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science.it)
 */
@Component(ResourcePolicyRest.CATEGORY + "." + ResourcePolicyRest.NAME)
public class ResourcePolicyRestRepository extends DSpaceRestRepository<ResourcePolicyRest, Integer> {

    @Autowired
    ResourcePolicyService resourcePolicyService;

    @Autowired
    ResourcePolicyOperationFactory resourcePolicyOperationPatchFactory;

    @Autowired
    Utils utils;

    @Autowired
    private EPersonService epersonService;

    @Autowired
    private GroupService groupService;

    @Autowired
    DSpaceObjectUtils dspaceObjectUtils;


    @Override
    @PreAuthorize("hasPermission(#id, 'resourcepolicy', 'READ')")
    public ResourcePolicyRest findOne(Context context, Integer id) {
        ResourcePolicy source = null;
        try {
            source = resourcePolicyService.find(context, id);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        if (source == null) {
            return null;
        }
        return converter.toRest(source, utils.obtainProjection());
    }

    @PreAuthorize("hasAuthority('AUTHENTICATED')")
    @Override
    public Page<ResourcePolicyRest> findAll(Context context, Pageable pageable) {
        throw new RepositoryMethodNotImplementedException(ResourcePolicyRest.NAME, "findAll");
    }

    @Override
    public Class<ResourcePolicyRest> getDomainClass() {
        return ResourcePolicyRest.class;
    }

    @PreAuthorize("hasPermission(#resourceUuid, 'dspaceObject', 'ADMIN')")
    @SearchRestMethod(name = "resource")
    public Page<ResourcePolicyRest> resource(@Parameter(value = "uuid", required = true) UUID resourceUuid,
                                      @Parameter(value = "action", required = false) String action, Pageable pageable) {

        List<ResourcePolicy> resourcePolisies = null;
        int total = 0;
        try {
            Context context = obtainContext();
            if (action != null) {
                int actionId = Constants.getActionID(action);
                resourcePolisies = resourcePolicyService.findByResouceUuidAndActionId(context, resourceUuid, actionId,
                        pageable.getOffset(), pageable.getOffset() + pageable.getPageSize());
                total = resourcePolicyService.countByResouceUuidAndActionId(context, resourceUuid, actionId);
            } else {
                resourcePolisies = resourcePolicyService.findByResouceUuid(context, resourceUuid,
                        pageable.getOffset(), pageable.getOffset() + pageable.getPageSize());
                total = resourcePolicyService.countByResourceUuid(context, resourceUuid);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        return converter.toRestPage(resourcePolisies, pageable, total, utils.obtainProjection(true));
    }

    @PreAuthorize("hasPermission(#epersonUuid, 'EPERSON', 'READ')")
    @SearchRestMethod(name = "eperson")
    public Page<ResourcePolicyRest> eperson(@Parameter(value = "uuid", required = true) UUID epersonUuid,
                                @Parameter(value = "resource", required = false) UUID resourceUuid, Pageable pageable) {

        List<ResourcePolicy> resourcePolisies = null;
        int total = 0;
        try {
            Context context = obtainContext();
            EPerson eperson = epersonService.find(context, epersonUuid);
            if (eperson == null) {
                return null;
            }
            if (resourceUuid != null) {
                resourcePolisies = resourcePolicyService.findByEPersonAndResourceUuid(context, eperson, resourceUuid,
                                   pageable.getOffset(), pageable.getOffset() + pageable.getPageSize());
                total = resourcePolicyService.countResourcePoliciesByEPersonAndResourceUuid(context,
                        resourceUuid, eperson);
            } else {
                resourcePolisies = resourcePolicyService.findByEPerson(context, eperson, pageable.getOffset(),
                        pageable.getOffset() + pageable.getPageSize());
                total = resourcePolicyService.countByEPerson(context, eperson);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        return converter.toRestPage(resourcePolisies, pageable, total, utils.obtainProjection(true));
    }

    @PreAuthorize("hasPermission(#groupUuid, 'GROUP', 'READ')")
    @SearchRestMethod(name = "group")
    public Page<ResourcePolicyRest> group(@Parameter(value = "uuid", required = true) UUID groupUuid,
                                @Parameter(value = "resource", required = false) UUID resourceUuid, Pageable pageable) {

        List<ResourcePolicy> resourcePolisies = null;
        int total = 0;
        try {
            Context context = obtainContext();
            Group group = groupService.find(context, groupUuid);
            if (group == null) {
                return null;
            }
            if (resourceUuid != null) {
                resourcePolisies = resourcePolicyService.findByGroupAndResourceUuid(context, group, resourceUuid,
                        pageable.getOffset(), pageable.getOffset() + pageable.getPageSize());
                total = resourcePolicyService.countByGroupAndResourceUuid(context, group, resourceUuid);
            } else {
                resourcePolisies = resourcePolicyService.findByGroup(context, group, pageable.getOffset(),
                        pageable.getOffset() + pageable.getPageSize());
                total = resourcePolicyService.countResourcePolicyByGroup(context, group);
            }

        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        return converter.toRestPage(resourcePolisies, pageable, total, utils.obtainProjection(true));
    }

    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    protected ResourcePolicyRest createAndReturn(Context context) throws AuthorizeException {

        String resourceUuidStr = getRequestService().getCurrentRequest().getServletRequest().getParameter("resource");
        String epersonUuidStr = getRequestService().getCurrentRequest().getServletRequest().getParameter("eperson");
        String groupUuidStr = getRequestService().getCurrentRequest().getServletRequest().getParameter("group");


        if (resourceUuidStr == null) {
            throw new MissingParameterException("Missing resource (uuid) parameter");
        }
        if ((epersonUuidStr == null && groupUuidStr == null) || (epersonUuidStr != null && groupUuidStr != null)) {
            throw new MissingParameterException("Both eperson than group parameters supploed, only one allowed");
        }

        HttpServletRequest req = getRequestService().getCurrentRequest().getHttpServletRequest();
        ObjectMapper mapper = new ObjectMapper();
        ResourcePolicyRest resourcePolicyRest = null;
        ResourcePolicy resourcePolicy = null;

        UUID resourceUuid = UUID.fromString(resourceUuidStr);

        try {
            resourcePolicyRest = mapper.readValue(req.getInputStream(), ResourcePolicyRest.class);
        } catch (IOException exIO) {
            throw new UnprocessableEntityException("error parsing the body " + exIO.getMessage(), exIO);
        }

        try {
            DSpaceObject dspaceObject = dspaceObjectUtils.findDSpaceObject(context, resourceUuid);
            if (dspaceObject == null) {
                throw new UnprocessableEntityException("DSpaceObject with this uuid: " + resourceUuid + " not found");
            }
            resourcePolicy = resourcePolicyService.create(context);
            resourcePolicy.setRpType(resourcePolicyRest.getPolicyType());
            resourcePolicy.setdSpaceObject(dspaceObject);
            resourcePolicy.setRpName(resourcePolicyRest.getName());
            resourcePolicy.setRpDescription(resourcePolicyRest.getDescription());
            resourcePolicy.setAction(Constants.getActionID(resourcePolicyRest.getAction()));
            resourcePolicy.setStartDate(resourcePolicyRest.getStartDate());
            resourcePolicy.setEndDate(resourcePolicyRest.getEndDate());
        } catch (SQLException excSQL) {
            throw new RuntimeException(excSQL.getMessage(), excSQL);
        }

        if (epersonUuidStr != null) {
            try {
                UUID epersonUuid = UUID.fromString(epersonUuidStr);
                EPerson ePerson = epersonService.find(context, epersonUuid);
                if (ePerson == null) {
                    throw new UnprocessableEntityException("EPerson with uuid: " + epersonUuid + " not found");
                }
                resourcePolicy.setEPerson(ePerson);
                resourcePolicyService.update(context, resourcePolicy);
            } catch (SQLException excSQL) {
                throw new RuntimeException(excSQL.getMessage(), excSQL);
            }
            return converter.toRest(resourcePolicy, Projection.DEFAULT);
        } else {
            try {
                UUID groupUuid = UUID.fromString(groupUuidStr);
                Group group = groupService.find(context, groupUuid);
                if (group == null) {
                    throw new UnprocessableEntityException("Group with uuid: " + groupUuid + " not found");
                }
                resourcePolicy.setGroup(group);
                resourcePolicyService.update(context, resourcePolicy);
            } catch (SQLException excSQL) {
                throw new RuntimeException(excSQL.getMessage(), excSQL);
            }
            return converter.toRest(resourcePolicy, Projection.DEFAULT);
        }
    }

    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    protected void delete(Context context, Integer id) throws AuthorizeException {
        ResourcePolicy resourcePolicy = null;
        try {
            resourcePolicy = resourcePolicyService.find(context, id);
            if (resourcePolicy == null) {
                throw new ResourceNotFoundException(
                        ResourcePolicyRest.CATEGORY + "." + ResourcePolicyRest.NAME + " with id: " + id + " not found");
            }
            resourcePolicyService.delete(context, resourcePolicy);
        } catch (SQLException e) {
            throw new RuntimeException("Unable to delete ResourcePolicy with id = " + id, e);
        }
    }

    @Override
    protected void patch(Context context, HttpServletRequest request, String apiCategory, String model, Integer id,
            Patch patch)
            throws RepositoryMethodNotImplementedException, SQLException, AuthorizeException, DCInputsReaderException {
        ResourcePolicyRest rest = findOne(id);
        for (Operation op : patch.getOperations()) {
            rest = resourcePolicyOperationPatchFactory.getOperationForPath(op.getPath()).perform(rest, op);
        }

        ResourcePolicy resourcePolicy = null;
        try {
            resourcePolicy = resourcePolicyService.find(context, id);
            resourcePolicy.setStartDate(rest.getStartDate());
            resourcePolicy.setEndDate(rest.getEndDate());
            resourcePolicy.setRpDescription(rest.getDescription());
            resourcePolicy.setRpName(rest.getName());
            resourcePolicyService.update(context, resourcePolicy);
        } catch (SQLException e) {
            throw new RuntimeException("Unable to patch ResourcePolicy with id = " + id, e);
        }
    }
}