/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xmlworkflow.state.actions.processingaction;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Metadatum;
import org.dspace.content.Item;
import org.dspace.content.MetadataSchema;
import org.dspace.core.Context;
import org.dspace.xmlworkflow.WorkflowException;
import org.dspace.xmlworkflow.WorkflowRequirementsManager;
import org.dspace.xmlworkflow.XmlWorkflowManager;
import org.dspace.xmlworkflow.state.Step;
import org.dspace.xmlworkflow.state.actions.ActionResult;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.sql.SQLException;

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
        Metadatum[] scores = wfi.getItem().getMetadata(WorkflowRequirementsManager.WORKFLOW_SCHEMA, "score", null, Item.ANY);
        if(0 < scores.length){
            int totalScoreCount = 0;
            for (Metadatum score : scores) {
                totalScoreCount += Integer.parseInt(score.value);
            }
            int scoreMean = totalScoreCount / scores.length;
            //We have passed if we have at least gained our minimum score
            hasPassed = getMinimumAcceptanceScore() <= scoreMean;
            //Wether or not we have passed, clear our score information
            wfi.getItem().clearMetadata(WorkflowRequirementsManager.WORKFLOW_SCHEMA, "score", null, Item.ANY);

            String provDescription = getProvenanceStartId() + " Approved for entry into archive with a score of: " + scoreMean;
            wfi.getItem().addMetadata(MetadataSchema.DC_SCHEMA, "description", "provenance", "en", provDescription);
            wfi.getItem().update();
        }
        if(hasPassed){
            return new ActionResult(ActionResult.TYPE.TYPE_OUTCOME, ActionResult.OUTCOME_COMPLETE);
        }else{
            //We haven't passed, reject our item
            XmlWorkflowManager.sendWorkflowItemBackSubmission(c, wfi, c.getCurrentUser(), this.getProvenanceStartId(), "The item was reject due to a bad review score.");
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
