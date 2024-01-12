/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.dspace.app.rest.utils.RegexUtils.REGEX_REQUESTMAPPING_IDENTIFIER_AS_UUID;

import java.io.IOException;
import java.sql.SQLException;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.dspace.app.rest.converter.ConverterService;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.GroupRest;
import org.dspace.app.rest.model.hateoas.GroupResource;
import org.dspace.app.rest.repository.CommunityRestRepository;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.app.util.AuthorizeUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Community;
import org.dspace.content.service.CommunityService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.ControllerUtils;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * This RestController will take care of all the calls for a specific community's admingroup
 * This is handled by calling "/api/core/communities/{uuid}/adminGroup" with the correct RequestMethod
 */
@RestController
@RequestMapping("/api/core/communities" + REGEX_REQUESTMAPPING_IDENTIFIER_AS_UUID + "/adminGroup")
public class CommunityAdminGroupRestController {

    @Autowired
    private CommunityService communityService;

    @Autowired
    private CommunityRestRepository communityRestRepository;

    @Autowired
    private ConverterService converterService;


    @Autowired
    private AuthorizeService authorizeService;

    /**
     * This method creates and returns an AdminGroup object for the given community
     * This is called by using RequestMethod.POST on the default url for this class
     * @param uuid      The UUID of the community for which we'll create an adminGroup
     * @param response  The current response
     * @param request   The current request
     * @return          The created AdminGroup
     * @throws SQLException         If something goes wrong
     * @throws AuthorizeException   If something goes wrong
     */
    @RequestMapping(method = RequestMethod.POST)
    @PreAuthorize("hasPermission(#uuid, 'COMMUNITY', 'WRITE')")
    public ResponseEntity<RepresentationModel<?>> postAdminGroup(@PathVariable UUID uuid, HttpServletResponse response,
                                                          HttpServletRequest request)
        throws SQLException, AuthorizeException {

        Context context = ContextUtil.obtainContext(request);
        Community community = communityService.find(context, uuid);

        if (community == null) {
            throw new ResourceNotFoundException("No such community: " + uuid);
        }
        AuthorizeUtil.authorizeManageAdminGroup(context, community);
        if (community.getAdministrators() != null) {
            throw new UnprocessableEntityException("The community with UUID: " + uuid + " already has " +
                                                       "an admin group");
        }
        GroupRest adminGroup = communityRestRepository.createAdminGroup(context, request, community);
        context.complete();
        GroupResource groupResource = converterService.toResource(adminGroup);
        return ControllerUtils.toResponseEntity(HttpStatus.CREATED, new HttpHeaders(), groupResource);
    }

    /**
     * This method takes care of the deletion of an AdminGroup for the given community
     * This is called by using RequestMethod.DELETE on the default url for this class
     * @param uuid      The UUID of the community for which we'll delete the AdminGroup
     * @param response  The current response
     * @param request   The current request
     * @return          An empty response if the deletion was successful
     * @throws SQLException         If something goes wrong
     * @throws AuthorizeException   If something goes wrong
     * @throws IOException          If something goes wrong
     */
    @RequestMapping(method = RequestMethod.DELETE)
    @PreAuthorize("hasPermission(#uuid, 'COMMUNITY', 'WRITE')")
    public ResponseEntity<RepresentationModel<?>> deleteAdminGroup(@PathVariable UUID uuid,
                                                                   HttpServletResponse response,
                                                                   HttpServletRequest request)
        throws SQLException, AuthorizeException, IOException {

        Context context = ContextUtil.obtainContext(request);
        Community community = communityService.find(context, uuid);
        if (community == null) {
            throw new ResourceNotFoundException("No such community: " + uuid);
        }
        AuthorizeUtil.authorizeManageAdminGroup(context, community);
        if (community.getAdministrators() == null) {
            throw new UnprocessableEntityException("The community with UUID: " + uuid + " doesn't have an admin " +
                                                       "group");
        }
        communityRestRepository.deleteAdminGroup(context, community);
        context.complete();
        return ControllerUtils.toEmptyResponse(HttpStatus.NO_CONTENT);
    }
}
