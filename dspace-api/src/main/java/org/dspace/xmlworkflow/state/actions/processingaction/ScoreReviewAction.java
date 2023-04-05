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
import java.util.List;
import javax.servlet.http.HttpServletRequest;

import org.dspace.app.util.Util;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.xmlworkflow.service.WorkflowRequirementsService;
import org.dspace.xmlworkflow.state.Step;
import org.dspace.xmlworkflow.state.actions.ActionResult;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;

/**
 * This action will allow multiple users to rate a certain item
 * if the mean of this score is higher then the minimum score the
 * item will be sent to the next action/step else it will be rejected
 *
 * @author Bram De Schouwer (bram.deschouwer at dot com)
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 */
public class ScoreReviewAction extends ProcessingAction {

    private static final String SUBMIT_SCORE = "submit_score";

    @Override
    public void activate(Context c, XmlWorkflowItem wf) {

    }

    @Override
    public ActionResult execute(Context c, XmlWorkflowItem wfi, Step step, HttpServletRequest request)
            throws SQLException, AuthorizeException {
        if (request.getParameter(SUBMIT_SCORE) != null) {
            int score = Util.getIntParameter(request, "score");
            //Add our score to the metadata
            itemService.addMetadata(c, wfi.getItem(), WorkflowRequirementsService.WORKFLOW_SCHEMA, "score", null, null,
                    String.valueOf(score));
            itemService.update(c, wfi.getItem());

            return new ActionResult(ActionResult.TYPE.TYPE_OUTCOME, ActionResult.OUTCOME_COMPLETE);
        } else {
            //We have pressed the leave button so return to our submission page
            return new ActionResult(ActionResult.TYPE.TYPE_SUBMISSION_PAGE);
        }
    }

    @Override
    public List<String> getOptions() {
        return Arrays.asList(SUBMIT_SCORE);
    }
}
