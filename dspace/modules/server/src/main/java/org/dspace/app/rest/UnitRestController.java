/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static java.util.regex.Pattern.compile;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_UNPROCESSABLE_ENTITY;
import static org.dspace.app.rest.utils.ContextUtil.obtainContext;
import static org.dspace.app.rest.utils.RegexUtils.REGEX_UUID;
import static org.springframework.web.bind.annotation.RequestMethod.DELETE;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.UnitRest;
import org.dspace.app.rest.utils.Utils;
import org.dspace.app.util.AuthorizeUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.Unit;
import org.dspace.eperson.service.EPersonService;
import org.dspace.eperson.service.GroupService;
import org.dspace.eperson.service.UnitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * This will be the entry point for the api/eperson/units endpoint with additional paths to it
 */
@RestController
@RequestMapping("/api/" + UnitRest.CATEGORY + "/" + UnitRest.UNITS)
public class UnitRestController {

    @Autowired
    private GroupService groupService;

    @Autowired
    private EPersonService ePersonService;

    @Autowired
    private UnitService unitService;

    @Autowired
    Utils utils;

    /**
     * Method to add one or more groups to a unit
     * The groups to be added should be provided in the request body as a uri-list.
     *
     * @param uuid the UUID of the unit to add the groups to
     */
//    @PreAuthorize("hasAuthority('AUTHENTICATED')")
    @PreAuthorize("hasAuthority('ADMIN')")
    @RequestMapping(method = POST, path = "/{uuid}/groups", consumes = {"text/uri-list"})
    public void addGroups(@PathVariable UUID uuid, HttpServletResponse response, HttpServletRequest request)
            throws SQLException, AuthorizeException {

        Context context = obtainContext(request);

        Unit unit = unitService.find(context, uuid);
        if (unit == null) {
            throw new ResourceNotFoundException("unit is not found for uuid: " + uuid);
        }

//        AuthorizeUtil.authorizeManageGroup(context, parentGroup);

        List<String> groupLinks = utils.getStringListFromRequest(request);

        List<Group> groups = new ArrayList<>();
        for (String groupLink : groupLinks) {
            Optional<Group> group = findGroup(context, groupLink);
            if (!group.isPresent() /*|| !canAddGroup(context, unit, group.get())*/) {
                throw new UnprocessableEntityException("cannot add group: " + groupLink);
            }
            groups.add(group.get());
        }

        for (Group group : groups) {
            unitService.addGroup(context, unit, group);
        }
        // ??? this is required to trigger the rebuild of the group2group cache
        unitService.update(context, unit);
        context.complete();

        response.setStatus(SC_NO_CONTENT);
    }

