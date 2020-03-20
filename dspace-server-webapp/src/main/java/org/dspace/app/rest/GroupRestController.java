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
import static org.dspace.app.util.AuthorizeUtil.authorizeManageAdminGroup;
import static org.dspace.app.util.AuthorizeUtil.authorizeManageSubmittersGroup;
import static org.dspace.app.util.AuthorizeUtil.authorizeManageWorkflowsGroup;
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
import org.dspace.app.rest.model.GroupRest;
import org.dspace.app.rest.utils.GroupUtil;
import org.dspace.app.rest.utils.Utils;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.service.EPersonService;
import org.dspace.eperson.service.GroupService;
import org.dspace.xmlworkflow.storedcomponents.CollectionRole;
import org.dspace.xmlworkflow.storedcomponents.service.CollectionRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * This will be the entry point for the api/eperson/groups endpoint with additional paths to it
 */
@RestController
@RequestMapping("/api/" + GroupRest.CATEGORY + "/" + GroupRest.GROUPS)
public class GroupRestController {

    @Autowired
    private GroupService groupService;

    @Autowired
    private EPersonService ePersonService;

    @Autowired
    private AuthorizeService authorizeService;

    @Autowired
    private CollectionRoleService collectionRoleService;

    @Autowired
    Utils utils;

    @Autowired
    GroupUtil groupUtil;

