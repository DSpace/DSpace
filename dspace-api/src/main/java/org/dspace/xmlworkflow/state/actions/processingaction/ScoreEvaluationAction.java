/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xmlworkflow.state.actions.processingaction;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.MetadataValue;
import org.dspace.content.Item;
import org.dspace.content.MetadataSchema;
import org.dspace.core.Context;
import org.dspace.workflow.WorkflowException;
import org.dspace.xmlworkflow.factory.XmlWorkflowServiceFactory;
import org.dspace.xmlworkflow.service.WorkflowRequirementsService;
import org.dspace.xmlworkflow.state.Step;
import org.dspace.xmlworkflow.state.actions.ActionResult;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

/**
 * Processing class for the score evaluation action
 * This action will allow multiple users to rate a certain item
 * if the mean of this score is higher then the minimum score the
 * item will be sent to the next action/step else it will be rejected
 *
 * @author Bram De Schouwer (bram.deschouwer at dot com)
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 */
public class ScoreEvaluationAction extends ProcessingAction{

    private int minimumAcceptanceScore;

    @Override
    public void activate(Context c, XmlWorkflowItem wf) throws SQLException, IOException, AuthorizeException, WorkflowException {

    }

    @Override
    public ActionResult execute(Context c, XmlWorkflowItem wfi, Step step, HttpServletRequest request) throws SQLException, AuthorizeException, IOException, WorkflowException {
        boolean hasPassed = false;
        //Retrieve all our scores from the metadata & add em up
        List<MetadataValue> scores = itemService.getMetadata(wfi.getItem(), WorkflowRequirementsService.WORKFLOW_SCHEMA, "score", null, Item.ANY);
        if(0 < scores.size()){
            int totalScoreCount = 0;
            for (MetadataValue score : scores) {
                totalScoreCount += Integer.parseInt(score.getValue());
            }
            int scoreMean = totalScoreCount / scores.size();
            //We have passed if we have at least gained our minimum score
            hasPassed = getMinimumAcceptanceScore() <= scoreMean;
            //Wether or not we have passed, clear our score information
            itemService.clearMetadata(c, wfi.getItem(), WorkflowRequirementsService.WORKFLOW_SCHEMA, "score", null, Item.ANY);

            String provDescription = getProvenanceStartId() + " Approved for entry into archive with a score of: " + scoreMean;
            itemService.addMetadata(c, wfi.getItem(), MetadataSchema.DC_SCHEMA, "description", "provenance", "en", provDescription);
            itemService.update(c, wfi.getItem());
        }
        if(hasPassed){
            return new ActionResult(ActionResult.TYPE.TYPE_OUTCOME, ActionResult.OUTCOME_COMPLETE);
        }else{
            //We haven't passed, reject our item
            XmlWorkflowServiceFactory.getInstance().getXmlWorkflowService().sendWorkflowItemBackSubmission(c, wfi, c.getCurrentUser(), this.getProvenanceStartId(), "The item was reject due to a bad review score.");
            return new ActionResult(ActionResult.TYPE.TYPE_SUBMISSION_PAGE);
        }
    }

    public int getMinimumAcceptanceScore() {
        return minimumAcceptanceScore;
    }

    public void setMinimumAcceptanceScore(int minimumAcceptanceScore) {
        this.minimumAcceptanceScore = minimumAcceptanceScore;
    }
}
