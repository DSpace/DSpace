package org.dspace.workflow.actions.userassignment;

import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.workflow.*;
import org.dspace.workflow.actions.ActionResult;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

/**
 * User: bram
 * Date: 17-aug-2010
 * Time: 10:30:53
 *
 * An action that will assign read,write rights to the submitter and grant the curators read rights
 */
public class AssignOriginalSubmitterAction extends UserSelectionAction{

    @Override
    public boolean isFinished(WorkflowItem wfi) {
        return false;
    }

    @Override
    public void regenerateTasks(Context c, WorkflowItem wfi, List<Integer> userToIgnore) throws SQLException {

    }

    @Override
    public boolean isValidUserSelection(Context context, WorkflowItem wfi, boolean hasUI) throws WorkflowConfigurationException, SQLException {
        return wfi.getSubmitter() != null;
    }

    @Override
    public void activate(Context c, WorkflowItem wf) throws SQLException, IOException {

    }

    @Override
    public ActionResult execute(Context c, WorkflowItem wfi, Step step, HttpServletRequest request) throws SQLException, AuthorizeException, IOException, WorkflowException {
        EPerson submitter = wfi.getSubmitter();
        Step currentStep = getParent().getStep();
        WorkflowRequirementsManager.addClaimedUser(c, wfi, currentStep, submitter);

        //Grant the curator read rights
        DryadWorkflowUtils.grantCuratorReadRightsOnItem(c, wfi, this);


        try {
            //TODO: stay with processoutcome ?
            WorkflowManager.processOutcome(c, submitter, currentStep.getWorkflow(), currentStep, this.getParent(), new ActionResult(ActionResult.TYPE.TYPE_OUTCOME, ActionResult.OUTCOME_COMPLETE), wfi);
        } catch (Exception e) {
            throw new WorkflowException("There was an error processing the workflow");

        }
        //It is important that we return to the submission page since we will continue our actions with the submitter
        return new ActionResult(ActionResult.TYPE.TYPE_SUBMISSION_PAGE);
    }
}
