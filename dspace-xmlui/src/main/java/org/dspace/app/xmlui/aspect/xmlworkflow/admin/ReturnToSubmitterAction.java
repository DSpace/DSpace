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
import org.dspace.authorize.AuthorizeServiceImpl;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.core.Context;
import org.dspace.xmlworkflow.XmlWorkflowServiceImpl;
import org.dspace.xmlworkflow.factory.XmlWorkflowServiceFactory;
import org.dspace.xmlworkflow.service.XmlWorkflowService;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;
import org.dspace.xmlworkflow.storedcomponents.service.XmlWorkflowItemService;

import java.util.Map;

/**
 * Action that sends all the workflow items in the request back to the submitter
 *
 * @author Bram De Schouwer (bram.deschouwer at dot com)
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 */
public class ReturnToSubmitterAction extends AbstractAction {

    protected AuthorizeService authorizeService = AuthorizeServiceFactory.getInstance().getAuthorizeService();
    protected XmlWorkflowService xmlWorkflowService = XmlWorkflowServiceFactory.getInstance().getXmlWorkflowService();
    protected XmlWorkflowItemService xmlWorkflowItemService = XmlWorkflowServiceFactory.getInstance().getXmlWorkflowItemService();

    @Override
    public Map act(Redirector redirector, SourceResolver resolver, Map objectModel, String source, Parameters parameters) throws Exception {
        Request request = ObjectModelHelper.getRequest(objectModel);
        Context context = ContextUtil.obtainContext(request);
        if(!authorizeService.isAdmin(context)){
            throw new AuthorizeException();
        }

        int[] workflowIdentifiers = Util.getIntParameters(request, "workflow_id");
        if(workflowIdentifiers != null){
            for (int workflowIdentifier : workflowIdentifiers) {
                XmlWorkflowItem workflowItem = xmlWorkflowItemService.find(context, workflowIdentifier);
                if (workflowItem != null) {
                    xmlWorkflowService.sendWorkflowItemBackSubmission(context, workflowItem, context.getCurrentUser(), "Item sent back to the submisson process by admin", null);
                }
            }
        }
        return null;
    }
}
