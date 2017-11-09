package ar.edu.unlp.sedici.dspace.xmlworkflow.state.actions.processingaction;

import java.io.IOException;
import java.sql.SQLException;

import javax.servlet.http.HttpServletRequest;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.handle.HandleManager;
import org.dspace.xmlworkflow.WorkflowException;
import org.dspace.xmlworkflow.state.Step;
import org.dspace.xmlworkflow.state.actions.ActionResult;
import org.dspace.xmlworkflow.state.actions.processingaction.ProcessingAction;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;

public class SeDiCISelectCollectionAction extends ProcessingAction {

	@Override
	public void activate(Context c, XmlWorkflowItem wf) throws SQLException, IOException, AuthorizeException, WorkflowException {
		// No hay nada para activar
	}

	@Override
	public ActionResult execute(Context c, XmlWorkflowItem wfi, Step step, HttpServletRequest request) throws SQLException, AuthorizeException, IOException, WorkflowException {
        if(request.getParameter("submit") != null){
            String collectionHandle = request.getParameter("collection_handle");
            if(collectionHandle == null || collectionHandle == "")
            	return new ActionResult(ActionResult.TYPE.TYPE_ERROR);
            
            DSpaceObject dso = HandleManager.resolveToObject(c, collectionHandle);
            if(!(dso instanceof Collection))
            	return new ActionResult(ActionResult.TYPE.TYPE_ERROR);
            
            wfi.setCollection((Collection) dso);
            wfi.update();
            
            return new ActionResult(ActionResult.TYPE.TYPE_OUTCOME, ActionResult.OUTCOME_COMPLETE);
        } else {
            //We pressed the leave button so return to our submissions page
            return new ActionResult(ActionResult.TYPE.TYPE_SUBMISSION_PAGE);
        }
	}

}
