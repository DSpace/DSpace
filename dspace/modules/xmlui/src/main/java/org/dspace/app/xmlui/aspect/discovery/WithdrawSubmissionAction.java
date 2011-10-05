package org.dspace.app.xmlui.aspect.discovery;

import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.acting.AbstractAction;
import org.apache.cocoon.environment.*;
import org.apache.cocoon.environment.http.HttpEnvironment;
import org.dspace.app.util.Util;
import org.dspace.app.xmlui.utils.ContextUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.InProgressSubmission;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.core.Context;
import org.dspace.workflow.DryadWorkflowUtils;
import org.dspace.workflow.WorkflowItem;
import org.dspace.workflow.WorkflowManager;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * User: kevin (kevin at atmire.com)
 * Date: 15-sep-2011
 * Time: 13:24:59
 */
public class WithdrawSubmissionAction extends AbstractAction {

    public Map act(Redirector redirector, SourceResolver resolver, Map objectModel, String source, Parameters parameters) throws Exception {
        Request request = ObjectModelHelper.getRequest(objectModel);
        Context context = ContextUtil.obtainContext(objectModel);

        context.turnOffAuthorisationSystem();
        WorkflowItem wfItem = WorkflowItem.findByItemId(context, Util.getIntParameter(request, "itemID"));
        if(wfItem != null){
            withdrawInsubmissionItem(context, wfItem);
        }else{
            withdrawInsubmissionItem(context, WorkspaceItem.findByItemId(context, Util.getIntParameter(request, "itemID")));
        }

        context.commit();

        context.restoreAuthSystemState();
        HttpServletResponse httpResponse = (HttpServletResponse) objectModel.get(HttpEnvironment.HTTP_RESPONSE_OBJECT);
        httpResponse.sendRedirect(request.getContextPath() + "/submissions");

        return new HashMap();
    }

    private void withdrawInsubmissionItem(Context context, InProgressSubmission is) throws SQLException, IOException, AuthorizeException {
        //Start by rejecting & deleting our data files
        Item[] dataFiles = DryadWorkflowUtils.getDataFiles(context, is.getItem());
        for (Item dataFile : dataFiles) {
            try {
                WorkspaceItem workspaceDataFile;
                WorkflowItem wfItem = WorkflowItem.findByItemId(context, dataFile.getID());
                if(wfItem != null){
                    workspaceDataFile = WorkflowManager.rejectWorkflowItem(context, wfItem, context.getCurrentUser(), null, null, false);
                }else{
                    workspaceDataFile = WorkspaceItem.findByItemId(context, dataFile.getID());
                }


                withdrawItem(workspaceDataFile);
            } catch (Exception e) {
                throw new IOException(e);
            }
        }

        WorkspaceItem workspaceItem;
        if(is  instanceof WorkflowItem){
            workspaceItem = WorkflowManager.rejectWorkflowItem(context, (WorkflowItem) is, context.getCurrentUser(), null, null, false);
        }else{
            workspaceItem = (WorkspaceItem) is;
        }
        withdrawItem(workspaceItem);
    }

    private void withdrawItem(WorkspaceItem workspaceItem) throws SQLException, AuthorizeException, IOException {
        if(workspaceItem != null){
            Item myItem = workspaceItem.getItem();
            Collection itemColl = workspaceItem.getCollection();
            workspaceItem.deleteWrapper();

            // create collection2item mapping
            itemColl.addItem(myItem);

            // set owning collection
            myItem.setOwningCollection(itemColl);

            // set in_archive=true
            myItem.withdraw();
            myItem.update();
            itemColl.update();
        }
    }
}
