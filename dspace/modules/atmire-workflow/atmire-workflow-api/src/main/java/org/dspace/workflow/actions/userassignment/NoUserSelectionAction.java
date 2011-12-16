package org.dspace.workflow.actions.userassignment;

import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.workflow.Step;
import org.dspace.workflow.WorkflowConfigurationException;
import org.dspace.workflow.WorkflowException;
import org.dspace.workflow.WorkflowItem;
import org.dspace.workflow.actions.ActionResult;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

/**
 * User: kevin (kevin at atmire.com)
 * Date: 19-aug-2010
 * Time: 16:07:48
 * An user selection action that doesn't create any tasks
 * This type of user selection action may only be used for automatic steps
 */
public class NoUserSelectionAction extends UserSelectionAction{
    @Override
    public boolean isFinished(WorkflowItem wfi) {
        return true;
    }

    @Override
    public void regenerateTasks(Context c, WorkflowItem wfi, List<Integer> userToIgnore) throws SQLException {
    }

    @Override
    public boolean isValidUserSelection(Context context, WorkflowItem wfi, boolean hasUI) throws WorkflowConfigurationException, SQLException {
        return true;
    }

    @Override
    public void activate(Context c, WorkflowItem wf) throws SQLException, IOException {
    }

    @Override
    public ActionResult execute(Context c, WorkflowItem wfi, Step step, HttpServletRequest request) throws SQLException, AuthorizeException, IOException, WorkflowException {
        return new ActionResult(ActionResult.TYPE.TYPE_OUTCOME, ActionResult.OUTCOME_COMPLETE);
    }
}
