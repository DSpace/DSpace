/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.xmlworkflow.admin;

import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.acting.AbstractAction;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Redirector;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.environment.SourceResolver;
import org.dspace.app.util.Util;
import org.dspace.app.xmlui.utils.ContextUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.WorkspaceItem;
import org.dspace.core.Context;
import org.dspace.xmlworkflow.XmlWorkflowManager;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;

import java.util.Map;

/**
 * An action that allows administrators to delete items in the workflow
 *
 * @author Bram De Schouwer (bram.deschouwer at dot com)
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 */
public class DeleteWorkflowItemsAction extends AbstractAction {

    @Override
    public Map act(Redirector redirector, SourceResolver resolver, Map objectModel, String source, Parameters parameters) throws Exception {
        Request request = ObjectModelHelper.getRequest(objectModel);
        Context context = ContextUtil.obtainContext(request);
        if(!AuthorizeManager.isAdmin(context)){
            throw new AuthorizeException();
        }

        int[] workflowIdentifiers = Util.getIntParameters(request, "workflow_id");
        if(workflowIdentifiers != null){
            for (int workflowIdentifier : workflowIdentifiers) {
                XmlWorkflowItem workflowItem = XmlWorkflowItem.find(context, workflowIdentifier);
                if (workflowItem != null) {
                    WorkspaceItem workspaceItem = XmlWorkflowManager.sendWorkflowItemBackSubmission(context, workflowItem, context.getCurrentUser(), "Item sent back to the submisson process by admin", null);
                    //Delete the workspaceItem
                    workspaceItem.deleteAll();
                }
            }
        }

        return null;
    }
}
