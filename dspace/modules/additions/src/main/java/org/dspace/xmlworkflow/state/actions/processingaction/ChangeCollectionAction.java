package org.dspace.xmlworkflow.state.actions.processingaction;

import java.io.IOException;
import java.sql.SQLException;

import javax.servlet.http.HttpServletRequest;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.handle.HandleManager;
import org.dspace.xmlworkflow.WorkflowConfigurationException;
import org.dspace.xmlworkflow.WorkflowException;
import org.dspace.xmlworkflow.WorkflowFactory;
import org.dspace.xmlworkflow.XmlWorkflowManager;
import org.dspace.xmlworkflow.state.Step;
import org.dspace.xmlworkflow.state.Workflow;
import org.dspace.xmlworkflow.state.actions.ActionResult;
import org.dspace.xmlworkflow.storedcomponents.ClaimedTask;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;

public class ChangeCollectionAction extends ProcessingAction {

	@Override
	public void activate(Context c, XmlWorkflowItem wf) throws SQLException,
			IOException, AuthorizeException, WorkflowException {
	}

	@Override
	public ActionResult execute(Context c, XmlWorkflowItem wfi, Step step,HttpServletRequest request) throws SQLException,AuthorizeException, IOException, WorkflowException {
		if(request.getParameter("submit") != null){
            String collectionHandle = request.getParameter("collection_handle");
            if(collectionHandle == null || collectionHandle == "")
            	return new ActionResult(ActionResult.TYPE.TYPE_ERROR);
            
            DSpaceObject dso = HandleManager.resolveToObject(c, collectionHandle);
            if(!(dso instanceof Collection))
            	return new ActionResult(ActionResult.TYPE.TYPE_ERROR);

            ClaimedTask oldClaimedTask = ClaimedTask.findByWorkflowIdAndEPerson(c, wfi.getID(), c.getCurrentUser().getID());
            XmlWorkflowManager.deleteClaimedTask(c, wfi, oldClaimedTask);
            wfi.setCollection((Collection) dso);
            wfi.update();
            return new ActionResult(ActionResult.TYPE.TYPE_OUTCOME, ActionResult.OUTCOME_COMPLETE);
        } else {
            //We pressed the "cancel" button so return to our submissions page
            return new ActionResult(ActionResult.TYPE.TYPE_CANCEL);
        }
	}

}
