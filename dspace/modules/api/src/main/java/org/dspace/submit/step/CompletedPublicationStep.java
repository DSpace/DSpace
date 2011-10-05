package org.dspace.submit.step;

import org.dspace.content.*;
import org.dspace.identifier.IdentifierService;
import org.dspace.submit.AbstractProcessingStep;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.app.util.SubmissionInfo;
import org.dspace.authorize.AuthorizeException;
import org.dspace.utils.DSpace;
import org.dspace.workflow.DryadWorkflowUtils;
import org.dspace.handle.HandleManager;
import org.apache.log4j.Logger;
import org.dspace.workflow.WorkflowRequirementsManager;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import java.io.IOException;
import java.sql.SQLException;

/**
 * User: @author kevinvandevelde (kevin at atmire.com)
 * Date: 1-feb-2010
 * Time: 13:57:23
 *
 * The last step in the submission process for a data package
 */
public class CompletedPublicationStep extends AbstractProcessingStep
{
    /** log4j logger */
    private static Logger log = Logger.getLogger(CompletedPublicationStep.class);


    public int doProcessing(Context context, HttpServletRequest request, HttpServletResponse response, SubmissionInfo subInfo) throws ServletException, IOException, SQLException, AuthorizeException {
        // The Submission is COMPLETE!!
        log.info(LogManager.getHeader(context, "submission_complete",
                "Completed data package with id="
                        + subInfo.getSubmissionItem().getID()));

        boolean success = false;
        String redirectUrl = null;
        try
        {
            //Clear any rejected metadata, since the item is beeing re submitted.
            subInfo.getSubmissionItem().getItem().clearMetadata(WorkflowRequirementsManager.WORKFLOW_SCHEMA, "step", "rejectDate", Item.ANY);
            subInfo.getSubmissionItem().getItem().update();
            if(subInfo.getSubmissionItem().getItem().getHandle() == null){
//                WorkflowItem workflowItem = WorkflowManager.start(context, (WorkspaceItem) subInfo
//                        .getSubmissionItem());
                Item publication = subInfo.getSubmissionItem().getItem();

                if(publication.getMetadata(MetadataSchema.DC_SCHEMA, "type", null, Item.ANY).length == 0)
                    publication.addMetadata(MetadataSchema.DC_SCHEMA, "type", null, Item.ANY, "Article");
                
                //Create a handle
                DSpace dspace = new DSpace();
                IdentifierService identifierService = dspace.getServiceManager().getServiceByName(IdentifierService.class.getName(), IdentifierService.class);
                identifierService.reserve(context, publication);


                //Time to get our workflowItem
                DCValue[] dcValues = publication.getMetadata(MetadataSchema.DC_SCHEMA, "relation", "haspart", Item.ANY);

                if(0 < dcValues.length){
                    // Find our workspaceitem
                    WorkspaceItem wsi = WorkspaceItem.find(context, Integer.parseInt(dcValues[0].value));
                    //Make sure that our workspaceitem is aware that it is part of the publication
                    wsi.getItem().clearMetadata(MetadataSchema.DC_SCHEMA, "relation", "ispartof", Item.ANY);
                    DCValue[] doiVals = wsi.getItem().getMetadata(MetadataSchema.DC_SCHEMA, "identifier", null, Item.ANY);
                    if(0 < doiVals.length)
                        wsi.getItem().addMetadata(MetadataSchema.DC_SCHEMA, "relation", "ispartof", null, doiVals[0].value);
                    else
                        wsi.getItem().addMetadata(MetadataSchema.DC_SCHEMA, "relation", "ispartof", null, HandleManager.resolveToURL(context, wsi.getItem().getHandle()));

                    inheritMetadata(publication, wsi.getItem());
                    wsi.getItem().update();

                    redirectUrl = request.getContextPath() + "/submit?workspaceID=" + dcValues[0].value + "&skipOverview=true";

                    //Now make sure we remove the id of the dataset
                    publication.clearMetadata(MetadataSchema.DC_SCHEMA, "relation", "haspart", Item.ANY);

                    //Also make sure that we add a boolean indicating that this item has been submitted
                    publication.addMetadata("internal", "workflow", "submitted", null, Boolean.TRUE.toString());
                    publication.update();
                }else{
                    //We don't have a dataset, so create one
                    try{
                        redirectUrl = DryadWorkflowUtils.createDataset(context, request, publication, false);
                        //Also make sure that we add a boolean indicating that this item has been submitted
                        publication.addMetadata("internal", "workflow", "submitted", null, Boolean.TRUE.toString());
                        publication.update();
                        success = true;
                    } catch (Exception e){
                        throw new ServletException(e);
                    } finally {
                        // commit changes to database
                        if(success)
                            context.commit();
                        else
                            context.getDBConnection().rollback();
                    }
                }
            }else{
                if(subInfo.getSubmissionItem() instanceof WorkspaceItem){
                    //We only come here when re-submitting so euhm add it to the metadata
                    Item publication = subInfo.getSubmissionItem().getItem();
                    if(publication.getMetadata("internal", "workflow", "submitted", Item.ANY).length == 0){
                        //A newly (re-)submitted publication
                        publication.addMetadata("internal", "workflow", "submitted", null, Boolean.TRUE.toString());
                        subInfo.getSubmissionItem().getItem().update();
                    }
                    //Go to the overview
                    redirectUrl = request.getContextPath() + "/submit-overview?workspaceID=" + subInfo.getSubmissionItem().getID();
                }else{
                    //Go to the overview
                    redirectUrl = request.getContextPath() + "/submit-overview?workflowID=" + subInfo.getSubmissionItem().getID();
                }
            }
            success = true;

        }
        catch (Exception e)
        {
            log.error("Caught exception in submission step: ",e);
            throw new ServletException(e);
        }
        finally
        {
        // commit changes to database
            if (success)
        context.commit();
            else
                context.getDBConnection().rollback();
        }
        if(redirectUrl != null){
            response.sendRedirect(redirectUrl);
        }



        return STATUS_COMPLETE;
    }

    public int getNumberOfPages(HttpServletRequest request,
            SubmissionInfo subInfo) throws ServletException
    {
        // This class represents the non-interactive processing step
        // that occurs just *before* the final confirmation page!
        // (so it should only be processed once!)
        return 1;
    }
}
