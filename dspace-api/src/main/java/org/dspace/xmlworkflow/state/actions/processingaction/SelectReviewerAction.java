/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xmlworkflow.state.actions.processingaction;

import org.dspace.app.util.Util;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.xmlworkflow.WorkflowException;
import org.dspace.xmlworkflow.state.Step;
import org.dspace.xmlworkflow.state.actions.ActionResult;
import org.dspace.xmlworkflow.storedcomponents.WorkflowItemRole;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.sql.SQLException;

/**
 * Processing class for an action where an assigned user can
 * assign another user to review the item
 *
 * @author Bram De Schouwer (bram.deschouwer at dot com)
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 */
public class SelectReviewerAction extends ProcessingAction{

    public static final int MAIN_PAGE = 0;
    public static final int SEARCH_RESULTS_PAGE = 1;

    public static final int RESULTS_PER_PAGE = 5;

    private String roleId;

    @Override
    public void activate(Context c, XmlWorkflowItem wf) throws SQLException, IOException, AuthorizeException, WorkflowException {

    }

    @Override
    public ActionResult execute(Context c, XmlWorkflowItem wfi, Step step, HttpServletRequest request) throws SQLException, AuthorizeException, IOException, WorkflowException {
        String submitButton = Util.getSubmitButton(request, "submit_cancel");

        //Check if our user has pressed cancel
        if(submitButton.equals("submit_cancel")){
            //Send us back to the submissions page
            return new ActionResult(ActionResult.TYPE.TYPE_CANCEL);

        }else
        if(submitButton.equals("submit_search")){
            //Perform the search
            String query = request.getParameter("query");
            int page = Util.getIntParameter(request, "result-page");
            if(page == -1){
                page = 0;
            }

            int resultCount = EPerson.searchResultCount(c, query);
            EPerson[] epeople = EPerson.search(c, query, page*RESULTS_PER_PAGE, RESULTS_PER_PAGE);


            request.setAttribute("eperson-result-count", resultCount);
            request.setAttribute("eperson-results", epeople);
            request.setAttribute("result-page", page);
            request.setAttribute("page", SEARCH_RESULTS_PAGE);
            return new ActionResult(ActionResult.TYPE.TYPE_PAGE, SEARCH_RESULTS_PAGE);
        }else
        if(submitButton.startsWith("submit_select_reviewer_")){
            //Retrieve the identifier of the eperson which will do the reviewing
            int reviewerId = Integer.parseInt(submitButton.substring(submitButton.lastIndexOf("_") + 1));
            EPerson reviewer = EPerson.find(c, reviewerId);
            //We have a reviewer, assign him, the workflowitemrole will be translated into a task in the autoassign
            WorkflowItemRole workflowItemRole = WorkflowItemRole.create(c);
            workflowItemRole.setEPerson(reviewer);
            workflowItemRole.setRoleId(getRoleId());
            workflowItemRole.setWorkflowItemId(wfi.getID());
            workflowItemRole.update();
            return new ActionResult(ActionResult.TYPE.TYPE_OUTCOME, ActionResult.OUTCOME_COMPLETE);
        }

        //There are only 2 active buttons on this page, so if anything else happens just return an error
        return new ActionResult(ActionResult.TYPE.TYPE_ERROR);
    }

    public String getRoleId() {
        return roleId;
    }

    public void setRoleId(String roleId) {
        this.roleId = roleId;
    }
}
