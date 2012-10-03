package org.dspace.app.xmlui.aspect.discovery;

import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.acting.AbstractAction;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Redirector;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.environment.SourceResolver;
import org.dspace.app.util.Util;
import org.dspace.app.xmlui.utils.ContextUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.workflow.*;
import org.dspace.workflow.actions.ActionResult;
import org.dspace.workflow.actions.WorkflowActionConfig;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

/**
 * User: kevin (kevin at atmire.com)
 * Date: 16-sep-2011
 * Time: 13:13:55
 */
public class AlterWorkflowStepAction extends AbstractAction {
    public Map act(Redirector redirector, SourceResolver resolver, Map objectModel, String source, Parameters parameters) throws Exception {
        Request request = ObjectModelHelper.getRequest(objectModel);
        Context context = ContextUtil.obtainContext(objectModel);

        context.turnOffAuthorisationSystem();
        //Retrieve the reason
        WorkflowItem wfItem = WorkflowItem.findByItemId(context, Util.getIntParameter(request, "itemID"));
        String newStepIdentifier = request.getParameter("step");

        if(wfItem != null && newStepIdentifier != null){
            Workflow workflow = WorkflowFactory.getWorkflow(wfItem.getCollection());
            Step newStep = workflow.getStep(newStepIdentifier);
            changeWorkflowStep(context, workflow, wfItem, newStep);
        }
        context.restoreAuthSystemState();

        return null;
    }

    private void changeWorkflowStep(Context context, Workflow workflow, WorkflowItem wfItem, Step newStep) throws AuthorizeException, SQLException, IOException, WorkflowConfigurationException, WorkflowException, TransformerException, SAXException, ParserConfigurationException {
        if(wfItem != null){
            //Clear all the metadata that might be saved by this step
            WorkflowRequirementsManager.clearStepMetadata(wfItem);
            //Remove all the tasks
            WorkflowManager.deleteAllTasks(context, wfItem);


            WorkflowActionConfig nextActionConfig = newStep.getUserSelectionMethod();
            nextActionConfig.getProcessingAction().activate(context, wfItem);

            if (!nextActionConfig.hasUserInterface()) {
                ActionResult newOutcome = nextActionConfig.getProcessingAction().execute(context, wfItem, newStep, null);
                WorkflowManager.processOutcome(context, context.getCurrentUser(), workflow, newStep, nextActionConfig, newOutcome, wfItem);
            }
        }
    }
}
