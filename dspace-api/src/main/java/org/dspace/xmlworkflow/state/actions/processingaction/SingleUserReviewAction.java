/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xmlworkflow.state.actions.processingaction;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.util.Util;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.workflow.WorkflowException;
import org.dspace.xmlworkflow.factory.XmlWorkflowServiceFactory;
import org.dspace.xmlworkflow.state.Step;
import org.dspace.xmlworkflow.state.actions.ActionResult;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;

/**
 * Processing class of an action where a single user has
 * been assigned and they can either accept/reject the workflow item
 * or reject the task
 *
 * @author Bram De Schouwer (bram.deschouwer at dot com)
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 */
public class SingleUserReviewAction extends ProcessingAction {
    private static final Logger log = LogManager.getLogger(SingleUserReviewAction.class);

    public static final int OUTCOME_REJECT = 1;

    protected static final String SUBMIT_DECLINE_TASK = "submit_decline_task";

    @Override
    public void activate(Context c, XmlWorkflowItem wfItem) {
        // empty
    }

    @Override
    public ActionResult execute(Context c, XmlWorkflowItem wfi, Step step, HttpServletRequest request)
        throws SQLException, AuthorizeException, IOException, WorkflowException {
        if (!super.isOptionInParam(request)) {
            return new ActionResult(ActionResult.TYPE.TYPE_CANCEL);
        }
        switch (Util.getSubmitButton(request, SUBMIT_CANCEL)) {
            case SUBMIT_APPROVE:
                return processAccept(c, wfi);
            case SUBMIT_REJECT:
                return processReject(c, wfi, request);
            case SUBMIT_DECLINE_TASK:
                return processDecline(c, wfi);
            default:
                return new ActionResult(ActionResult.TYPE.TYPE_CANCEL);
        }
    }

    /**
     * Process {@link super#SUBMIT_REJECT} on this action, will either:
     * - If submitter of item no longer exists => Permanently delete corresponding item (no wfi/wsi remaining)
     * - Otherwise: reject item back to submission => becomes wsi of submitter again
     */
    private ActionResult processReject(Context c, XmlWorkflowItem wfi, HttpServletRequest request)
        throws SQLException, IOException, AuthorizeException {
        if (wfi.getSubmitter() == null) {
            // If the original submitter is no longer there, delete the task
            return processDelete(c, wfi);
        } else {
            return super.processRejectPage(c, wfi, request);
        }
    }

    /**
     * Accept the workflow item => last step in workflow so will be archived
     * Info on step & reviewer will be added on metadata dc.description.provenance of resulting item
     */
    public ActionResult processAccept(Context c, XmlWorkflowItem wfi) throws SQLException, AuthorizeException {
        super.addApprovedProvenance(c, wfi);
        return new ActionResult(ActionResult.TYPE.TYPE_OUTCOME, ActionResult.OUTCOME_COMPLETE);
    }

    @Override
    public List<String> getOptions() {
        List<String> options = new ArrayList<>();
        options.add(SUBMIT_APPROVE);
        options.add(SUBMIT_REJECT);
        options.add(SUBMIT_DECLINE_TASK);
        return options;
    }

    /**
     * Since original submitter no longer exists, workflow item is permanently deleted
     */
    private ActionResult processDelete(Context c, XmlWorkflowItem wfi)
        throws SQLException, AuthorizeException, IOException {
        EPerson user = c.getCurrentUser();
        c.turnOffAuthorisationSystem();
        WorkspaceItem workspaceItem = XmlWorkflowServiceFactory.getInstance().getXmlWorkflowService()
            .abort(c, wfi, user);
        ContentServiceFactory.getInstance().getWorkspaceItemService().deleteAll(c, workspaceItem);
        c.restoreAuthSystemState();
        return new ActionResult(ActionResult.TYPE.TYPE_SUBMISSION_PAGE);
    }

    /**
     * Selected reviewer declines to review task, then the workflow is aborted and restarted
     */
    private ActionResult processDecline(Context c, XmlWorkflowItem wfi)
        throws SQLException, IOException, AuthorizeException, WorkflowException {
        c.turnOffAuthorisationSystem();
        xmlWorkflowService.restartWorkflow(c, wfi, c.getCurrentUser(), this.getProvenanceStartId());
        c.restoreAuthSystemState();
        return new ActionResult(ActionResult.TYPE.TYPE_SUBMISSION_PAGE);
    }

}
