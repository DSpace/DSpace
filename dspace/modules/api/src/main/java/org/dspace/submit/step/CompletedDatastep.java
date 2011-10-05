package org.dspace.submit.step;

import org.dspace.app.util.SubmissionInfo;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.*;
import org.dspace.core.Context;
import org.dspace.handle.HandleManager;
import org.dspace.identifier.IdentifierException;
import org.dspace.identifier.IdentifierService;
import org.dspace.submit.AbstractProcessingStep;
import org.dspace.utils.DSpace;
import org.dspace.workflow.*;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;

/**
 * User: kevin (kevin at atmire.com)
 * Date: 13-apr-2010
 * Time: 18:01:18
 *
 * The last step in the submission process for a data file
 */
public class CompletedDatastep extends AbstractProcessingStep {

    @Override
    public int doProcessing(Context context, HttpServletRequest request, HttpServletResponse response, SubmissionInfo subInfo) throws ServletException, IOException, SQLException, AuthorizeException {
        Item completedDataset = subInfo.getSubmissionItem().getItem();
        Item publication = DryadWorkflowUtils.getDataPackage(context, subInfo.getSubmissionItem().getItem());


        if(completedDataset.getHandle() == null){
            //Since we are done we need to create a handle for this
            //Create a handle & make sure our publication is aware of this (if it is still in the workflow)!
            IdentifierService service = new DSpace().getSingletonService(IdentifierService.class);
            try {
                service.reserve(context, completedDataset);
            } catch (IdentifierException e) {
                throw new ServletException(e);
            }
            if(!publication.isArchived()){
                String id;
                DCValue[] doiIdentifiers = completedDataset.getMetadata(MetadataSchema.DC_SCHEMA, "identifier", null, Item.ANY);
                if(0 < doiIdentifiers.length)
                    id = doiIdentifiers[0].value;
                else
                    id = HandleManager.resolveToURL(context, completedDataset.getHandle());

                publication.addMetadata(MetadataSchema.DC_SCHEMA, "relation", "haspart", null, id);
                publication.update();
            }
        }
        String redirectUrl = null;
        DCValue[] redirToWorkflowVals = completedDataset.getMetadata(WorkflowRequirementsManager.WORKFLOW_SCHEMA, "submit", "toworkflow", Item.ANY);
        if(0 < redirToWorkflowVals.length && Boolean.valueOf(redirToWorkflowVals[0].value)){
            //Clear the metadata from the workflow
            completedDataset.clearMetadata(WorkflowRequirementsManager.WORKFLOW_SCHEMA, "submit", "toworkflow", Item.ANY);

            //Send our item to the workflow
            try {
                WorkflowItem workflowItem = WorkflowManager.start(context, (WorkspaceItem) subInfo.getSubmissionItem());

                WorkflowItem datasetWf = WorkflowItem.findByItemId(context, publication.getID());
                ClaimedTask task = ClaimedTask.findByWorkflowIdAndEPerson(context, datasetWf.getID(), context.getCurrentUser().getID());

                //Redir us to our overview
                redirectUrl = request.getContextPath() + "/handle/" + workflowItem.getCollection().getHandle() + "/workflow?workflowID=" + datasetWf.getID() + "&stepID=" + task.getStepID() + "&actionID=" + task.getActionID();
            } catch (Exception e) {
                throw new ServletException(e);
            }

        } else {
            if(subInfo.getSubmissionItem() instanceof WorkspaceItem){
                //We have a workspace item
                //Don't forget to set submitted to true for the item that is just through the workflow !
                if(0 == completedDataset.getMetadata("internal", "workflow", "submitted", Item.ANY).length)
                    completedDataset.addMetadata("internal", "workflow", "submitted", null, Boolean.TRUE.toString());
                redirectUrl = request.getContextPath() + "/submit-overview?workspaceID=" + subInfo.getSubmissionItem().getID();
            }else{
                redirectUrl = request.getContextPath() + "/submit-overview?workflowID=" + subInfo.getSubmissionItem().getID();
            }
        }

        if(completedDataset.getMetadata(MetadataSchema.DC_SCHEMA, "type", null, Item.ANY).length == 0)
            completedDataset.addMetadata(MetadataSchema.DC_SCHEMA, "type", null, Item.ANY, "Dataset");
        
        completedDataset.update();

        //Commit our changes before redirect
        context.commit();

        //Redirect us to an overview page of our completed dataset !
        response.sendRedirect(redirectUrl);

        //Find out if we need to add another dataset
        return STATUS_COMPLETE;
    }

    @Override
    public int getNumberOfPages(HttpServletRequest request, SubmissionInfo subInfo) throws ServletException {
        return 1;
    }

    @Override
    public boolean isStepShownInProgressBar() {
        return false;
    }

//    @Override
//    public boolean isStepAccessible(Context context, Item item) {
//        Only show this step if we are submitting a new item, if our item already has a handle there is no need to show this page
//        return item.getHandle() == null;
//
//    }
}
