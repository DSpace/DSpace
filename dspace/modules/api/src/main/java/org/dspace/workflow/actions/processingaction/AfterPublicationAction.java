/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dspace.workflow.actions.processingaction;

import java.io.IOException;
import java.sql.SQLException;
import javax.servlet.http.HttpServletRequest;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DCDate;
import org.dspace.content.MetadataSchema;
import org.dspace.core.Context;
import org.dspace.workflow.Step;
import org.dspace.workflow.WorkflowItem;
import org.dspace.workflow.WorkflowManager;
import org.dspace.workflow.actions.ActionResult;

/**
 *
 * @author dan.leehr@nescent.org
 */
public class AfterPublicationAction extends ProcessingAction {

    @Override
    public ActionResult execute(Context c, WorkflowItem wfi, Step step, HttpServletRequest request) throws SQLException, AuthorizeException, IOException {
        if(request.getParameter("after_blackout_submit") != null) {
            addApprovedProvenance(c, wfi);
            // TODO: dleehr 2013-10-01 determine if we need to remove/update embargo
            // Finished, archive
            return new ActionResult(ActionResult.TYPE.TYPE_OUTCOME, ActionResult.OUTCOME_COMPLETE);
        } else {
            //We pressed the leave button so return to our submissions page
            return new ActionResult(ActionResult.TYPE.TYPE_SUBMISSION_PAGE);
        }
    }

    // Copied from EditMetadataAction
    private void addApprovedProvenance(Context c, WorkflowItem wfi) throws SQLException, AuthorizeException {
        //Add the provenance for the accept
        String now = DCDate.getCurrent().toString();

        // Get user's name + email address
        String usersName = WorkflowManager.getEPersonName(c.getCurrentUser());

        String provDescription = getProvenanceStartId() + " Approved for entry into archive by "
                + usersName + " on " + now + " (GMT) ";

        // Add to item as a DC field
        wfi.getItem().addMetadata(MetadataSchema.DC_SCHEMA, "description", "provenance", "en", provDescription);
        wfi.getItem().update();
    }
}
