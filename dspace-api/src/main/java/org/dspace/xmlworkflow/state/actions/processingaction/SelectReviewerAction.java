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
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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

    private static final Logger log = LogManager.getLogger(SelectReviewerAction.class);

    private static final String SUBMIT_CANCEL = "submit_cancel";
    private static final String SUBMIT_SELECT_REVIEWER = "submit_select_reviewer";
    private static final String PARAM_REVIEWER = "eperson";

    private static final String CONFIG_REVIEWER_GROUP = "action.selectrevieweraction.group";

    private Role role;

    @Autowired(required = true)
    private EPersonService ePersonService;

    @Autowired(required = true)
    private WorkflowItemRoleService workflowItemRoleService;

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private GroupService groupService;

    private static Group selectFromReviewsGroup;
    private static boolean selectFromReviewsGroupInitialised = false;

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
            return processSelectReviewers(c, wfi, request);
        }

        //There are only 2 active buttons on this page, so if anything else happens just return an error
        return new ActionResult(ActionResult.TYPE.TYPE_ERROR);
    }

    /**
     * Method to handle the {@link this#SUBMIT_SELECT_REVIEWER} action:
     * - will retrieve the reviewer(s) uuid from request (param {@link this#PARAM_REVIEWER})
     * - assign them to a {@link WorkflowItemRole}
     * - In {@link org.dspace.xmlworkflow.state.actions.userassignment.AutoAssignAction} these reviewer(s) will get
     * claimed task for this {@link XmlWorkflowItem}
     * Will result in error if:
     * - No reviewer(s) uuid in request (param {@link this#PARAM_REVIEWER})
     * - If none of the reviewer(s) uuid passed along result in valid EPerson
     * - If the reviewer(s) passed along are not in {@link this#selectFromReviewsGroup} when it is set
     *
     * @param c       current DSpace session
     * @param wfi     the item on which the action is to be performed
     * @param request the current client request
     * @return the result of performing the action
     */
    private ActionResult processSelectReviewers(Context c, XmlWorkflowItem wfi, HttpServletRequest request)
        throws SQLException, AuthorizeException {
        //Retrieve the identifier of the eperson which will do the reviewing
        String[] reviewerIds = request.getParameterValues(PARAM_REVIEWER);
        if (ArrayUtils.isEmpty(reviewerIds)) {
            return new ActionResult(ActionResult.TYPE.TYPE_ERROR);
        }
        List<EPerson> reviewers = new ArrayList<>();
        for (String reviewerId : reviewerIds) {
            EPerson reviewer = ePersonService.find(c, UUID.fromString(reviewerId));
            if (reviewer == null) {
                log.warn("No EPerson found with uuid {}", reviewerId);
            } else {
                reviewers.add(reviewer);
            }
        }

        if (!this.checkReviewersValid(c, reviewers)) {
            return new ActionResult(ActionResult.TYPE.TYPE_ERROR);
        }

        createWorkflowItemRole(c, wfi, reviewers);
        return new ActionResult(ActionResult.TYPE.TYPE_OUTCOME, ActionResult.OUTCOME_COMPLETE);
    }

    private boolean checkReviewersValid(Context c, List<EPerson> reviewers) throws SQLException {
        if (reviewers.size() == 0) {
            return false;
        }
        Group group = this.getGroup(c);
        if (group != null) {
            for (EPerson reviewer: reviewers) {
                if (!groupService.isMember(c, reviewer, group)) {
                    log.error("Reviewers selected must be member of group {}", group.getID());
                    return false;
                }
            }
        }
        return true;
    }

    private WorkflowItemRole createWorkflowItemRole(Context c, XmlWorkflowItem wfi, List<EPerson> reviewers)
        throws SQLException, AuthorizeException {
        WorkflowItemRole workflowItemRole = workflowItemRoleService.create(c);
        workflowItemRole.setRoleId(getRole().getId());
        workflowItemRole.setWorkflowItem(wfi);
        if (reviewers.size() == 1) {
            // 1 reviewer in workflowitemrole => will be translated into a claimed task in the autoassign
            workflowItemRole.setEPerson(reviewers.get(0));
        } else {
            // multiple reviewers, create a temporary group and assign this group, the workflowitemrole will be
            // translated into a claimed task for reviewers in the autoassign, where group will be deleted
            c.turnOffAuthorisationSystem();
            Group selectedReviewsGroup = groupService.create(c);
            groupService.setName(selectedReviewsGroup, "selectedReviewsGroup_" + wfi.getID());
            for (EPerson reviewer : reviewers) {
                groupService.addMember(c, selectedReviewsGroup, reviewer);
            }
            workflowItemRole.setGroup(selectedReviewsGroup);
            c.restoreAuthSystemState();
        }
        workflowItemRoleService.update(c, workflowItemRole);
        return workflowItemRole;
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
        if (getGroup(null) != null) {
            selectReviewerActionAdvancedInfo.setGroup(getGroup(null).getID().toString());
        }
        selectReviewerActionAdvancedInfo.setType(SUBMIT_SELECT_REVIEWER);
        selectReviewerActionAdvancedInfo.generateId(SUBMIT_SELECT_REVIEWER);
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
     *
     * @return configured reviewers Group from property or null if none
     */
    private Group getGroup(@Nullable Context context) {
        if (selectFromReviewsGroupInitialised) {
            return this.selectFromReviewsGroup;
        }
        if (context == null) {
            context = new Context();
        }
        String groupIdOrName = configurationService.getProperty(CONFIG_REVIEWER_GROUP);

        if (StringUtils.isNotBlank(groupIdOrName)) {
            Group group = null;
            try {
                // try to get group by name
                group = groupService.findByName(context, groupIdOrName);
                if (group == null) {
                    // try to get group by uuid if not a name
                    group = groupService.find(context, UUID.fromString(groupIdOrName));
                }
            } catch (Exception e) {
                // There is an issue with the reviewer group that is set; if it is not set then can be chosen
                // from all epeople
                log.error("Issue with determining matching group for config {}={} for reviewer group of " +
                    "select reviewers workflow", CONFIG_REVIEWER_GROUP, groupIdOrName);
            }

            this.selectFromReviewsGroup = group;
        }
        selectFromReviewsGroupInitialised = true;
        return this.selectFromReviewsGroup;
    }

    /**
     * To be used by IT, e.g. {@code XmlWorkflowServiceIT}, when defining new 'Reviewers' group
     */
    static public void resetGroup() {
        selectFromReviewsGroup = null;
        selectFromReviewsGroupInitialised = false;
    }
}
