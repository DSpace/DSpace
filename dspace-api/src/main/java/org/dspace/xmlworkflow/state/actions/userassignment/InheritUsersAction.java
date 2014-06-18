/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xmlworkflow.state.actions.userassignment;

import org.dspace.core.Context;
import org.dspace.xmlworkflow.RoleMembers;
import org.dspace.xmlworkflow.state.Step;
import org.dspace.xmlworkflow.WorkflowConfigurationException;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;
import org.dspace.xmlworkflow.state.actions.ActionResult;

import javax.servlet.http.HttpServletRequest;
import java.sql.SQLException;

/**
 * A user selection action that inherits user
 * assignment from the previous step
 *
 * @author Bram De Schouwer (bram.deschouwer at dot com)
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 */
public class InheritUsersAction extends UserSelectionAction {

    @Override
    public void activate(Context c, XmlWorkflowItem wfItem) {
    }

    @Override
    public ActionResult execute(Context c, XmlWorkflowItem wfi, Step step, HttpServletRequest request) {
        return new ActionResult(ActionResult.TYPE.TYPE_OUTCOME, ActionResult.OUTCOME_COMPLETE);
    }

    @Override
    public boolean isFinished(XmlWorkflowItem wfi) {
        return false;
    }

    @Override
    public void regenerateTasks(Context c, XmlWorkflowItem wfi,  RoleMembers roleMembers) throws SQLException {
    }

    @Override
    public boolean isValidUserSelection(Context context, XmlWorkflowItem wfi, boolean hasUI) throws WorkflowConfigurationException, SQLException {
        return false;
    }

    @Override
    public boolean usesTaskPool() {
        return false;
    }
}
