/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.submission.submit;

import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.log4j.Logger;
import org.apache.tools.ant.util.StringUtils;
import org.dspace.app.util.SubmissionInfo;
import org.dspace.app.xmlui.aspect.submission.AbstractStep;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.HandleUtil;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.*;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.*;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.dspace.paymentsystem.*;
import org.dspace.submit.AbstractProcessingStep;
import org.dspace.utils.DSpace;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.SQLException;
import java.util.Date;

/**
 * CheckoutStep responsible for supplying interface for Paypal Checkout.
 *
 * @author Mark Diggory, mdiggory at atmire.com
 * @author Fabio Bolognesi, fabio at atmire.com
 * @author Lantian Gai, lantian at atmire.com
 */
public class CheckoutStep extends AbstractStep {
    private static final Message T_HEAD = message("xmlui.Submission.submit.Checkout.head");
    private static final Message T_HELP = message("xmlui.Submission.submit.Checkout.help");
    private static final Message T_TRAIL = message("xmlui.Submission.submit.Checkout.trail");
    private static final Message T_FINALIZE_BUTTON = message("xmlui.Submission.submit.OverviewStep.button.finalize");
    private static final Logger log = Logger.getLogger(AbstractDSpaceTransformer.class);
    @Override
    public void addBody(Body body) throws SAXException, WingException, UIException, SQLException, IOException, AuthorizeException {
        Collection collection = submission.getCollection();
        String actionURL = contextPath + "/handle/" + collection.getHandle() + "/submit/" + knot.getId() + ".continue";

        body.addDivision("step-link","step-link").addPara(T_TRAIL);

        Division helpDivision = body.addDivision("general-help","general-help");
        helpDivision.setHead(T_HEAD);
        helpDivision.addPara(T_HELP);

        Division mainDiv = body.addInteractiveDivision("submit-completed-dataset", actionURL, Division.METHOD_POST, "primary submission");

        Request request = ObjectModelHelper.getRequest(objectModel);
        SubmissionInfo submissionInfo=(SubmissionInfo)request.getAttribute("dspace.submission.info");
        org.dspace.content.Item item = submissionInfo.getSubmissionItem().getItem();
        DSpaceObject dso = HandleUtil.obtainHandle(objectModel);
        PaypalService paypalService = new DSpace().getSingletonService(PaypalService.class);
        paypalService.generateUserForm(context,mainDiv,actionURL,knot.getId(),"A",request,item,dso);


    }







}