/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.xmlworkflow.admin;

import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.dspace.app.util.Util;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.ContextUtil;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.app.xmlui.wing.element.ReferenceSet;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeServiceImpl;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.core.Context;
import org.dspace.xmlworkflow.factory.XmlWorkflowServiceFactory;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;
import org.dspace.xmlworkflow.storedcomponents.service.XmlWorkflowItemService;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.sql.SQLException;

/**
 * A transformer that renders xmlworkflow items
 * so that an admin can view the item
 *
 * @author Bram De Schouwer (bram.deschouwer at dot com)
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 */
public class WorkflowItemTransformer extends AbstractDSpaceTransformer {

    private static final Message T_workflow_title = message("xmlui.XMLWorkflow.WorkflowItemTransformer.title");

    private static final Message T_dspace_home =
        message("xmlui.general.dspace_home");

    private static final Message T_trail =
        message("xmlui.XMLWorkflow.WorkflowItemTransformer.trail");

    protected static final Message T_showfull =
        message("xmlui.Submission.general.showfull");
    protected static final Message T_showsimple =
        message("xmlui.Submission.general.showsimple");

    protected AuthorizeService authorizeService = AuthorizeServiceFactory.getInstance().getAuthorizeService();
    protected XmlWorkflowItemService xmlWorkflowItemService = XmlWorkflowServiceFactory.getInstance().getXmlWorkflowItemService();


    @Override
    public void addPageMeta(PageMeta pageMeta) throws SAXException, WingException, UIException, SQLException, IOException, AuthorizeException {
        Request request = ObjectModelHelper.getRequest(objectModel);
        Context context = ContextUtil.obtainContext(objectModel);

        XmlWorkflowItem xmlWorkflowItem = retrieveWorkflowItem(request, context);

        pageMeta.addMetadata("title").addContent(T_workflow_title.parameterize(xmlWorkflowItem.getItem().getName()));

        pageMeta.addTrailLink(contextPath + "/",T_dspace_home);
        pageMeta.addTrail().addContent(T_trail);

    }

    @Override
    public void addBody(Body body) throws SAXException, WingException, UIException, SQLException, IOException, AuthorizeException {
        Request request = ObjectModelHelper.getRequest(objectModel);
        Context context = ContextUtil.obtainContext(request);
        if(!authorizeService.isAdmin(context)){
            throw new AuthorizeException();
        }

        XmlWorkflowItem xmlWorkflowItem = retrieveWorkflowItem(request, context);
        String actionURL = contextPath + "/admin/display-workflowItem";

        Division div = body.addInteractiveDivision("perform-task", actionURL, Division.METHOD_POST, "primary workflow");
//        div.setHead(T_workflow_head);

        div.addHidden("wfiId").setValue(xmlWorkflowItem.getID());

        String showfull = request.getParameter("submit_full_item_info");

        // if the user selected showsimple, remove showfull.
        if (showfull != null && request.getParameter("submit_simple_item_info") != null)
            showfull = null;

        if (showfull == null)
        {
	        ReferenceSet referenceSet = div.addReferenceSet("narf",ReferenceSet.TYPE_SUMMARY_VIEW);
            referenceSet.addReference(xmlWorkflowItem.getItem());
	        div.addPara().addButton("submit_full_item_info").setValue(T_showfull);
        }
        else
        {
            ReferenceSet referenceSet = div.addReferenceSet("narf", ReferenceSet.TYPE_DETAIL_VIEW);
            referenceSet.addReference(xmlWorkflowItem.getItem());
            div.addPara().addButton("submit_simple_item_info").setValue(T_showsimple);

            div.addHidden("submit_full_item_info").setValue("true");
        }

//        TODO: show more information about the item: owning people, step, workflow, ....


    }

    private XmlWorkflowItem retrieveWorkflowItem(Request request, Context context) throws SQLException, AuthorizeException, IOException {
        int workflowItemId = Util.getIntParameter(request, "wfiId");
        return xmlWorkflowItemService.find(context, workflowItemId);
    }
}
