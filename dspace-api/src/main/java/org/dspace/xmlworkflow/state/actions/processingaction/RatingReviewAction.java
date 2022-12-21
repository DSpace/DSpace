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
import java.util.Arrays;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

import org.dspace.app.util.Util;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.workflow.WorkflowException;
import org.dspace.xmlworkflow.service.WorkflowRequirementsService;
import org.dspace.xmlworkflow.state.Step;
import org.dspace.xmlworkflow.state.actions.ActionAdvancedInfo;
import org.dspace.xmlworkflow.state.actions.ActionResult;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;

public class RatingReviewAction extends ProcessingAction {

    private static final String RATING = "rating";

    private boolean descriptionRequired;
    private int maxValue;

    @Override
    public void activate(Context c, XmlWorkflowItem wf)
        throws SQLException, IOException, AuthorizeException, WorkflowException {

    }

    @Override
    public ActionResult execute(Context c, XmlWorkflowItem wfi, Step step, HttpServletRequest request)
        throws SQLException, AuthorizeException, IOException, WorkflowException {
        if (request.getParameter(RATING) != null) {
            int rating = Util.getIntParameter(request, "rating");
            //Add our rating to the metadata
            itemService.addMetadata(c, wfi.getItem(), WorkflowRequirementsService.WORKFLOW_SCHEMA, "rating", null, null,
                                    String.valueOf(rating));
            itemService.update(c, wfi.getItem());

            return new ActionResult(ActionResult.TYPE.TYPE_OUTCOME, ActionResult.OUTCOME_COMPLETE);
        } else {
            //We have pressed the leave button so return to our submission page
            return new ActionResult(ActionResult.TYPE.TYPE_SUBMISSION_PAGE);
        }
    }

    @Override
    public List<String> getOptions() {
        return Arrays.asList(RATING);
    }

    @Override
    protected List<String> getAdvancedOptions() {
        return Arrays.asList(RATING);
    }

    @Override
    protected boolean isAdvanced() {
        return !getAdvancedOptions().isEmpty();
    }

    @Override
    protected List<ActionAdvancedInfo> getAdvancedInfo() {
        List<ActionAdvancedInfo> advancedInfo = super.getAdvancedInfo();
        RatingReviewActionAdvancedInfo ratingReviewActionAdvancedInfo = new RatingReviewActionAdvancedInfo();
        ratingReviewActionAdvancedInfo.setDescriptionRequired(descriptionRequired);
        ratingReviewActionAdvancedInfo.setMaxValue(maxValue);
        ratingReviewActionAdvancedInfo.setType(RATING);
        ratingReviewActionAdvancedInfo.setId(RATING);
        advancedInfo.add(ratingReviewActionAdvancedInfo);
        return advancedInfo;
    }

    public void setDescriptionRequired(boolean descriptionRequired) {
        this.descriptionRequired = descriptionRequired;
    }

    public void setMaxValue(int maxValue) {
        this.maxValue = maxValue;
    }
}
