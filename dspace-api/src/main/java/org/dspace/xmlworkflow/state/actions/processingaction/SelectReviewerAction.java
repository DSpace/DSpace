/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xmlworkflow.state.actions.processingaction;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.ArrayUtils;
import org.dspace.app.util.Util;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.service.EPersonService;
import org.dspace.eperson.service.GroupService;
import org.dspace.services.ConfigurationService;
import org.dspace.xmlworkflow.Role;
import org.dspace.xmlworkflow.state.Step;
import org.dspace.xmlworkflow.state.actions.ActionAdvancedInfo;
import org.dspace.xmlworkflow.state.actions.ActionResult;
import org.dspace.xmlworkflow.storedcomponents.WorkflowItemRole;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;
import org.dspace.xmlworkflow.storedcomponents.service.WorkflowItemRoleService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Processing class for an action where an assigned user can
 * assign another user to review the item
 *
 * @author Bram De Schouwer (bram.deschouwer at dot com)
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 */
public class SelectReviewerAction extends ProcessingAction {

    public static final int SEARCH_RESULTS_PAGE = 1;

    public static final int RESULTS_PER_PAGE = 5;

    private static final String SUBMIT_CANCEL = "submit_cancel";
    private static final String SUBMIT_SEARCH = "submit_search";
    private static final String SUBMIT_SELECT_REVIEWER = "submit_select_reviewer";

    private Role role;

    @Autowired(required = true)
    private EPersonService ePersonService;

    @Autowired(required = true)
    private WorkflowItemRoleService workflowItemRoleService;

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private GroupService groupService;

    @Override
    public void activate(Context c, XmlWorkflowItem wf) {

    }

    @Override
    public ActionResult execute(Context c, XmlWorkflowItem wfi, Step step, HttpServletRequest request)
            throws SQLException, AuthorizeException {
        String submitButton = Util.getSubmitButton(request, SUBMIT_CANCEL);

        //Check if our user has pressed cancel
        if (submitButton.equals(SUBMIT_CANCEL)) {
            //Send us back to the submissions page
            return new ActionResult(ActionResult.TYPE.TYPE_CANCEL);
        } else if (submitButton.startsWith(SUBMIT_SELECT_REVIEWER)) {
            return processSelectReviewers(c, wfi, step, request);
        }

        //There are only 2 active buttons on this page, so if anything else happens just return an error
        return new ActionResult(ActionResult.TYPE.TYPE_ERROR);
    }

    /**
     * Method to handle the "submit_select_reviewer" action
     *
     * @param c       current DSpace session
     * @param wfi     the item on which the action is to be performed
     * @param step    the workflow step in which the action is performed
     * @param request the current client request
     * @return the result of performing the action
     * @throws SQLException
     * @throws AuthorizeException
     */
    public ActionResult processSelectReviewers(Context c, XmlWorkflowItem wfi, Step step, HttpServletRequest request)
        throws SQLException, AuthorizeException {
        //Retrieve the identifier of the eperson which will do the reviewing
        String[] reviewerIds = request.getParameterValues("eperson");
        if (ArrayUtils.isEmpty(reviewerIds)) {
            return new ActionResult(ActionResult.TYPE.TYPE_ERROR);
        }
        List<EPerson> reviewers = new ArrayList<>();
        for (String reviewerId : reviewerIds) {
            reviewers.add(ePersonService.find(c, UUID.fromString(reviewerId)));
        }
        if (reviewers.size() == 1) {
            if (!groupService.allMembers(c, getGroup()).contains(reviewers.get(0))) {
                return new ActionResult(ActionResult.TYPE.TYPE_ERROR);
            }
            //We have a reviewer, assign him, the workflowitemrole will be translated into a task in the autoassign
            WorkflowItemRole workflowItemRole = workflowItemRoleService.create(c);
            workflowItemRole.setEPerson(reviewers.get(0));
            workflowItemRole.setRoleId(getRole().getId());
            workflowItemRole.setWorkflowItem(wfi);
            workflowItemRoleService.update(c, workflowItemRole);
            return new ActionResult(ActionResult.TYPE.TYPE_OUTCOME, ActionResult.OUTCOME_COMPLETE);
        } else {
            if (!groupService.allMembers(c, getGroup()).containsAll(reviewers)) {
                return new ActionResult(ActionResult.TYPE.TYPE_ERROR);
            }
            //We have multiple reviewers, create a group and assign this group, the workflowitemrole will be
            // translated into a task in the autoassign
            c.turnOffAuthorisationSystem();
            Group reviewerGroup = groupService.create(c);
            for (EPerson reviewer : reviewers) {
                groupService.addMember(c, reviewerGroup, reviewer);
            }
            c.restoreAuthSystemState();
            WorkflowItemRole workflowItemRole = workflowItemRoleService.create(c);
            workflowItemRole.setGroup(reviewerGroup);
            workflowItemRole.setRoleId(getRole().getId());
            workflowItemRole.setWorkflowItem(wfi);
            workflowItemRoleService.update(c, workflowItemRole);
            return new ActionResult(ActionResult.TYPE.TYPE_OUTCOME, ActionResult.OUTCOME_COMPLETE);
        }
    }

    @Override
    public List<String> getOptions() {
        List<String> options = new ArrayList<>();
        options.add(SUBMIT_SELECT_REVIEWER);
        options.add(RETURN_TO_POOL);
        return options;
    }

    @Override
    protected List<String> getAdvancedOptions() {
        return Arrays.asList(SUBMIT_SELECT_REVIEWER);
    }

    @Override
    protected List<ActionAdvancedInfo> getAdvancedInfo() {
        List<ActionAdvancedInfo> advancedInfo = new ArrayList<>();
        SelectReviewerActionAdvancedInfo selectReviewerActionAdvancedInfo = new SelectReviewerActionAdvancedInfo();
        if (getGroup() != null) {
            selectReviewerActionAdvancedInfo.setGroup(getGroup().getID().toString());
        }
        selectReviewerActionAdvancedInfo.setType(SUBMIT_SELECT_REVIEWER);
        selectReviewerActionAdvancedInfo.setId(SUBMIT_SELECT_REVIEWER);
        advancedInfo.add(selectReviewerActionAdvancedInfo);
        return advancedInfo;
    }

    public Role getRole() {
        return role;
    }

    @Autowired(required = true)
    public void setRole(Role role) {
        this.role = role;
    }

    /**
     * Get the Reviewer group from the "action.selectrevieweraction.group" property in actions.cfg by its UUID or name
     * Returns null if no (valid) group configured
     * @return configured reviewers Group from property or null if none
     */
    private Group getGroup() {
        Context context = new Context();
        String groupIdOrName = configurationService.getProperty("action.selectrevieweraction.group");
        Group group = null;

        try {
            // try to get group by name
            group = groupService.findByName(context, groupIdOrName);
            if (group == null) {
                // try to get group by uuid if not a name
                group = groupService.find(context, UUID.fromString(groupIdOrName));
            }
        } catch (Exception ignored) {
            // ignore, there is no reviewer group set
        }

        return group;
    }
}
