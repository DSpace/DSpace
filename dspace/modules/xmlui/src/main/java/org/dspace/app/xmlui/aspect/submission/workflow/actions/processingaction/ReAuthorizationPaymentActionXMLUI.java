/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.submission.workflow.actions.processingaction;

import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.dspace.app.xmlui.aspect.submission.workflow.AbstractXMLUIAction;
import org.dspace.app.xmlui.utils.HandleUtil;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.*;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.dspace.paymentsystem.PaymentSystemConfigurationManager;
import org.dspace.paymentsystem.PaymentSystemService;
import org.dspace.paymentsystem.PaypalService;
import org.dspace.paymentsystem.ShoppingCart;
import org.dspace.submit.AbstractProcessingStep;
import org.dspace.utils.DSpace;
import org.dspace.workflow.WorkflowItem;
import org.dspace.workflow.WorkflowManager;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Date;

/**
 *
 * XMLUI Interface for the Payment Reauthorization Step.
 *
 * @author Mark Diggory, mdiggory at atmire.com
 * @author Fabio Bolognesi, fabio at atmire.com
 * @author Lantian Gai, lantian at atmire.com
 */
public class ReAuthorizationPaymentActionXMLUI extends AbstractXMLUIAction {

    @Override
    public void addBody(Body body) throws SAXException, WingException, SQLException, IOException, AuthorizeException {
        Request request = ObjectModelHelper.getRequest(objectModel);
        String workflowID = request.getParameter("workflowID");
        String stepID = request.getParameter("stepID");
        String actionID = request.getParameter("actionID");

        Item item = workflowItem.getItem();
        DSpaceObject dso = HandleUtil.obtainHandle(objectModel);
        Collection collection = workflowItem.getCollection();
        PaypalService paypalService = new DSpace().getSingletonService(PaypalService.class);
        String actionURL = contextPath + "/handle/"+collection.getHandle() + "/workflow_new";
        Division mainDiv = body.addInteractiveDivision("submit-completed-dataset", actionURL, Division.METHOD_POST, "primary submission");
        //generate form
        paypalService.generateUserForm(context,mainDiv,actionURL,knot.getId(),"S",request,item,dso);

    }


}
