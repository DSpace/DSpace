/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.xmlworkflow;

import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.environment.SourceResolver;
import org.apache.log4j.Logger;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.HandleUtil;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.app.xmlui.wing.element.ReferenceSet;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.core.LogManager;
import org.dspace.xmlworkflow.factory.XmlWorkflowServiceFactory;
import org.dspace.xmlworkflow.state.actions.ActionInterface;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

/**
 * This abstract class represents an abstract action in the xml workflow
 * This class provides a placeholder for some utility methods
 *
 * @author Bram De Schouwer (bram.deschouwer at dot com)
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 */
public abstract class AbstractXMLUIAction extends AbstractDSpaceTransformer implements ActionInterface {

    protected static Logger log = Logger.getLogger(AbstractXMLUIAction.class);


    protected static final Message T_dspace_home =
        message("xmlui.general.dspace_home");

    protected static final Message T_showfull =
        message("xmlui.Submission.general.showfull");
    protected static final Message T_showsimple =
            message("xmlui.Submission.general.showsimple");

    protected static final Message T_workflow_title =
        message("xmlui.Submission.general.workflow.title");

    protected static final Message T_workflow_trail =
        message("xmlui.Submission.general.workflow.trail");

    protected static final Message T_workflow_head =
        message("xmlui.Submission.general.workflow.head");

    /**
     * The current DSpace SubmissionInfo
     */
    protected XmlWorkflowItem workflowItem;

    @Override
    public void setup(SourceResolver resolver, Map objectModel, String src, Parameters parameters) throws ProcessingException, SAXException, IOException {
        super.setup(resolver, objectModel, src, parameters);

        int id = parameters.getParameterAsInteger("workflowID", -1);
        try {
            workflowItem = XmlWorkflowServiceFactory.getInstance().getXmlWorkflowItemService().find(context, id);
        } catch (SQLException e) {
            log.error(LogManager.getHeader(context, "Error while retrieving workflowitem", "workflowitemid: " + id), e);
            throw new ProcessingException("Error while retrieving workflowitem", e);
        }
    }

    @Override
    public void addPageMeta(PageMeta pageMeta) throws SAXException, WingException, UIException, SQLException, IOException, AuthorizeException {
        super.addPageMeta(pageMeta);

        pageMeta.addMetadata("title").addContent(T_workflow_title);

        Collection collection = workflowItem.getCollection();

        pageMeta.addTrailLink(contextPath + "/",T_dspace_home);
        HandleUtil.buildHandleTrail(context, collection,pageMeta,contextPath, true);
        pageMeta.addTrail().addContent(T_workflow_trail);

    }

    @Override
    public abstract void addBody(Body body) throws SAXException, WingException,
            SQLException, IOException, AuthorizeException;

    protected void addWorkflowItemInformation(Division div, Item item, Request request) throws WingException {
        String showfull = request.getParameter("submit_full_item_info");

        // if the user selected showsimple, remove showfull.
        if (showfull != null && request.getParameter("submit_simple_item_info") != null)
            showfull = null;

        if (showfull == null)
        {
	        ReferenceSet referenceSet = div.addReferenceSet("narf",ReferenceSet.TYPE_SUMMARY_VIEW);
	        referenceSet.setHead(T_workflow_head);
            referenceSet.addReference(item);
	        div.addPara().addButton("submit_full_item_info").setValue(T_showfull);
        }
        else
        {
            ReferenceSet referenceSet = div.addReferenceSet("narf", ReferenceSet.TYPE_DETAIL_VIEW);
            referenceSet.setHead(T_workflow_head);
            referenceSet.addReference(item);
            div.addPara().addButton("submit_simple_item_info").setValue(T_showsimple);

            div.addHidden("submit_full_item_info").setValue("true");
        }
    }
}
