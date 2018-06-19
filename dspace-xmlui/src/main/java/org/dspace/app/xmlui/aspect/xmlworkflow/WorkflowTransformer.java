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
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.SourceResolver;
import org.apache.log4j.Logger;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Options;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.app.xmlui.wing.element.UserMeta;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.LogManager;
import org.dspace.xmlworkflow.XmlWorkflowFactoryImpl;
import org.dspace.xmlworkflow.factory.XmlWorkflowFactory;
import org.dspace.xmlworkflow.factory.XmlWorkflowServiceFactory;
import org.dspace.xmlworkflow.state.Step;
import org.dspace.xmlworkflow.state.Workflow;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;
import org.dspace.xmlworkflow.storedcomponents.service.XmlWorkflowItemService;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

/**
 * A class that will render the current action in which the given workflow item
 * is currently located
 *
 * @author Bram De Schouwer (bram.deschouwer at dot com)
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 */
public class WorkflowTransformer extends AbstractDSpaceTransformer {

    private AbstractXMLUIAction xmluiActionUI;
    private boolean authorized = true;


    private static Logger log = Logger.getLogger(WorkflowTransformer.class);

    protected XmlWorkflowItemService xmlWorkflowItemService = XmlWorkflowServiceFactory.getInstance().getXmlWorkflowItemService();
    protected XmlWorkflowFactory workflowFactory = XmlWorkflowServiceFactory.getInstance().getWorkflowFactory();


    @Override
    public void setup(SourceResolver resolver, Map objectModel, String src, Parameters parameters) throws ProcessingException, SAXException, IOException {
        super.setup(resolver, objectModel, src, parameters);
        authorized = true;
        try {
            String stepID = parameters.getParameter("stepID");
            String actionID = parameters.getParameter("actionID");
            int workflowID = parameters.getParameterAsInteger("workflowID");
            XmlWorkflowItem wfi = xmlWorkflowItemService.find(context, workflowID);
            Workflow wf = workflowFactory.getWorkflow(wfi.getCollection());

            Step step = wf.getStep(stepID);
            xmluiActionUI = (AbstractXMLUIAction) WorkflowXMLUIFactory.getActionInterface(actionID);
            authorized = step.getActionConfig(actionID).getProcessingAction().isAuthorized(context, ObjectModelHelper.getRequest(objectModel), wfi);

            if(xmluiActionUI != null)
                xmluiActionUI.setup(resolver, objectModel, src, parameters);
//            else
//                throw new ProcessingException("Step class is null!  We do not have a valid AbstractStep in " + this.transformerClassName + ". ");
        } catch (Exception e) {
            log.error(LogManager.getHeader(context, "error while setting up workflowtransformer", ""), e);
            throw new ProcessingException("Something went wrong while setting up the workflow");
        }
    }

    public void addBody(Body body) throws SAXException, WingException, SQLException, IOException, AuthorizeException {
        if(!authorized)
            throw new AuthorizeException("You are not authorized to perform this task");

        xmluiActionUI.addBody(body);
    }

    /** What to add to the options list */
    public void addOptions(Options options) throws SAXException, WingException,
            UIException, SQLException, IOException, AuthorizeException
    {
        //call addOptions for this step
    	xmluiActionUI.addOptions(options);
    }

    /** What user metadata to add to the document */
    public void addUserMeta(UserMeta userMeta) throws SAXException,
            WingException, UIException, SQLException, IOException,
            AuthorizeException
    {
    	//call addUserMeta for this step
    	xmluiActionUI.addUserMeta(userMeta);
    }

    /** What page metadata to add to the document */
    public void addPageMeta(PageMeta pageMeta) throws SAXException,
            WingException, UIException, SQLException, IOException,
            AuthorizeException
    {
    	//call addPageMeta for this step
    	xmluiActionUI.addPageMeta(pageMeta);
    }

    /**
     * Recycle
     */
    public void recycle() 
    {
        if(xmluiActionUI!=null)
        {
            this.xmluiActionUI.recycle();
            this.xmluiActionUI = null;
        }
        super.recycle();
    }
}
