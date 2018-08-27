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
import org.dspace.content.DCDate;
import org.dspace.content.MetadataSchema;
import org.dspace.core.Context;
import org.dspace.xmlworkflow.factory.XmlWorkflowServiceFactory;
import org.dspace.xmlworkflow.state.Step;
import org.dspace.xmlworkflow.state.actions.ActionResult;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;

/**
 * Processing class of an action that allows users to
 * edit/accept/reject a workflow item
 *
 * @author Bram De Schouwer (bram.deschouwer at dot com)
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 */
public class AcceptEditRejectAction extends ProcessingAction {

    public static final int MAIN_PAGE = 0;
    public static final int REJECT_PAGE = 1;

    //TODO: rename to AcceptAndEditMetadataAction

    @Override
    public void activate(Context c, XmlWorkflowItem wf) throws SQLException {

    }

    @Override
    public ActionResult execute(Context c, XmlWorkflowItem wfi, Step step, HttpServletRequest request)
        throws SQLException, AuthorizeException, IOException {

        if (request.getParameter("submit_approve") != null) {
            return processMainPage(c, wfi, step, request);
        } else {
            if (request.getParameter("submit_reject") != null) {
                return processRejectPage(c, wfi, step, request);
            }
        }
        return new ActionResult(ActionResult.TYPE.TYPE_CANCEL);
    }

    public ActionResult processMainPage(Context c, XmlWorkflowItem wfi, Step step, HttpServletRequest request)
        throws SQLException, AuthorizeException {
        //Delete the tasks
        addApprovedProvenance(c, wfi);

        return new ActionResult(ActionResult.TYPE.TYPE_OUTCOME, ActionResult.OUTCOME_COMPLETE);
    }

    public ActionResult processRejectPage(Context c, XmlWorkflowItem wfi, Step step, HttpServletRequest request)
        throws SQLException, AuthorizeException, IOException {
        String reason = request.getParameter("reason");
        if (reason == null || 0 == reason.trim().length()) {
            addErrorField(request, "reason");
            return new ActionResult(ActionResult.TYPE.TYPE_ERROR);
        }

        // We have pressed reject, so remove the task the user has & put it back
        // to a workspace item
        XmlWorkflowServiceFactory.getInstance().getXmlWorkflowService().sendWorkflowItemBackSubmission(c, wfi,
                c.getCurrentUser(), this.getProvenanceStartId(), reason);

        return new ActionResult(ActionResult.TYPE.TYPE_SUBMISSION_PAGE);
    }

    private void addApprovedProvenance(Context c, XmlWorkflowItem wfi) throws SQLException, AuthorizeException {
        //Add the provenance for the accept
        String now = DCDate.getCurrent().toString();

        // Get user's name + email address
        String usersName = XmlWorkflowServiceFactory.getInstance().getXmlWorkflowService()
                                                    .getEPersonName(c.getCurrentUser());

        String provDescription = getProvenanceStartId() + " Approved for entry into archive by "
            + usersName + " on " + now + " (GMT) ";

        // Add to item as a DC field
        itemService.addMetadata(c, wfi.getItem(), MetadataSchema.DC_SCHEMA, "description", "provenance", "en",
                                provDescription);
        itemService.update(c, wfi.getItem());
    }
}
