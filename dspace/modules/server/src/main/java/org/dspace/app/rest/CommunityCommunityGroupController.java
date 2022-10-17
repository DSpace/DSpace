/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static java.lang.Integer.parseInt;
import static java.util.stream.Collectors.toList;
import static org.dspace.app.rest.utils.ContextUtil.obtainContext;

import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;

import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.converter.ConverterService;
import org.dspace.app.rest.exception.DSpaceBadRequestException;
import org.dspace.app.rest.model.CommunityRest;
import org.dspace.app.rest.model.hateoas.CommunityResource;
import org.dspace.app.rest.utils.Utils;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Community;
import org.dspace.content.CommunityGroup;
import org.dspace.content.service.CommunityGroupService;
import org.dspace.content.service.CommunityService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * UMD Custom Class for LIBDRUM-701
 * 
 * This RestController takes care of the creation and deletion of Communities'
 * nested objects
 * This class will typically receive the UUID of a Community and it'll perform
 * logic on its nested objects
 * 
 * @author Mohamed Abdul Rasheed (mohideen at umd.edu)
 */
@RestController
@RequestMapping("/api/" + CommunityRest.CATEGORY + "/" + CommunityRest.PLURAL_NAME
        + CommunityCommunityGroupController.REGEX_REQUESTMAPPING_IDENTIFIER_AS_UUID + "/communityGroup")
public class CommunityCommunityGroupController {

    /**
     * Regular expression in the request mapping to accept UUID as identifier
     */
    protected static final String REGEX_REQUESTMAPPING_IDENTIFIER_AS_UUID = "/{uuid:[0-9a-fxA-FX]{8}-[0-9a-fxA-FX]{4}-[0-9a-fxA-FX]{4}-[0-9a-fxA-FX]{4}-[0-9a-fxA-FX]{12}}";

    @Autowired
    protected Utils utils;

    @Autowired
    private CommunityService communityService;

    @Autowired
    private CommunityGroupService communityGroupService;

    @Autowired
    ConverterService converter;

    private static final Logger log = org.apache.logging.log4j.LogManager
            .getLogger(CommunityCommunityGroupController.class);

    /**
     * This method will update the community group of the community that
     * corresponds to the provided community uuid.
     *
     * @param uuid    The UUID of the community for which to update the community
     *                format
     * @param request The request object
     * @return The wrapped resource containing the community which in turn contains
     *         the community group
     * @throws SQLException If something goes wrong in the database
     */
    @RequestMapping(method = RequestMethod.PUT, consumes = { "text/uri-list" })
    @PreAuthorize("hasPermission(#uuid, 'COMMUNITY', 'WRITE')")
    @PostAuthorize("returnObject != null")
    public CommunityResource updateCommunityGroup(@PathVariable UUID uuid,
            HttpServletRequest request) throws SQLException, AuthorizeException {

        Context context = obtainContext(request);

        List<CommunityGroup> communityGroups = constructCommunityGroupFormatList(request, context);

        if (communityGroups.size() > 1) {
            throw new DSpaceBadRequestException("Only one community group is allowed");
        }

        CommunityGroup communityGroup = communityGroups.stream().findFirst()
                .orElseThrow(() -> new DSpaceBadRequestException("No valid community group was provided"));

        Community community = communityService.find(context, uuid);

        if (community == null) {
            throw new ResourceNotFoundException("Community with id: " + uuid + " not found");
        }

        community.setGroupID(communityGroup.getID());
        communityService.update(context, community);

        context.commit();

        CommunityRest communityRest = converter.toRest(context.reloadEntity(community), utils.obtainProjection());
        return converter.toResource(communityRest);
    }

    /**
     * This method will construct a List of CommunityGroups out of a request.
     * It will call the {@link Utils#getStringListFromRequest(HttpServletRequest)}
     * method to retrieve a list of links
     * out of the request.
     * The method will iterate over this list of links and parse the links to
     * retrieve the integer ID from it.
     * It will then retrieve the CommunityGroup corresponding to this ID.
     * If one is found, this CommunityGroup is added to the List of
     * CommunityGroups that we will return.
     *
     * @param request The request out of which we'll create the List of
     *                CommunityGroups
     * @param context The relevant DSpace context
     * @return The resulting list of CommunityGroups that we parsed out of the
     *         request
     */
    public List<CommunityGroup> constructCommunityGroupFormatList(HttpServletRequest request, Context context) {

        return utils.getStringListFromRequest(request).stream()
                .map(link -> {
                    if (link.endsWith("/")) {
                        link = link.substring(0, link.length() - 1);
                    }
                    return link.substring(link.lastIndexOf('/') + 1);
                })
                .map(id -> {
                    try {
                        return communityGroupService.find(parseInt(id));
                    } catch (NumberFormatException e) {
                        log.error("Could not find community group format for id: " + id, e);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(toList());
    }

}
