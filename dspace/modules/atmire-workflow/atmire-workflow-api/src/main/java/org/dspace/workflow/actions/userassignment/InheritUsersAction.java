package org.dspace.workflow.actions.userassignment;

import org.dspace.core.Context;
import org.dspace.workflow.Step;
import org.dspace.workflow.WorkflowConfigurationException;
import org.dspace.workflow.WorkflowItem;
import org.dspace.workflow.actions.ActionResult;

import javax.servlet.http.HttpServletRequest;
import java.sql.SQLException;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: bram
 * Date: 2-aug-2010
 * Time: 17:38:55
 * To change this template use File | Settings | File Templates.
 */
public class InheritUsersAction extends UserSelectionAction {

    @Override
    public void activate(Context c, WorkflowItem wfItem) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public ActionResult execute(Context c, WorkflowItem wfi, Step step, HttpServletRequest request) {
        return new ActionResult(ActionResult.TYPE.TYPE_OUTCOME, ActionResult.OUTCOME_COMPLETE);  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean isFinished(WorkflowItem wfi) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void regenerateTasks(Context c, WorkflowItem wfi, List<Integer> userToIgnore) throws SQLException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean isValidUserSelection(Context context, WorkflowItem wfi, boolean hasUI) throws WorkflowConfigurationException, SQLException {
        return false;
    }
}
