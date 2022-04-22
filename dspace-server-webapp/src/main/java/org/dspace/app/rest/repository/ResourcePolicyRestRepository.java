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
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.dspace.app.rest.DiscoverableEndpointsService;
import org.dspace.app.rest.Parameter;
import org.dspace.app.rest.SearchRestMethod;
import org.dspace.app.rest.exception.MissingParameterException;
import org.dspace.app.rest.exception.RESTAuthorizationException;
import org.dspace.app.rest.exception.RepositoryMethodNotImplementedException;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.ResourcePolicyRest;
import org.dspace.app.rest.model.patch.Patch;
import org.dspace.app.rest.repository.patch.ResourcePatch;
import org.dspace.app.rest.utils.DSpaceObjectUtils;
import org.dspace.app.rest.utils.Utils;
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
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.hateoas.Link;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * Controller for exposition of default access condition
 *
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science.it)
 */
@Component(ResourcePolicyRest.CATEGORY + "." + ResourcePolicyRest.NAME)
public class ResourcePolicyRestRepository extends DSpaceRestRepository<ResourcePolicyRest, Integer>
                                          implements InitializingBean {

    @Autowired
    ResourcePolicyService resourcePolicyService;

    @Autowired
    Utils utils;

    @Autowired
    private EPersonService epersonService;

    @Autowired
    private GroupService groupService;

    @Autowired
    DSpaceObjectUtils dspaceObjectUtils;

    @Autowired
    ResourcePatch<ResourcePolicy> resourcePatch;

    @Autowired
    DiscoverableEndpointsService discoverableEndpointsService;

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

    /**
     * Find the resource policies matching the uuid of the resource object and/or the specified action
     *
     * @param resourceUuid mandatory, the uuid of the resource object of the policy
     * @param action       optional, limit the returned policies to the specified action
     * @param pageable     contains the pagination information
     * @return a Page of ResourcePolicyRest instances matching the uuid of the resource object and/or the specified
     * action
     */
    @PreAuthorize("hasPermission(#resourceUuid, 'dspaceObject', 'ADMIN')")
    @SearchRestMethod(name = "resource")
    public Page<ResourcePolicyRest> findByResource(@Parameter(value = "uuid", required = true) UUID resourceUuid,
                                                   @Parameter(value = "action", required = false) String action,
                                                   Pageable pageable) {
        List<ResourcePolicy> resourcePolisies = null;
        int total = 0;
        try {
            Context context = obtainContext();
            if (action != null) {
                int actionId = Constants.getActionID(action);
                resourcePolisies = resourcePolicyService.findByResouceUuidAndActionId(context, resourceUuid, actionId,
                    Math.toIntExact(pageable.getOffset()),
                    Math.toIntExact(pageable.getPageSize()));
                total = resourcePolicyService.countByResouceUuidAndActionId(context, resourceUuid, actionId);
            } else {
                resourcePolisies = resourcePolicyService.findByResouceUuid(context, resourceUuid,
                    Math.toIntExact(pageable.getOffset()),
                    Math.toIntExact(pageable.getPageSize()));
                total = resourcePolicyService.countByResourceUuid(context, resourceUuid);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        return converter.toRestPage(resourcePolisies, pageable, total, utils.obtainProjection());
    }

    /**
     * Find the resource policies matching uuid of the eperson and/or the one specified resource object
     *
     * @param epersonUuid  mandatory, the uuid of the eperson that benefit of the policy
     * @param resourceUuid optional, limit the returned policies to the ones related to the specified resource
     * @param pageable     contains the pagination information
     * @return It returns the list of explicit matching resource policies, no inherited or broader resource policies
     * will be included in the list nor policies derived by groups' membership
     */
    @PreAuthorize("hasPermission(#epersonUuid, 'EPERSON', 'READ')")
    @SearchRestMethod(name = "eperson")
    public Page<ResourcePolicyRest> findByEPerson(@Parameter(value = "uuid", required = true) UUID epersonUuid,
                                                  @Parameter(value = "resource", required = false) UUID resourceUuid,
                                                  Pageable pageable) {
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
                    Math.toIntExact(pageable.getOffset()),
                    Math.toIntExact(pageable.getPageSize()));
                total = resourcePolicyService.countResourcePoliciesByEPersonAndResourceUuid(context,
                    eperson, resourceUuid);
            } else {
                resourcePolisies = resourcePolicyService.findByEPerson(context, eperson,
                    Math.toIntExact(pageable.getOffset()),
                    Math.toIntExact(pageable.getPageSize()));
                total = resourcePolicyService.countByEPerson(context, eperson);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        return converter.toRestPage(resourcePolisies, pageable, total, utils.obtainProjection());
    }

    /**
     * Find the resource policies matching uuid of the group and/or the ones specified resource object
     *
     * @param groupUuid    mandatory, the uuid of the group that benefit of the policy
     * @param resourceUuid optional, limit the returned policies to the ones related to the specified resource
     * @param pageable     contains the pagination information
     * @return It returns the list of explicit matching resource policies, no inherited or broader resource policies
     * will be included in the list nor policies derived by groups' membership
     */
    @PreAuthorize("hasPermission(#groupUuid, 'GROUP', 'READ')")
    @SearchRestMethod(name = "group")
    public Page<ResourcePolicyRest> findByGroup(@Parameter(value = "uuid", required = true) UUID groupUuid,
                                                @Parameter(value = "resource", required = false) UUID resourceUuid,
                                                Pageable pageable) {
        List<ResourcePolicy> resourcePolisies = null;
        int total = 0;
        try {
            Context context = obtainContext();
            if (context.getCurrentUser() == null) {
                throw new RESTAuthorizationException("Only loggedin users can search resource policies by group");
            }
            Group group = groupService.find(context, groupUuid);
            if (group == null) {
                return null;
            }
            if (resourceUuid != null) {
                resourcePolisies = resourcePolicyService.findByGroupAndResourceUuid(context, group, resourceUuid,
                    Math.toIntExact(pageable.getOffset()),
                    Math.toIntExact(pageable.getPageSize()));
                total = resourcePolicyService.countByGroupAndResourceUuid(context, group, resourceUuid);
            } else {
                resourcePolisies = resourcePolicyService.findByGroup(context, group,
                    Math.toIntExact(pageable.getOffset()),
                    Math.toIntExact(pageable.getPageSize()));
                total = resourcePolicyService.countResourcePolicyByGroup(context, group);
            }

        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        return converter.toRestPage(resourcePolisies, pageable, total, utils.obtainProjection());
    }

    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    protected ResourcePolicyRest createAndReturn(Context context) throws AuthorizeException, SQLException {

        String resourceUuidStr = getRequestService().getCurrentRequest().getServletRequest().getParameter("resource");
        String epersonUuidStr = getRequestService().getCurrentRequest().getServletRequest().getParameter("eperson");
        String groupUuidStr = getRequestService().getCurrentRequest().getServletRequest().getParameter("group");


        if (resourceUuidStr == null) {
            throw new MissingParameterException("Missing resource (uuid) parameter");
        }
        if ((epersonUuidStr == null && groupUuidStr == null) || (epersonUuidStr != null && groupUuidStr != null)) {
            throw new MissingParameterException("Both eperson than group parameters supplied, only one allowed");
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
            return converter.toRest(resourcePolicy, utils.obtainProjection());
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
            return converter.toRest(resourcePolicy, utils.obtainProjection());
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
    @PreAuthorize("hasPermission(#id, 'resourcepolicy', 'ADMIN')")
    protected void patch(Context context, HttpServletRequest request, String apiCategory, String model, Integer id,
                         Patch patch) throws RepositoryMethodNotImplementedException, SQLException, AuthorizeException {
        ResourcePolicy resourcePolicy = resourcePolicyService.find(context, id);
        if (resourcePolicy == null) {
            throw new ResourceNotFoundException(
                ResourcePolicyRest.CATEGORY + "." + ResourcePolicyRest.NAME + " with id: " + id + " not found");
        }
        resourcePatch.patch(obtainContext(), resourcePolicy, patch.getOperations());
        resourcePolicyService.update(context, resourcePolicy);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        discoverableEndpointsService.register(this, Arrays.asList(
                      Link.of("/api/" + ResourcePolicyRest.CATEGORY + "/" + ResourcePolicyRest.PLURAL_NAME + "/search",
                                         ResourcePolicyRest.PLURAL_NAME + "-search")));
    }
}
