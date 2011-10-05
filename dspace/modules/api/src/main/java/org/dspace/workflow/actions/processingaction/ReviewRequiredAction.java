package org.dspace.workflow.actions.processingaction;

import org.dspace.app.util.Util;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DCValue;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.workflow.Step;
import org.dspace.workflow.WorkflowItem;
import org.dspace.workflow.WorkflowRequirementsManager;
import org.dspace.workflow.actions.ActionResult;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.sql.SQLException;

/**
 * Created by IntelliJ IDEA.
 * User: bram
 * Date: 20-aug-2010
 * Time: 14:36:08
 *
 * An action in the dryad review process which indicates wether or not a submission is to go to the review stage
 */
public class ReviewRequiredAction extends ProcessingAction{

    private static int REVIEW_REQUIRED = 0;
    private static int REVIEW_NOT_REQUIRED = 1;


    @Override
    public void activate(Context c, WorkflowItem wfItem) {

    }

    @Override
    public ActionResult execute(Context c, WorkflowItem wfi, Step step, HttpServletRequest request) throws SQLException, AuthorizeException, IOException {
        Item item = wfi.getItem();

        DCValue[] skipVals = item.getMetadata(WorkflowRequirementsManager.WORKFLOW_SCHEMA, "submit", "skipReviewStage", Item.ANY);
        item.clearMetadata(WorkflowRequirementsManager.WORKFLOW_SCHEMA, "submit", "skipReviewStage", Item.ANY);
        item.update();
        if(0 == skipVals.length || Boolean.valueOf(skipVals[0].value)){
            return new ActionResult(ActionResult.TYPE.TYPE_OUTCOME, REVIEW_NOT_REQUIRED);
        }else{
            return new ActionResult(ActionResult.TYPE.TYPE_OUTCOME, REVIEW_REQUIRED);
        }
    }

}
