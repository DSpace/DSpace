package org.dspace.workflow.actions.processingaction;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DCValue;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.submit.utils.DryadJournalSubmissionUtils;
import org.dspace.workflow.Step;
import org.dspace.workflow.WorkflowItem;
import org.dspace.workflow.WorkflowRequirementsManager;
import org.dspace.workflow.actions.ActionResult;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.sql.SQLException;

/**
 * Created by IntelliJ IDEA.
 * User: fabio.bolognesi
 * Date: 9/7/11
 * Time: 9:35 AM
 * To change this template use File | Settings | File Templates.
 */

/**
 * This action determines whether the item should be kept in the "Pending Publication Step" based on "publicationBlackout" parameter defined on config/DryadJournalSubmission.properties.
 * if journal.TYPE.publicationBlackout=true: kept in step.
 * if journal.TYPE.publicationBlackout=false || null: sent to the next step (in this case archiving).
 */
public class PendingPublicationAction extends ProcessingAction{

    private static final int REVIEW_REQUIRED = 1;
    private static final int REVIEW_NOT_REQUIRED = 0;


    @Override
    public void activate(Context c, WorkflowItem wfItem) {}

    @Override
    public ActionResult execute(Context c, WorkflowItem wfi, Step step, HttpServletRequest request) throws SQLException, AuthorizeException, IOException {
        if(DryadJournalSubmissionUtils.isJournalBlackedOut(c, wfi.getItem(), wfi.getCollection()))
            return new ActionResult(ActionResult.TYPE.TYPE_OUTCOME, REVIEW_REQUIRED);

        return new ActionResult(ActionResult.TYPE.TYPE_OUTCOME, REVIEW_NOT_REQUIRED);

    }
}
