/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xmlworkflow.state.actions.processingaction;

import org.dspace.app.util.Util;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.DCDate;
import org.dspace.content.MetadataSchema;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.xmlworkflow.factory.XmlWorkflowServiceFactory;
import org.dspace.xmlworkflow.service.WorkflowRequirementsService;
import org.dspace.xmlworkflow.service.XmlWorkflowService;
import org.dspace.xmlworkflow.state.Step;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;
import org.dspace.xmlworkflow.storedcomponents.service.WorkflowItemRoleService;
import org.dspace.xmlworkflow.storedcomponents.service.XmlWorkflowItemService;
import org.dspace.xmlworkflow.state.actions.ActionResult;
import org.dspace.content.Item;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.sql.SQLException;

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
    public static final int DELETE_PAGE = 2;

    //TODO: rename to AcceptAndEditMetadataAction

    @Override
    public void activate(Context c, XmlWorkflowItem wf) throws SQLException {

    }

    @Override
    public ActionResult execute(Context c, XmlWorkflowItem wfi, Step step, HttpServletRequest request) throws SQLException, AuthorizeException, IOException {
        int page = Util.getIntParameter(request, "page");

        switch (page){
            case MAIN_PAGE:
                return processMainPage(c, wfi, step, request);
            case REJECT_PAGE:
                return processRejectPage(c, wfi, step, request);
            case DELETE_PAGE:
                return processDeleteItem(c, wfi, step, request);

        }

        return new ActionResult(ActionResult.TYPE.TYPE_CANCEL);
    }

    public ActionResult processMainPage(Context c, XmlWorkflowItem wfi, Step step, HttpServletRequest request) throws SQLException, AuthorizeException {
        if(request.getParameter("submit_approve") != null){
            //Delete the tasks
            addApprovedProvenance(c, wfi);

            return new ActionResult(ActionResult.TYPE.TYPE_OUTCOME, ActionResult.OUTCOME_COMPLETE);
        } else if(request.getParameter("submit_reject") != null){
            // Make sure we indicate which page we want to process
            request.setAttribute("page", REJECT_PAGE);
            // We have pressed reject item, so take the user to a page where he can reject
            return new ActionResult(ActionResult.TYPE.TYPE_PAGE);
        } else if(request.getParameter("submit_delete") != null){
        	request.setAttribute("page", DELETE_PAGE);
            // We have pressed delete item
            return new ActionResult(ActionResult.TYPE.TYPE_PAGE);
        } else {
            //We pressed the leave button so return to our submissions page
            return new ActionResult(ActionResult.TYPE.TYPE_SUBMISSION_PAGE);
        }
    }

    public ActionResult processRejectPage(Context c, XmlWorkflowItem wfi, Step step, HttpServletRequest request) throws SQLException, AuthorizeException, IOException {
        if(request.getParameter("submit_reject") != null){
            String reason = request.getParameter("reason");
            if(reason == null || 0 == reason.trim().length()){
                addErrorField(request, "reason");
                request.setAttribute("page", REJECT_PAGE);
                return new ActionResult(ActionResult.TYPE.TYPE_ERROR);
            }

            //We have pressed reject, so remove the task the user has & put it back to a workspace item
            XmlWorkflowServiceFactory.getInstance().getXmlWorkflowService().sendWorkflowItemBackSubmission(c, wfi, c.getCurrentUser(), this.getProvenanceStartId(), reason);


            return new ActionResult(ActionResult.TYPE.TYPE_SUBMISSION_PAGE);
        }else{
            //Cancel, go back to the main task page
            request.setAttribute("page", MAIN_PAGE);

            return new ActionResult(ActionResult.TYPE.TYPE_PAGE);
        }
    }

    private void addApprovedProvenance(Context c, XmlWorkflowItem wfi) throws SQLException, AuthorizeException {
        //Add the provenance for the accept
        String now = DCDate.getCurrent().toString();

        // Get user's name + email address
        String usersName = XmlWorkflowServiceFactory.getInstance().getXmlWorkflowService().getEPersonName(c.getCurrentUser());

        String provDescription = getProvenanceStartId() + " Approved for entry into archive by "
                + usersName + " on " + now + " (GMT) ";

        // Add to item as a DC field
        itemService.addMetadata(c, wfi.getItem(), MetadataSchema.DC_SCHEMA, "description", "provenance", "en", provDescription);
        itemService.update(c, wfi.getItem());
    }
    
    public ActionResult processDeleteItem(Context c, XmlWorkflowItem wfi, Step step, HttpServletRequest request) throws SQLException, AuthorizeException, IOException {
    	if(request.getParameter("submit_delete") != null){
            AuthorizeService authorizeService= AuthorizeServiceFactory.getInstance().getAuthorizeService();
            XmlWorkflowService xmlWorkflowService = XmlWorkflowServiceFactory.getInstance().getXmlWorkflowService();
            WorkflowRequirementsService workflowRequirementsService=XmlWorkflowServiceFactory.getInstance().getWorkflowRequirementsService();
            WorkflowItemRoleService workflowItemRoleService=XmlWorkflowServiceFactory.getInstance().getWorkflowItemRoleService();
            XmlWorkflowItemService xmlWorkflowItemService=XmlWorkflowServiceFactory.getInstance().getXmlWorkflowItemService();
            ItemService itemService= ContentServiceFactory.getInstance().getItemService();

            Item item =wfi.getItem();
            //Check for permission
            authorizeService.authorizeAction(c, item, Constants.DELETE);

            //Remove references from 'cwf_in_progress_user' and 'cwf_claimtask' tables
            workflowRequirementsService.clearInProgressUsers(c, wfi);
            xmlWorkflowService.deleteAllTasks(c,wfi);

            //Remove (if any) the workflowItemroles for this item
            workflowItemRoleService.deleteForWorkflowItem(c, wfi);

            //Remove the workflowItem
            xmlWorkflowItemService.deleteWrapper(c, wfi);

            //Shut down authorization system
            c.turnOffAuthorisationSystem();
            //Remove item
            itemService.delete(c,item);
            c.restoreAuthSystemState();

            c.commit();
            return new ActionResult(ActionResult.TYPE.TYPE_SUBMISSION_PAGE);
        }

        //Cancel, go back to the main task page
        request.setAttribute("page", MAIN_PAGE);
        return new ActionResult(ActionResult.TYPE.TYPE_PAGE);
    }
	
}
