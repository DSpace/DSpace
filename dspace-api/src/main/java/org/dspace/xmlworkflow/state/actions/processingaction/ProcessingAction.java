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
import javax.servlet.http.HttpServletRequest;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.xmlworkflow.service.XmlWorkflowService;
import org.dspace.xmlworkflow.state.actions.Action;
import org.dspace.xmlworkflow.state.actions.ActionResult;
import org.dspace.xmlworkflow.storedcomponents.ClaimedTask;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;
import org.dspace.xmlworkflow.storedcomponents.service.ClaimedTaskService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Represent an action that can be offered to a workflow step's user(s).
 *
 * @author Bram De Schouwer (bram.deschouwer at dot com)
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 */
public abstract class ProcessingAction extends Action {

    @Autowired(required = true)
    protected ClaimedTaskService claimedTaskService;
    @Autowired(required = true)
    protected ItemService itemService;
    @Autowired
    protected XmlWorkflowService xmlWorkflowService;

    public static final String SUBMIT_EDIT_METADATA = "submit_edit_metadata";
    public static final String SUBMIT_CANCEL = "submit_cancel";
    protected static final String SUBMIT_APPROVE = "submit_approve";
    protected static final String SUBMIT_REJECT = "submit_reject";
    protected static final String RETURN_TO_POOL = "return_to_pool";
    protected static final String REJECT_REASON = "reason";

    @Override
    public boolean isAuthorized(Context context, HttpServletRequest request, XmlWorkflowItem wfi) throws SQLException {
        ClaimedTask task = null;
        if (context.getCurrentUser() != null) {
            task = claimedTaskService.findByWorkflowIdAndEPerson(context, wfi, context.getCurrentUser());
        }
        //Check if we have claimed the current task
        return task != null &&
            task.getWorkflowID().equals(getParent().getStep().getWorkflow().getID()) &&
            task.getStepID().equals(getParent().getStep().getId()) &&
            task.getActionID().equals(getParent().getId());
    }

    /**
     * Process result when option {@link this#SUBMIT_REJECT} is selected.
     * - Sets the reason and workflow step responsible on item in dc.description.provenance
     * - Send workflow back to the submission
     * If reason is not given => error
     */
    public ActionResult processRejectPage(Context c, XmlWorkflowItem wfi, HttpServletRequest request)
        throws SQLException, AuthorizeException, IOException {
        String reason = request.getParameter(REJECT_REASON);
        if (reason == null || 0 == reason.trim().length()) {
            addErrorField(request, REJECT_REASON);
            return new ActionResult(ActionResult.TYPE.TYPE_ERROR);
        }

        // We have pressed reject, so remove the task the user has & put it back
        // to a workspace item
        xmlWorkflowService.sendWorkflowItemBackSubmission(c, wfi, c.getCurrentUser(), this.getProvenanceStartId(),
            reason);

        return new ActionResult(ActionResult.TYPE.TYPE_SUBMISSION_PAGE);
    }

    @Override
    protected boolean isAdvanced() {
        return !getAdvancedOptions().isEmpty();
    }
}