    /**
     * Method to add one or more subgroups to a group.
     * The subgroups to be added should be provided in the request body as a uri-list.
     * Note that only the 'AUTHENTICATED' state will be checked in PreAuthorize, a more detailed check will be done by
     * using the 'checkAuthorization' method.
     *
     * @param uuid the uuid of the group to add the subgroups to
     */
    @PreAuthorize("hasAuthority('AUTHENTICATED')")
    @RequestMapping(method = POST, path = "/{uuid}/subgroups", consumes = {"text/uri-list"})
    public void addChildGroups(@PathVariable UUID uuid, HttpServletResponse response, HttpServletRequest request)
            throws SQLException, AuthorizeException {

        Context context = obtainContext(request);

        Group parentGroup = groupService.find(context, uuid);
        if (parentGroup == null) {
            throw new ResourceNotFoundException("parent group is not found for uuid: " + uuid);
        }

        checkAuthorization(context, parentGroup);

        List<String> groupLinks = utils.getStringListFromRequest(request);

        List<Group> childGroups = new ArrayList<>();
        for (String groupLink : groupLinks) {
            Optional<Group> childGroup = findGroup(context, groupLink);
            if (!childGroup.isPresent() || !canAddGroup(context, parentGroup, childGroup.get())) {
                throw new UnprocessableEntityException("cannot add child group: " + groupLink);
            }
            childGroups.add(childGroup.get());
        }

        for (Group childGroup : childGroups) {
            groupService.addMember(context, parentGroup, childGroup);
        }

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

    private boolean canAddGroup(Context context, Group parentGroup, Group childGroup) throws SQLException {

        return !groupService.isParentOf(context, childGroup, parentGroup);
    }

    /**
     * Method to add one or more members to a group.
     * The members to be added should be provided in the request body as a uri-list.
     * Note that only the 'AUTHENTICATED' state will be checked in PreAuthorize, a more detailed check will be done by
     * using the 'checkAuthorization' method.
     *
     * @param uuid the uuid of the group to add the members to
     */
    @PreAuthorize("hasAuthority('AUTHENTICATED')")
    @RequestMapping(method = POST, path = "/{uuid}/epersons", consumes = {"text/uri-list"})
    public void addMembers(@PathVariable UUID uuid, HttpServletResponse response, HttpServletRequest request)
            throws SQLException, AuthorizeException {

        Context context = obtainContext(request);

        Group parentGroup = groupService.find(context, uuid);
        if (parentGroup == null) {
            throw new ResourceNotFoundException("parent group is not found for uuid: " + uuid);
        }

        checkAuthorization(context, parentGroup);

        List<String> memberLinks = utils.getStringListFromRequest(request);

        List<EPerson> members = new ArrayList<>();
        for (String memberLink : memberLinks) {
            Optional<EPerson> member = findEPerson(context, memberLink);
            if (!member.isPresent()) {
                throw new UnprocessableEntityException("cannot add child group: " + memberLink);
            }
            members.add(member.get());
        }

        for (EPerson member : members) {
            groupService.addMember(context, parentGroup, member);
        }

        context.complete();

        response.setStatus(SC_NO_CONTENT);
    }

    private Optional<EPerson> findEPerson(Context context, String groupLink) throws SQLException {

        EPerson ePerson = null;

        Pattern linkPattern = compile("^.*/(" + REGEX_UUID + ")/?$");
        Matcher matcher = linkPattern.matcher(groupLink);
        if (matcher.matches()) {
            ePerson = ePersonService.find(context, UUID.fromString(matcher.group(1)));
        }

        return Optional.ofNullable(ePerson);
    }

    /**
     * Method to remove a subgroup from a group.
     * Note that only the 'AUTHENTICATED' state will be checked in PreAuthorize, a more detailed check will be done by
     * using the 'checkAuthorization' method.
     *
     * @param parentUUID the uuid of the parent group
     * @param childUUID  the uuid of the subgroup which has to be removed
     */
    @PreAuthorize("hasAuthority('AUTHENTICATED')")
    @RequestMapping(method = DELETE, path = "/{parentUUID}/subgroups/{childUUID}")
    public void removeChildGroup(@PathVariable UUID parentUUID, @PathVariable UUID childUUID,
                                 HttpServletResponse response, HttpServletRequest request)
            throws IOException, SQLException, AuthorizeException {

        Context context = obtainContext(request);

        Group parentGroup = groupService.find(context, parentUUID);
        if (parentGroup == null) {
            throw new ResourceNotFoundException("parent group is not found for uuid: " + parentUUID);
        }

        checkAuthorization(context, parentGroup);

        Group childGroup = groupService.find(context, childUUID);
        if (childGroup == null) {
            response.sendError(SC_UNPROCESSABLE_ENTITY);
        }

        groupService.removeMember(context, parentGroup, childGroup);

        context.complete();

        response.setStatus(SC_NO_CONTENT);
    }

    /**
     * Method to remove a member from a group.
     * Note that only the 'AUTHENTICATED' state will be checked in PreAuthorize, a more detailed check will be done by
     * using the 'checkAuthorization' method.
     *
     * @param parentUUID the uuid of the parent group
     * @param memberUUID the uuid of the member which has to be removed
     */
    @PreAuthorize("hasAuthority('AUTHENTICATED')")
    @RequestMapping(method = DELETE, path = "/{parentUUID}/epersons/{memberUUID}")
    public void removeMember(@PathVariable UUID parentUUID, @PathVariable UUID memberUUID,
                             HttpServletResponse response, HttpServletRequest request)
            throws IOException, SQLException, AuthorizeException {

        Context context = obtainContext(request);

        Group parentGroup = groupService.find(context, parentUUID);
        if (parentGroup == null) {
            throw new ResourceNotFoundException("parent group is not found for uuid: " + parentUUID);
        }

        checkAuthorization(context, parentGroup);

        EPerson childGroup = ePersonService.find(context, memberUUID);
        if (childGroup == null) {
            response.sendError(SC_UNPROCESSABLE_ENTITY);
        }

        groupService.removeMember(context, parentGroup, childGroup);

        context.complete();

        response.setStatus(SC_NO_CONTENT);
    }

    /**
     * This method checks whether the current user has sufficient rights to modify the group.
     * Depending on the kind of group and due to delegated administration, separate checks need to be done to verify
     * whether the user is allowed to modify the group.
     *
     * @param context the context of which the user will be checked
     * @param group   the group to be checked
     * @throws SQLException
     * @throws AuthorizeException
     */
    private void checkAuthorization(Context context, Group group) throws SQLException, AuthorizeException {

        if (authorizeService.isAdmin(context)) {
            return;
        }

        Collection collection = groupUtil.getCollection(context, group);
        if (collection != null) {

            if (group.equals(collection.getSubmitters())) {
                authorizeManageSubmittersGroup(context, collection);
                return;
            }


            List<CollectionRole> collectionRoles = collectionRoleService.findByCollection(context, collection);
            for (CollectionRole role : collectionRoles) {
                if (group.equals(role.getGroup())) {
                    authorizeManageWorkflowsGroup(context, collection);
                    return;
                }
            }

            if (group.equals(collection.getAdministrators())) {
                authorizeManageAdminGroup(context, collection);
                return;
            }
        }

        Community community = groupUtil.getCommunity(context, group);
        if (community != null) {
            authorizeManageAdminGroup(context, community);
            return;
        }

        throw new AuthorizeException("not authorized to manage this group");
    }
}
