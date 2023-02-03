/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xmlworkflow.state.actions.processingaction;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.util.Util;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.MetadataFieldName;
import org.dspace.core.Context;
import org.dspace.xmlworkflow.service.WorkflowRequirementsService;
import org.dspace.xmlworkflow.state.Step;
import org.dspace.xmlworkflow.state.actions.ActionAdvancedInfo;
import org.dspace.xmlworkflow.state.actions.ActionResult;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;

/**
 * This action will allow multiple users to rate a certain item
 * if the mean of this score is higher then the minimum score the
 * item will be sent to the next action/step else it will be rejected
 */
public class ScoreReviewAction extends ProcessingAction {
    private static final Logger log = LogManager.getLogger(ScoreReviewAction.class);

    // Option(s)
    public static final String SUBMIT_SCORE = "submit_score";

    // Response param(s)
    private static final String SCORE = "score";
    private static final String REVIEW = "review";

    // Metadata fields to save params in
    public static final MetadataFieldName SCORE_FIELD =
        new MetadataFieldName(WorkflowRequirementsService.WORKFLOW_SCHEMA, SCORE, null);
    public static final MetadataFieldName REVIEW_FIELD =
        new MetadataFieldName(WorkflowRequirementsService.WORKFLOW_SCHEMA, REVIEW, null);

    // Whether or not it is required that a text review is added to the rating
    private boolean descriptionRequired;
    // Maximum value rating is allowed to be
    private int maxValue;

    @Override
    public void activate(Context c, XmlWorkflowItem wf) {
        // empty
    }

    @Override
    public ActionResult execute(Context c, XmlWorkflowItem wfi, Step step, HttpServletRequest request)
        throws SQLException, AuthorizeException {
        if (super.isOptionInParam(request) &&
            StringUtils.equalsIgnoreCase(Util.getSubmitButton(request, SUBMIT_CANCEL), SUBMIT_SCORE)) {
            return processSetRating(c, wfi, request);
        }
        return new ActionResult(ActionResult.TYPE.TYPE_CANCEL);
    }

    private ActionResult processSetRating(Context c, XmlWorkflowItem wfi, HttpServletRequest request)
        throws SQLException, AuthorizeException {

        int score = Util.getIntParameter(request, SCORE);
        String review = request.getParameter(REVIEW);
        if (!this.checkRequestValid(score, review)) {
            return new ActionResult(ActionResult.TYPE.TYPE_ERROR);
        }
        //Add our rating and review to the metadata
        itemService.addMetadata(c, wfi.getItem(), SCORE_FIELD.schema, SCORE_FIELD.element, SCORE_FIELD.qualifier, null,
            String.valueOf(score));
        if (StringUtils.isNotBlank(review)) {
            itemService.addMetadata(c, wfi.getItem(), REVIEW_FIELD.schema, REVIEW_FIELD.element,
                REVIEW_FIELD.qualifier, null, String.format("%s - %s", score, review));
        }
        itemService.update(c, wfi.getItem());

        return new ActionResult(ActionResult.TYPE.TYPE_OUTCOME, ActionResult.OUTCOME_COMPLETE);
    }

    /**
     * Request is not valid if:
     * - Given score is higher than configured maxValue
     * - There is no review given and description is configured to be required
     * Config in workflow-actions.xml
     *
     * @param score  Given score rating from request
     * @param review Given review/description from request
     * @return True if valid request params with config, otherwise false
     */
    private boolean checkRequestValid(int score, String review) {
        if (score > this.maxValue) {
            log.error("{} only allows max rating {} (config workflow-actions.xml), given rating of " +
                "{} not allowed.", this.getClass().toString(), this.maxValue, score);
            return false;
        }
        if (StringUtils.isBlank(review) && this.descriptionRequired) {
            log.error("{} has config descriptionRequired=true (workflow-actions.xml), so rating " +
                "requests without 'review' query param containing description are not allowed",
                this.getClass().toString());
            return false;
        }
        return true;
    }

    @Override
    public List<String> getOptions() {
        return List.of(SUBMIT_SCORE, RETURN_TO_POOL);
    }

    @Override
    protected List<String> getAdvancedOptions() {
        return Arrays.asList(SUBMIT_SCORE);
    }

    @Override
    protected List<ActionAdvancedInfo> getAdvancedInfo() {
        ScoreReviewActionAdvancedInfo scoreReviewActionAdvancedInfo = new ScoreReviewActionAdvancedInfo();
        scoreReviewActionAdvancedInfo.setDescriptionRequired(descriptionRequired);
        scoreReviewActionAdvancedInfo.setMaxValue(maxValue);
        scoreReviewActionAdvancedInfo.setType(SUBMIT_SCORE);
        scoreReviewActionAdvancedInfo.generateId(SUBMIT_SCORE);
        return Collections.singletonList(scoreReviewActionAdvancedInfo);
    }

    /**
     * Setter that sets the descriptionRequired property from workflow-actions.xml
     * @param descriptionRequired boolean whether a description is required
     */
    public void setDescriptionRequired(boolean descriptionRequired) {
        this.descriptionRequired = descriptionRequired;
    }

    /**
     * Setter that sets the maxValue property from workflow-actions.xml
     * @param maxValue integer of the maximum allowed value
     */
    public void setMaxValue(int maxValue) {
        this.maxValue = maxValue;
    }
}
