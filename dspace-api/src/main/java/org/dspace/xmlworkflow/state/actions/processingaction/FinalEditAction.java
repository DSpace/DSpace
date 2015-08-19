/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xmlworkflow.state.actions.processingaction;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DCDate;
import org.dspace.content.MetadataSchema;
import org.dspace.core.Context;
import org.dspace.xmlworkflow.factory.XmlWorkflowServiceFactory;
import org.dspace.xmlworkflow.state.Step;
import org.dspace.xmlworkflow.state.actions.ActionResult;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.sql.SQLException;

/**
 * Processing class of an action that allows users to
 * accept/reject a workflow item
 *
 * @author Bram De Schouwer (bram.deschouwer at dot com)
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 */
public class FinalEditAction extends ProcessingAction {


    @Override
    public void activate(Context c, XmlWorkflowItem wf) throws SQLException {

    }

    @Override
    public ActionResult execute(Context c, XmlWorkflowItem wfi, Step step, HttpServletRequest request) throws SQLException, AuthorizeException, IOException {
        return processMainPage(c, wfi, step, request);
    }

    public ActionResult processMainPage(Context c, XmlWorkflowItem wfi, Step step, HttpServletRequest request) throws SQLException, AuthorizeException {
        if(request.getParameter("submit_approve") != null){
            //Delete the tasks
            addApprovedProvenance(c, wfi);

            return new ActionResult(ActionResult.TYPE.TYPE_OUTCOME, ActionResult.OUTCOME_COMPLETE);
        } else {
            //We pressed the leave button so return to our submissions page
            return new ActionResult(ActionResult.TYPE.TYPE_SUBMISSION_PAGE);
        }
    }

    private void addApprovedProvenance(Context c, XmlWorkflowItem wfi) throws SQLException, AuthorizeException {
        //Add the provenance for the accept
        String now = DCDate.getCurrent().toString();

        // Get user's name + email address
        String usersName = XmlWorkflowServiceFactory.getInstance().getXmlWorkflowService().getEPersonName(c.getCurrentUser());

        String provDescription = getProvenanceStartId() + " Approved for entry into archive by "
                + usersName + " on " + now + " (GMT) ";

        // Add to item as a DC field
        itemService.addMetadata(c, wfi.getItem(), MetadataSchema.DC_SCHEMA, "description", "provenance", "en", provDescription);
        itemService.update(c, wfi.getItem());
    }



}
