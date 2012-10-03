package org.dspace.app.xmlui.aspect.discovery;

import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.acting.AbstractAction;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Redirector;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.environment.SourceResolver;
import org.dspace.app.util.Util;
import org.dspace.app.xmlui.utils.ContextUtil;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.workflow.DryadWorkflowUtils;
import org.dspace.workflow.WorkflowItem;
import org.dspace.workflow.WorkflowManager;

import java.io.IOException;
import java.util.Map;

/**
 * User: kevin (kevin at atmire.com)
 * Date: 15-sep-2011
 * Time: 9:00:04
 */
public class BackToSubmissionAction extends AbstractAction {


    public Map act(Redirector redirector, SourceResolver resolver, Map objectModel, String source, Parameters parameters) throws Exception {
        Request request = ObjectModelHelper.getRequest(objectModel);
        Context context = ContextUtil.obtainContext(objectModel);

        context.turnOffAuthorisationSystem();
        //Retrieve the reason
        WorkflowItem wfItem = WorkflowItem.findByItemId(context, Util.getIntParameter(request, "itemID"));
        String reason = request.getParameter("reason");

        if(wfItem != null){
            //Reject the item
            WorkflowManager.rejectWorkflowItem(context, wfItem, context.getCurrentUser(), null, reason, true);

            //Also reject all the data files
            Item[] dataFiles = DryadWorkflowUtils.getDataFiles(context, wfItem.getItem());
            for (Item dataFile : dataFiles) {
                try {
                    WorkflowManager.rejectWorkflowItem(context, WorkflowItem.findByItemId(context, dataFile.getID()), context.getCurrentUser(), null, reason, false);
                } catch (Exception e) {
                    throw new IOException(e);
                }
            }

            context.commit();
        }
        context.restoreAuthSystemState();
        return null;
    }
}
