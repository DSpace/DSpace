package ar.edu.unlp.sedici.dspace.xmlworkflow.state.actions.processingaction;

import java.sql.SQLException;

import org.dspace.authorize.AuthorizeManager;
import org.dspace.core.Context;
import org.dspace.xmlworkflow.WorkflowConfigurationException;
import org.dspace.xmlworkflow.state.actions.userassignment.ClaimAction;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;


/**
 * Behaves like the ClaimAction processing action, except that when the logged user is a target collection's admin, 
 * this step is not executed.
 * 
 * @author nestor
 */
public class SkipAdminClaimAction extends ClaimAction {

	@Override
	public boolean isValidUserSelection(Context context, XmlWorkflowItem wfi, boolean hasUI) throws WorkflowConfigurationException, SQLException {
		if(AuthorizeManager.isAdmin(context, wfi.getCollection()))
			return false;
		return super.isValidUserSelection(context, wfi, hasUI);
	}
}
