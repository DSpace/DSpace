/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xmlworkflow.state.actions.processingaction;

import static org.dspace.xmlworkflow.state.actions.processingaction.ScoreReviewAction.REVIEW_FIELD;
import static org.dspace.xmlworkflow.state.actions.processingaction.ScoreReviewAction.SCORE_FIELD;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.content.MetadataSchemaEnum;
import org.dspace.content.MetadataValue;
import org.dspace.core.Context;
import org.dspace.xmlworkflow.factory.XmlWorkflowServiceFactory;
import org.dspace.xmlworkflow.state.Step;
import org.dspace.xmlworkflow.state.actions.ActionResult;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;

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
public class ScoreEvaluationAction extends ProcessingAction {

    // Minimum aggregate of scores
    private int minimumAcceptanceScore;

    @Override
    public void activate(Context c, XmlWorkflowItem wf) {

    }

    @Override
    public ActionResult execute(Context c, XmlWorkflowItem wfi, Step step, HttpServletRequest request)
        throws SQLException, AuthorizeException, IOException {
        // Retrieve all our scores from the metadata & add em up
        int scoreMean = getMeanScore(wfi);
        //We have passed if we have at least gained our minimum score
        boolean hasPassed = getMinimumAcceptanceScore() <= scoreMean;
        //Whether or not we have passed, clear our score information
        itemService.clearMetadata(c, wfi.getItem(), SCORE_FIELD.schema, SCORE_FIELD.element, SCORE_FIELD.qualifier,
            Item.ANY);
        if (hasPassed) {
            this.addRatingInfoToProv(c, wfi, scoreMean);
            return new ActionResult(ActionResult.TYPE.TYPE_OUTCOME, ActionResult.OUTCOME_COMPLETE);
        } else {
            //We haven't passed, reject our item
            XmlWorkflowServiceFactory.getInstance().getXmlWorkflowService()
                .sendWorkflowItemBackSubmission(c, wfi, c.getCurrentUser(), this.getProvenanceStartId(),
                    "The item was reject due to a bad review score.");
            return new ActionResult(ActionResult.TYPE.TYPE_SUBMISSION_PAGE);
        }
    }

    private int getMeanScore(XmlWorkflowItem wfi) {
        List<MetadataValue> scores = itemService
            .getMetadata(wfi.getItem(), SCORE_FIELD.schema, SCORE_FIELD.element, SCORE_FIELD.qualifier, Item.ANY);
        int scoreMean = 0;
        if (0 < scores.size()) {
            int totalScoreCount = 0;
            for (MetadataValue score : scores) {
                totalScoreCount += Integer.parseInt(score.getValue());
            }
            scoreMean = totalScoreCount / scores.size();
        }
        return scoreMean;
    }

    private void addRatingInfoToProv(Context c, XmlWorkflowItem wfi, int scoreMean)
        throws SQLException, AuthorizeException {
        StringBuilder provDescription = new StringBuilder();
        provDescription.append(String.format("%s Approved for entry into archive with a score of: %s",
            getProvenanceStartId(), scoreMean));
        List<MetadataValue> reviews = itemService
            .getMetadata(wfi.getItem(), REVIEW_FIELD.schema, REVIEW_FIELD.element, REVIEW_FIELD.qualifier, Item.ANY);
        if (!reviews.isEmpty()) {
            provDescription.append(" | Reviews: ");
        }
        for (MetadataValue review : reviews) {
            provDescription.append(String.format("; %s", review.getValue()));
        }
        c.turnOffAuthorisationSystem();
        itemService.addMetadata(c, wfi.getItem(), MetadataSchemaEnum.DC.getName(),
            "description", "provenance", "en", provDescription.toString());
        itemService.update(c, wfi.getItem());
        c.restoreAuthSystemState();
    }

    @Override
    public List<String> getOptions() {
        List<String> options = new ArrayList<>();
        options.add(RETURN_TO_POOL);
        return options;
    }

    public int getMinimumAcceptanceScore() {
        return minimumAcceptanceScore;
    }

    public void setMinimumAcceptanceScore(int minimumAcceptanceScore) {
        this.minimumAcceptanceScore = minimumAcceptanceScore;
    }
}
