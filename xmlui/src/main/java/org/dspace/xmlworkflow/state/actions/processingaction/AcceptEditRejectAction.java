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

import org.apache.cocoon.Constants;
import org.dspace.app.util.Util;
import org.dspace.app.xmlui.aspect.administrative.FlowItemUtils;
import org.dspace.app.xmlui.aspect.administrative.FlowResult;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.DCDate;
import org.dspace.content.Item;
import org.dspace.content.MetadataSchema;
import org.dspace.core.Context;
import org.dspace.xmlworkflow.XmlWorkflowManager;
import org.dspace.xmlworkflow.state.Step;
import org.dspace.xmlworkflow.state.actions.ActionResult;
import org.dspace.xmlworkflow.state.actions.processingaction.ProcessingAction;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;

import ar.edu.unlp.sedici.util.FlashMessagesUtil;

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
                return processDeletePage(c, wfi, step, request);

        }

        return new ActionResult(ActionResult.TYPE.TYPE_CANCEL);
    }

    public ActionResult processMainPage(Context c, XmlWorkflowItem wfi, Step step, HttpServletRequest request) throws SQLException, AuthorizeException {
        if(request.getParameter("submit_approve") != null){
            //Delete the tTYPE_PAGEasks
            addApprovedProvenance(c, wfi);
            
            // Delete the workflow_eidted flag
            XmlWorkflowManager.cleanWorkflowEdited(c, wfi.getItem() );
            
            return new ActionResult(ActionResult.TYPE.TYPE_OUTCOME, ActionResult.OUTCOME_COMPLETE);
        } else if(request.getParameter("submit_reject") != null){
            // Make sure we indicate which page we want to process
            request.setAttribute("page", REJECT_PAGE);
            // We have pressed reject item, so take the user to a page where he can reject
            return new ActionResult(ActionResult.TYPE.TYPE_PAGE);
        } else if(request.getParameter("submit_delete") != null){
            // Make sure we indicate which page we want to process
            request.setAttribute("page", DELETE_PAGE);
            // We have pressed reject item, so take the user to a page where he can reject
            return new ActionResult(ActionResult.TYPE.TYPE_PAGE);
        }else {
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

            // Delete the workflow_eidted flag
            XmlWorkflowManager.cleanWorkflowEdited(c, wfi.getItem() );

            //We have pressed reject, so remove the task the user has & put it back to a workspace item
            XmlWorkflowManager.sendWorkflowItemBackSubmission(c, wfi, c.getCurrentUser(), this.getProvenanceStartId(), reason);


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
        String usersName = XmlWorkflowManager.getEPersonName(c.getCurrentUser());

        String provDescription = getProvenanceStartId() + " Approved for entry into archive by "
                + usersName + " on " + now + " (GMT) ";

        // Add to item as a DC field
        wfi.getItem().addMetadata(MetadataSchema.DC_SCHEMA, "description", "provenance", "en", provDescription);
        wfi.getItem().update();
    }
    
    public ActionResult processDeletePage(Context c, XmlWorkflowItem wfi, Step step, HttpServletRequest request) throws SQLException, AuthorizeException, IOException {
    	if(request.getParameter("submit_delete") != null){
            if (AuthorizeManager.authorizeActionBoolean(c,wfi.getItem(),org.dspace.core.Constants.DELETE)){
            	FlowResult resultado = FlowItemUtils.processDeleteItem(c,wfi.getItem().getID());
            	if (resultado.getContinue()){
            		FlashMessagesUtil.setNoticeMessage(request.getSession(), "xmlui.flashMessage.deleteItem.success");
            		return new ActionResult(ActionResult.TYPE.TYPE_SUBMISSION_PAGE);
            	} else {
                	FlashMessagesUtil.setErrorMessage(request.getSession(), "xmlui.flashMessage.deleteItem.failure");
            	}
            } else {
            	FlashMessagesUtil.setErrorMessage(request.getSession(), "xmlui.flashMessage.deleteItem.failure");
            }
        }
        //Cancel, go back to the main task page
        request.setAttribute("page", MAIN_PAGE);

        return new ActionResult(ActionResult.TYPE.TYPE_PAGE);

    }
}
