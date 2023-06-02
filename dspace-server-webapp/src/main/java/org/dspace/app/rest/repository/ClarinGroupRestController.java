/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import static java.util.regex.Pattern.compile;
import static org.apache.http.HttpStatus.SC_OK;
import static org.dspace.app.rest.utils.ContextUtil.obtainContext;
import static org.dspace.app.rest.utils.RegexUtils.REGEX_UUID;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.GroupRest;
import org.dspace.app.rest.utils.Utils;
import org.dspace.app.util.AuthorizeUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.service.EPersonService;
import org.dspace.eperson.service.GroupService;
import org.dspace.xmlworkflow.storedcomponents.PoolTask;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;
import org.dspace.xmlworkflow.storedcomponents.service.PoolTaskService;
import org.dspace.xmlworkflow.storedcomponents.service.XmlWorkflowItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Specialized controller created for Clarin-Dspace group import.
 *
 * @author Michaela Paurikova (michaela.paurikova at dataquest.sk)
 */
@RestController
@RequestMapping("/api/clarin/" + GroupRest.CATEGORY + "/" + GroupRest.GROUPS)
public class ClarinGroupRestController {

    private static final Logger log = org.apache.logging.log4j.LogManager
            .getLogger(ClarinGroupRestController.class);
    @Autowired
    private GroupService groupService;
    @Autowired
    private EPersonService ePersonService;
    @Autowired
    Utils utils;
    @Autowired
    private XmlWorkflowItemService workflowItemService;
    @Autowired
    private PoolTaskService poolTaskService;

    /**
     * Method to add one or more subgroups to a group.
     * This method is similar with method addChildGroups in GroupRestController,
     * but here we remove from incorrectly input grouplink letter \".
     * The mapping for requested endpoint, for example
     * <pre>
     * {@code
     * https://<dspace.server.url>/api/clarin/eperson/groups/26453b4d-e513-44e8-8d5b-395f62972eff/subgroups
     * }
     * </pre>
     * @param uuid     the uuid of the group to add the subgroups to
     * @param response response
     * @param request  request
     * @throws SQLException       if database error
     * @throws AuthorizeException if authorization error
     */
    @PreAuthorize("hasAuthority('AUTHENTICATED')")
    @RequestMapping(method = POST, path = "/{uuid}/subgroups")
    public void addChildGroups(@PathVariable UUID uuid, HttpServletResponse response, HttpServletRequest request)
            throws SQLException, AuthorizeException {

        Context context = obtainContext(request);
        Group parentGroup = groupService.find(context, uuid);
        if (Objects.isNull(parentGroup)) {
            throw new ResourceNotFoundException("parent group is not found for uuid: " + uuid);
        }

        AuthorizeUtil.authorizeManageGroup(context, parentGroup);
        List<String> groupLinks = utils.getStringListFromRequest(request);

        List<Group> childGroups = new ArrayList<>();
        for (String groupLink : groupLinks) {
            groupLink = groupLink.replace("\"", "");
            Optional<Group> childGroup = findGroup(context, groupLink);
            if (!childGroup.isPresent() || !canAddGroup(context, parentGroup, childGroup.get())) {
                throw new UnprocessableEntityException("cannot add child group: " + groupLink);
            }
            childGroups.add(childGroup.get());
        }

        for (Group childGroup : childGroups) {
            groupService.addMember(context, parentGroup, childGroup);
        }
        // this is required to trigger the rebuild of the group2group cache
        groupService.update(context, parentGroup);
        context.complete();

        response.setStatus(SC_OK);
    }

