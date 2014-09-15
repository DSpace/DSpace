package ar.edu.unlp.sedici.dspace.xmlworkflow.state.actions.processingaction;

import java.sql.SQLException;

import org.dspace.authorize.AuthorizeManager;
import org.dspace.core.Context;
import org.dspace.xmlworkflow.WorkflowConfigurationException;
import org.dspace.xmlworkflow.state.actions.userassignment.ClaimAction;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;

public class AcceptRoleOrNotAdmin extends ClaimAction {

	public boolean isValidUserSelection(Context ctx, XmlWorkflowItem wfi, boolean hasUI) throws WorkflowConfigurationException, SQLException {
		if (skipStep(ctx, wfi))
			return false;
		else{
			return super.isValidUserSelection(ctx, wfi, hasUI);
		}
		
	}
	
	private boolean skipStep(Context ctx, XmlWorkflowItem wfi) throws SQLException{
		return (AuthorizeManager.isAdmin(ctx, wfi.getCollection()) && !(isUserMemberInStepRole(ctx,wfi)));
	}
	
	private boolean isUserMemberInStepRole(Context ctx, XmlWorkflowItem wfi) throws SQLException{
		return getParent().getStep().getRole().getMembers(ctx, wfi).getAllUniqueMembers(ctx).contains(ctx.getCurrentUser());
	}
}
