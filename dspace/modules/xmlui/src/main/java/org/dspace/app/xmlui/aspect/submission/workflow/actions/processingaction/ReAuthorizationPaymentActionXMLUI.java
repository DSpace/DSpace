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
import org.dspace.app.xmlui.aspect.submission.workflow.AbstractXMLUIAction;
import org.dspace.app.xmlui.utils.HandleUtil;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.*;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.paymentsystem.PaymentService;
import org.dspace.paymentsystem.PaymentServiceImpl;
import org.dspace.utils.DSpace;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.sql.SQLException;

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
        PaymentService paymentService = new DSpace().getSingletonService(PaymentService.class);
        String actionURL = contextPath + "/handle/"+collection.getHandle() + "/workflow_new";
        Division mainDiv = body.addInteractiveDivision("submit-completed-dataset", actionURL, Division.METHOD_POST, "primary submission");
        //generate form
        paymentService.generateUserForm(context,mainDiv,actionURL,knot.getId(), PaymentServiceImpl.PAYPAL_SALE,request,item,dso);

    }


}