    /**
     * Method to add one or more members to a group.
     * This method is similar with method addMembers in GroupRestController,
     * but here we remove from incorrectly input grouplink letter \".
     * The mapping for requested endpoint, for example
     * <pre>
     * {@code
     * https://<dspace.server.url>/api/clarin/eperson/groups/26453b4d-e513-44e8-8d5b-395f62972eff/epersons
     * }
     * </pre>
     * @param uuid     the uuid of the group to add the members to
     * @param response response
     * @param request  request
     * @throws SQLException       if database error
     * @throws AuthorizeException if authorization error
     */
    @PreAuthorize("hasAuthority('AUTHENTICATED')")
    @RequestMapping(method = POST, path = "/{uuid}/epersons")
    public void addMembers(@PathVariable UUID uuid, HttpServletResponse response, HttpServletRequest request)
            throws SQLException, AuthorizeException {

        Context context = obtainContext(request);

        Group parentGroup = groupService.find(context, uuid);
        if (parentGroup == null) {
            throw new ResourceNotFoundException("parent group is not found for uuid: " + uuid);
        }

        AuthorizeUtil.authorizeManageGroup(context, parentGroup);

        List<String> memberLinks = utils.getStringListFromRequest(request);

        List<EPerson> members = new ArrayList<>();
        for (String memberLink : memberLinks) {
            memberLink = memberLink.replace("\"", "");
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

        response.setStatus(SC_OK);
    }

    /**
     * Method to add eperson to group based on workflowitem.
     * This method exists because in dspace7 the epersons don't have rights to manipulate with workflowitems.
     * Only groups have these rights, so we add epersons into correcponding groups.
     * In dspace5, the epersons with these rights were in table tasklistitem.
     * <pre>
     * {@code
     * https://<dspace.server.url>/api/clarin/eperson/groups/takslistitem
     * }
     * </pre>
     * @param response response
     * @param request  request
     * @throws SQLException       if database error
     * @throws AuthorizeException if authorization error
     */
    @PreAuthorize("hasAuthority('AUTHENTICATED')")
    @RequestMapping(method = POST, path = "/tasklistitem")
    public void addTasklistitemMembers(HttpServletResponse response, HttpServletRequest request)
            throws SQLException, AuthorizeException {

        Context context = obtainContext(request);
        if (Objects.isNull(context)) {
            throw new RuntimeException("Context is null!");
        }

        //get workflowitem from request based on its id
        String wfString = request.getParameter("workflowitem_id");
        if (StringUtils.isBlank(wfString)) {
            log.error("Cannot add eperson to group based on workflowitem, " +
                    "because the workflowitem is entered incorrectly!");
            throw new RuntimeException("Cannot add eperson to group based on workflowitem, " +
                    "because the workflowitem is incorrectly entered!");
        }
        Integer wfId = getIntegerFromString(wfString);
        XmlWorkflowItem wf = workflowItemService.find(context, wfId);
        if (Objects.isNull(wf)) {
            log.error("Workflowitem with id: " + wfId + " doesn't exist!");
            throw new UnprocessableEntityException("Workflowitem with id: " + wfId + " doesn't exist!");
        }

        //find all created pooltasks for workflowitem
        List<PoolTask> poolTasks = poolTaskService.find(context, wf);
        if (CollectionUtils.isEmpty(poolTasks)) {
            log.error("Workflowitem with id: " + wfId + " doesn't exist any pooltask!");
            throw new UnprocessableEntityException("Workflowitem with id: " + wfId + " doesn't exist any pooltask!");
        }

        //get eperson from request
        String epersonUUIDString = request.getParameter("epersonUUID");
        if (StringUtils.isBlank(epersonUUIDString)) {
            log.error("Eperson UUID is entered incorrectly!");
            throw new RuntimeException("Eperson UUID is entered incorrectly!");
        }
        UUID epersonUUID = UUID.fromString(epersonUUIDString);
        EPerson ePerson = ePersonService.find(context, epersonUUID);
        if (Objects.isNull(ePerson)) {
            log.error("Eperson with id: " + epersonUUID + " is null!");
            throw new UnprocessableEntityException("Eperson with id: " + epersonUUID + " is null!");
        }

        //add eperson to group connecting with pooltask
        for (PoolTask poolTask: poolTasks) {
            Group group = poolTask.getGroup();
            if (Objects.isNull(group)) {
                continue;
            }
            if (!ePerson.getGroups().contains(group)) {
                groupService.addMember(context, group, ePerson);
                groupService.update(context, group);
            }
        }

        context.complete();
        response.setStatus(SC_OK);
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

    private Optional<EPerson> findEPerson(Context context, String groupLink) throws SQLException {

        EPerson ePerson = null;

        Pattern linkPattern = compile("^.*/(" + REGEX_UUID + ")/?$");
        Matcher matcher = linkPattern.matcher(groupLink);
        if (matcher.matches()) {
            ePerson = ePersonService.find(context, UUID.fromString(matcher.group(1)));
        }

        return Optional.ofNullable(ePerson);
    }

    private boolean canAddGroup(Context context, Group parentGroup, Group childGroup) throws SQLException {
        return !groupService.isParentOf(context, childGroup, parentGroup);
    }

    private Integer getIntegerFromString(String value) {
        Integer output = null;
        if (StringUtils.isNotBlank(value)) {
            output = Integer.parseInt(value);
        }
        return output;
    }
}
