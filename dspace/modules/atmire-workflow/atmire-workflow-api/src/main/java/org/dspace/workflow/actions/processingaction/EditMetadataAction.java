package org.dspace.workflow.actions.processingaction;

import org.dspace.app.util.Util;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DCDate;
import org.dspace.content.MetadataSchema;
import org.dspace.core.Context;
import org.dspace.workflow.ClaimedTask;
import org.dspace.workflow.Step;
import org.dspace.workflow.WorkflowItem;
import org.dspace.workflow.WorkflowManager;
import org.dspace.workflow.actions.ActionResult;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.sql.SQLException;

/**
 * Created by IntelliJ IDEA.
 * User: bram
 * Date: 2-aug-2010
 * Time: 17:42:56
 * To change this template use File | Settings | File Templates.
 */
public class EditMetadataAction extends ProcessingAction {

    public static final int MAIN_PAGE = 0;
    public static final int REJECT_PAGE = 1;

    //TODO: rename to AcceptAndEditMetadataAction
    
    @Override
    public void activate(Context c, WorkflowItem wf) throws SQLException {

    }

    @Override
    public ActionResult execute(Context c, WorkflowItem wfi, Step step, HttpServletRequest request) throws SQLException, AuthorizeException, IOException {
        int page = Util.getIntParameter(request, "page");

        switch (page){
            case MAIN_PAGE:
                return processMainPage(c, wfi, step, request);
            case REJECT_PAGE:
                return processRejectPage(c, wfi, step, request);

        }

        return new ActionResult(ActionResult.TYPE.TYPE_CANCEL);
    }

    public ActionResult processMainPage(Context c, WorkflowItem wfi, Step step, HttpServletRequest request) throws SQLException, AuthorizeException {
        if(request.getParameter("submit_approve") != null){
            //Delete the tasks
            addApprovedProvenance(c, wfi);

            return new ActionResult(ActionResult.TYPE.TYPE_OUTCOME, ActionResult.OUTCOME_COMPLETE);
        } else if(request.getParameter("submit_reject") != null){
            // Make sure we indicate which page we want to process
            request.setAttribute("page", REJECT_PAGE);
            // We have pressed reject item, so take the user to a page where he can reject
            return new ActionResult(ActionResult.TYPE.TYPE_PAGE);
        } else {
            //We pressed the leave button so return to our submissions page
            return new ActionResult(ActionResult.TYPE.TYPE_SUBMISSION_PAGE);
        }
    }

    public ActionResult processRejectPage(Context c, WorkflowItem wfi, Step step, HttpServletRequest request) throws SQLException, AuthorizeException, IOException {
        if(request.getParameter("submit_reject") != null){
            String reason = request.getParameter("reason");
            if(reason == null || 0 == reason.trim().length()){
                addErrorField(request, "reason");
                request.setAttribute("page", REJECT_PAGE);
                return new ActionResult(ActionResult.TYPE.TYPE_ERROR);
            }

            //We have pressed reject, so remove the task the user has & put it back to a workspace item
            WorkflowManager.rejectWorkflowItem(c, wfi, c.getCurrentUser(), this, reason);


            return new ActionResult(ActionResult.TYPE.TYPE_SUBMISSION_PAGE);
        }else{
            //Cancel, go back to the main task page
            request.setAttribute("page", MAIN_PAGE);

            return new ActionResult(ActionResult.TYPE.TYPE_PAGE);
        }
    }

    private void addApprovedProvenance(Context c, WorkflowItem wfi) throws SQLException, AuthorizeException {
        //Add the provenance for the accept
        String now = DCDate.getCurrent().toString();

        // Get user's name + email address
        String usersName = WorkflowManager.getEPersonName(c.getCurrentUser());

        String provDescription = getProvenanceStartId() + " Approved for entry into archive by "
                + usersName + " on " + now + " (GMT) ";

        // Add to item as a DC field
        wfi.getItem().addMetadata(MetadataSchema.DC_SCHEMA, "description", "provenance", "en", provDescription);
        wfi.getItem().update();
    }
}
