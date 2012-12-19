package org.dspace.workflow.actions.processingaction;

import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Redirector;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.environment.SourceResolver;
import org.dspace.app.util.Util;
import org.dspace.app.xmlui.utils.ContextUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.*;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.PluginManager;
import org.dspace.embargo.EmbargoLifter;
import org.dspace.handle.HandleManager;
import org.dspace.workflow.*;
import org.dspace.workflow.actions.ActionResult;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: bram
 * Date: 2-aug-2010
 * Time: 17:42:56
 * The last step in the dryad workflow contains edit/reject/accept actions which can be performed on the submission
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

    public ActionResult processMainPage(Context c, WorkflowItem wfi, Step step, HttpServletRequest request) throws SQLException, AuthorizeException, IOException {
        if(request.getParameter("submit_approve") != null){
            //Delete the tasks
            addApprovedProvenance(c, wfi);

            // in case the Curator approve the item in pendingPublicationStep
            // if(embargo is "untilArticleAppears"): Remove it!
            if(step.getId().equals("pendingPublicationStep")){
                Item[] dataFiles = DryadWorkflowUtils.getDataFiles(c, wfi.getItem());
                for(Item i : dataFiles){
                    DCValue[] values = i.getMetadata("dc.type.embargo");
                    if(values!=null && values.length > 0){
                        if(values[0].value.equals("untilArticleAppears")){
                            i.clearMetadata("dc", "type", "embargo", Item.ANY);
                            i.addMetadata("dc", "type", "embargo", "en", "none");
                        }
                    }
                    i.update();
                }

            }

            return new ActionResult(ActionResult.TYPE.TYPE_OUTCOME, ActionResult.OUTCOME_COMPLETE);
        } else if(request.getParameter("submit_reject") != null){
            // Make sure we indicate which page we want to process
            request.setAttribute("page", REJECT_PAGE);
            // We have pressed reject item, so take the user to a page where he can reject
            return new ActionResult(ActionResult.TYPE.TYPE_PAGE);
        }
        else if(request.getParameter("submit_remove") != null){
            removeSubmission(c, wfi, request);
            return new ActionResult(ActionResult.TYPE.TYPE_SUBMISSION_PAGE);

        }

        else {
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
            WorkflowManager.rejectWorkflowItem(c, wfi, c.getCurrentUser(), this, reason, true);
            //Also reject all the data files
            Item[] dataFiles = DryadWorkflowUtils.getDataFiles(c, wfi.getItem());
            for (Item dataFile : dataFiles) {
                try {
                    WorkflowManager.rejectWorkflowItem(c, WorkflowItem.findByItemId(c, dataFile.getID()), c.getCurrentUser(), this, reason, false);
                } catch (Exception e) {
                    throw new IOException(e);
                }
            }


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


    private void removeSubmission(Context c, WorkflowItem wfi, HttpServletRequest request) throws SQLException, AuthorizeException, IOException{
        // If they selected to remove the item then delete everything.
        Item[] dataFiles = DryadWorkflowUtils.getDataFiles(c, wfi.getItem());
        for (Item dataFile : dataFiles) {
            WorkflowItem datafileItem = WorkflowItem.findByItemId(c, dataFile.getID());
            //Found so delete it
            if(datafileItem!=null)
                datafileItem.deleteAll();
        }
        //Make sure we remove all child datasets
        removeDatasets(c, wfi.getItem());
        wfi.deleteAll();
    }

    private void removeDatasets(Context context, Item publication) throws SQLException, AuthorizeException, IOException {
        //If our publication doesn't have a handle this probably implies that we have no datasets so just return
        if(publication.getHandle() == null)
            return;

        String pubUrl = HandleManager.resolveToURL(context, publication.getHandle());

        //We have handle already so find & remove that one
        // We need to find an inprogressSubmission for this item so we can delete that one
        List<InProgressSubmission> allUserSubmissions = new ArrayList<InProgressSubmission>();
        Collections.addAll(allUserSubmissions, WorkflowItem.findByEPerson(context, context.getCurrentUser()));
        Collections.addAll(allUserSubmissions, WorkspaceItem.findByEPerson(context, context.getCurrentUser()));

        for (InProgressSubmission inProgressSubmission : allUserSubmissions){
            DCValue[] pubs = inProgressSubmission.getItem().getMetadata(MetadataSchema.DC_SCHEMA, "relation", "ispartof", Item.ANY);
            //Check if one of our values is the puburl, thus implying that this item is a dataset for this publication
            for (DCValue parentUrl : pubs) {
                if (parentUrl != null && parentUrl.value != null && parentUrl.value.equals(pubUrl)) {
                    //We have a match remove this item !
                    if (inProgressSubmission instanceof WorkspaceItem)
                        ((WorkspaceItem) inProgressSubmission).deleteAll();
                    else {
                        WorkflowItem workFlowItemToDel = (WorkflowItem) inProgressSubmission;
                        workFlowItemToDel.deleteAll();
                        //TODO: make sure that no email is sent out
                        WorkspaceItem wsi = WorkflowManager.rejectWorkflowItem(context, workFlowItemToDel, context.getCurrentUser(), null, "Deleted publication", false);
                        if(wsi!=null)
                            wsi.deleteAll();
                    }

                }
            }
        }
    }
}
