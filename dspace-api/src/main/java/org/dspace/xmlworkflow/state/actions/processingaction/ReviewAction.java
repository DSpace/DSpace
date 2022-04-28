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

import org.dspace.app.util.Util;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DCDate;
import org.dspace.content.MetadataSchemaEnum;
import org.dspace.core.Context;
import org.dspace.xmlworkflow.factory.XmlWorkflowServiceFactory;
import org.dspace.xmlworkflow.state.Step;
import org.dspace.xmlworkflow.state.actions.ActionResult;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;

/**
 * Processing class of an accept/reject action
 *
 * @author Bram De Schouwer (bram.deschouwer at dot com)
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 */
public class ReviewAction extends ProcessingAction {

    public static final int MAIN_PAGE = 0;
    public static final int REJECT_PAGE = 1;

    private static final String SUBMIT_APPROVE = "submit_approve";
    private static final String SUBMIT_REJECT = "submit_reject";
    private static final String SUBMITTER_IS_DELETED_PAGE = "submitter_deleted";


    @Override
    public void activate(Context c, XmlWorkflowItem wfItem) {

    }

    @Override
    public ActionResult execute(Context c, XmlWorkflowItem wfi, Step step, HttpServletRequest request)
        throws SQLException, AuthorizeException, IOException {
        if (super.isOptionInParam(request)) {
            switch (Util.getSubmitButton(request, SUBMIT_CANCEL)) {
                case SUBMIT_APPROVE:
                    return processAccept(c, wfi);
                case SUBMIT_REJECT:
                    return processRejectPage(c, wfi, step, request);
                case SUBMITTER_IS_DELETED_PAGE:
                    return processSubmitterIsDeletedPage(c, wfi, request);
                default:
                    return new ActionResult(ActionResult.TYPE.TYPE_CANCEL);
            }
        }
        return new ActionResult(ActionResult.TYPE.TYPE_CANCEL);
    }

    @Override
    public List<String> getOptions() {
        List<String> options = new ArrayList<>();
        options.add(SUBMIT_APPROVE);
        options.add(SUBMIT_REJECT);
        return options;
    }

    public ActionResult processAccept(Context c, XmlWorkflowItem wfi) throws SQLException, AuthorizeException {
        //Delete the tasks
        addApprovedProvenance(c, wfi);
        return new ActionResult(ActionResult.TYPE.TYPE_OUTCOME, ActionResult.OUTCOME_COMPLETE);
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
        itemService.addMetadata(c, wfi.getItem(), MetadataSchemaEnum.DC.getName(), "description", "provenance", "en",
            provDescription);
        itemService.update(c, wfi.getItem());
    }

    public ActionResult processRejectPage(Context c, XmlWorkflowItem wfi, Step step, HttpServletRequest request)
        throws SQLException, AuthorizeException, IOException {
        String reason = request.getParameter("reason");
        if (reason == null || 0 == reason.trim().length()) {
            request.setAttribute("page", REJECT_PAGE);
            addErrorField(request, "reason");
            return new ActionResult(ActionResult.TYPE.TYPE_ERROR);
        }

        //We have pressed reject, so remove the task the user has & put it back to a workspace item
        XmlWorkflowServiceFactory.getInstance().getXmlWorkflowService()
            .sendWorkflowItemBackSubmission(c, wfi, c.getCurrentUser(),
                this.getProvenanceStartId(), reason);


        return new ActionResult(ActionResult.TYPE.TYPE_SUBMISSION_PAGE);
    }

    public ActionResult processSubmitterIsDeletedPage(Context c, XmlWorkflowItem wfi, HttpServletRequest request)
            throws SQLException, AuthorizeException, IOException {
        if (request.getParameter("submit_delete") != null) {
            XmlWorkflowServiceFactory.getInstance().getXmlWorkflowService()
                                     .deleteWorkflowByWorkflowItem(c, wfi, c.getCurrentUser());
            // Delete and send user back to myDspace page
            return new ActionResult(ActionResult.TYPE.TYPE_SUBMISSION_PAGE);
        } else if (request.getParameter("submit_keep_it") != null) {
            // Do nothing, just send it back to myDspace page
            return new ActionResult(ActionResult.TYPE.TYPE_SUBMISSION_PAGE);
        } else {
            //Cancel, go back to the main task page
            request.setAttribute("page", MAIN_PAGE);
            return new ActionResult(ActionResult.TYPE.TYPE_PAGE);
        }
    }
}
