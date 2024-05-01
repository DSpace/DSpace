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
import java.util.Collections;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.workflow.WorkflowException;
import org.dspace.xmlworkflow.state.Step;
import org.dspace.xmlworkflow.state.actions.ActionResult;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;

/**
 * Null workflow processing action for testing.
 * @author mwood
 */
public class NoAction extends ProcessingAction {
    @Override
    public void activate(Context c, XmlWorkflowItem wf)
            throws SQLException, IOException, AuthorizeException, WorkflowException {
    }

    @Override
    public ActionResult execute(Context c, XmlWorkflowItem wfi, Step step,
            HttpServletRequest request)
            throws SQLException, AuthorizeException, IOException, WorkflowException {
        return new ActionResult(ActionResult.TYPE.TYPE_OUTCOME,
                ActionResult.OUTCOME_COMPLETE);
    }

    @Override
    public List<String> getOptions() {
        return Collections.EMPTY_LIST;
    }
}