    private Optional<Group> findGroup(Context context, String groupLink) throws SQLException {

        Group group = null;

        Pattern linkPattern = compile("^.*/(" + REGEX_UUID + ")/?$");
        Matcher matcher = linkPattern.matcher(groupLink);
        if (matcher.matches()) {
            group = groupService.find(context, UUID.fromString(matcher.group(1)));
        }

        return Optional.ofNullable(group);
    }
//
//    private boolean canAddGroup(Context context, Group parentGroup, Group childGroup) throws SQLException {
//
//        return !groupService.isParentOf(context, childGroup, parentGroup);
//    }
//
//    /**
//     * Method to add one or more members to a group.
//     * The members to be added should be provided in the request body as a uri-list.
//     * Note that only the 'AUTHENTICATED' state will be checked in PreAuthorize, a more detailed check will be done by
//     * using the 'checkAuthorization' method.
//     *
//     * @param uuid the uuid of the group to add the members to
//     */
//    @PreAuthorize("hasAuthority('AUTHENTICATED')")
//    @RequestMapping(method = POST, path = "/{uuid}/epersons", consumes = {"text/uri-list"})
//    public void addMembers(@PathVariable UUID uuid, HttpServletResponse response, HttpServletRequest request)
//            throws SQLException, AuthorizeException {
//
//        Context context = obtainContext(request);
//
//        Group parentGroup = groupService.find(context, uuid);
//        if (parentGroup == null) {
//            throw new ResourceNotFoundException("parent group is not found for uuid: " + uuid);
//        }
//
//        AuthorizeUtil.authorizeManageGroup(context, parentGroup);
//
//        List<String> memberLinks = utils.getStringListFromRequest(request);
//
//        List<EPerson> members = new ArrayList<>();
//        for (String memberLink : memberLinks) {
//            Optional<EPerson> member = findEPerson(context, memberLink);
//            if (!member.isPresent()) {
//                throw new UnprocessableEntityException("cannot add child group: " + memberLink);
//            }
//            members.add(member.get());
//        }
//
//        for (EPerson member : members) {
//            groupService.addMember(context, parentGroup, member);
//        }
//
//        context.complete();
//
//        response.setStatus(SC_NO_CONTENT);
//    }
//
//    private Optional<EPerson> findEPerson(Context context, String groupLink) throws SQLException {
//
//        EPerson ePerson = null;
//
//        Pattern linkPattern = compile("^.*/(" + REGEX_UUID + ")/?$");
//        Matcher matcher = linkPattern.matcher(groupLink);
//        if (matcher.matches()) {
//            ePerson = ePersonService.find(context, UUID.fromString(matcher.group(1)));
//        }
//
//        return Optional.ofNullable(ePerson);
//    }
//
    /**
     * Method to remove a group from a unit.
     *
     * @param unitUUID the UUID of the unit
     * @param groupUUID  the UUID of the group to remove
     */
//    @PreAuthorize("hasAuthority('AUTHENTICATED')")
    @PreAuthorize("hasAuthority('ADMIN')")
    @RequestMapping(method = DELETE, path = "/{unitUUID}/groups/{groupUUID}")
    public void removeGroup(@PathVariable UUID unitUUID, @PathVariable UUID groupUUID,
                                 HttpServletResponse response, HttpServletRequest request)
            throws IOException, SQLException, AuthorizeException {

        Context context = obtainContext(request);

        Unit unit = unitService.find(context, unitUUID);
        if (unit == null) {
            throw new ResourceNotFoundException("unit is not found for uuid: " + unitUUID);
        }

//        AuthorizeUtil.authorizeManageGroup(context, parentGroup);

        Group group = groupService.find(context, groupUUID);
        if (group == null) {
            response.sendError(SC_UNPROCESSABLE_ENTITY);
            return;
        }

        unitService.removeGroup(context, unit, group);
        // ??? this is required to trigger the rebuild of the group2group cache
        unitService.update(context, unit);;
        context.complete();

        response.setStatus(SC_NO_CONTENT);
    }
//
//    /**
//     * Method to remove a member from a group.
//     * Note that only the 'AUTHENTICATED' state will be checked in PreAuthorize, a more detailed check will be done by
//     * using the 'checkAuthorization' method.
//     *
//     * @param parentUUID the uuid of the parent group
//     * @param memberUUID the uuid of the member which has to be removed
//     */
//    @PreAuthorize("hasAuthority('AUTHENTICATED')")
//    @RequestMapping(method = DELETE, path = "/{parentUUID}/epersons/{memberUUID}")
//    public void removeMember(@PathVariable UUID parentUUID, @PathVariable UUID memberUUID,
//                             HttpServletResponse response, HttpServletRequest request)
//            throws IOException, SQLException, AuthorizeException {
//
//        Context context = obtainContext(request);
//
//        Group parentGroup = groupService.find(context, parentUUID);
//        if (parentGroup == null) {
//            throw new ResourceNotFoundException("parent group is not found for uuid: " + parentUUID);
//        }
//
//        AuthorizeUtil.authorizeManageGroup(context, parentGroup);
//
//        EPerson childGroup = ePersonService.find(context, memberUUID);
//        if (childGroup == null) {
//            response.sendError(SC_UNPROCESSABLE_ENTITY);
//        }
//
//        groupService.removeMember(context, parentGroup, childGroup);
//
//        context.complete();
//
//        response.setStatus(SC_NO_CONTENT);
//    }
}

