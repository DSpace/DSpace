package ar.edu.unlp.sedici.dspace.xmlworkflow.state.actions.userassignment;

import java.sql.SQLException;

import org.dspace.core.Context;
import org.dspace.xmlworkflow.WorkflowConfigurationException;
import org.dspace.xmlworkflow.state.actions.userassignment.ClaimAction;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;
import org.dspace.xmlworkflow.Role;

/**
 * This class rejects the current step if the item submitter is member of the configured role. Otherwise, proceed normally as ClaimAction does.
 * @author "facundo@sedici.unlp.edu.ar"
 *
 */
public class RejectRole extends ClaimAction {

	public boolean isValidUserSelection(Context context, XmlWorkflowItem wfi, boolean hasUI) throws WorkflowConfigurationException, SQLException{
		Role role =	this.getParent().getStep().getRole();
		boolean submitterIsInRole = role.getMembers(context, wfi).getAllUniqueMembers(context).contains(wfi.getSubmitter());
		
		if(!submitterIsInRole) {
			return super.isValidUserSelection(context, wfi, hasUI);
		}
		//If the item submitter belongs to the configured role, then reject the step.
		return !submitterIsInRole;
	}
	
}
