/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.paymentsystem;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Date;
import java.util.Properties;

import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.log4j.Logger;
import org.dspace.app.util.SubmissionInfo;
import org.dspace.app.xmlui.cocoon.*;

import org.dspace.app.xmlui.utils.HandleUtil;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.*;
import org.dspace.authorize.AuthorizeException;

import org.dspace.content.*;
import org.dspace.core.ConfigurationManager;
import org.dspace.eperson.EPerson;
import org.dspace.paymentsystem.PaymentSystemConfigurationManager;
import org.dspace.paymentsystem.PaymentSystemException;
import org.dspace.paymentsystem.PaymentSystemService;
import org.dspace.paymentsystem.ShoppingCart;
import org.xml.sax.SAXException;
import org.dspace.utils.DSpace;

import javax.xml.parsers.DocumentBuilderFactory;


/**
 * Embedded IFrame Result Page, used in ShoppingCartTransformer to capture
 * Result of Paypal interaction.
 *
 * @author Mark Diggory, mdiggory at atmire.com
 * @author Fabio Bolognesi, fabio at atmire.com
 * @author Lantian Gai, lantian at atmire.com
 */
public class PayPalConfirmationTransformer extends AbstractDSpaceTransformer{

    private static final Logger log = Logger.getLogger(AbstractDSpaceTransformer.class);

//    protected static final Message T_Header=
//            message("xmlui.PaymentSystem.shoppingcart.order.header");


    public void addOptions(Options options) throws SAXException, org.dspace.app.xmlui.wing.WingException,
            UIException, SQLException, IOException, AuthorizeException {

        Request request = ObjectModelHelper.getRequest(objectModel);


        try{

            //add the order summary form
            List info = options.addList("PayPal",List.TYPE_FORM,"paypal");
            info.addItem().addContent(request.toString());

        }catch (Exception pe)
        {
            log.error("Exception: paypal:", pe);
        }
    }


    /** What to add at the end of the body */
    public void addBody(Body body) throws SAXException, WingException,
            UIException, SQLException, IOException, AuthorizeException
    {

        Division form = body.addDivision("form");
        form.addPara("Payflow transparent credit card processing - basic demo");

        String secureTokenId = DigestUtils.md5Hex(new Date().toString()); //"9a9ea8208de1413abc3d60c86cb1f4c5";;
        String secureToken = generateToken(secureTokenId);



        List formBody=form.addList("paypal-form",List.TYPE_FORM,"paypal");

//        formBody.addLabel("secure information");
//
        formBody.addItem("SECURETOKENID","SECURETOKENID").addText("SECURETOKENID").setValue(secureTokenId);
        formBody.addItem("SECURETOKEN","SECURETOKEN").addText("SECURETOKEN").setValue(secureToken);

        formBody.addItem("SILENTTRAN","SILENTTRAN").addText("SILENTTRAN").setValue("TRUE");
//


        formBody.addItem("VERBOSITY","VERBOSITY").addText("VERBOSITY").setValue("HIGH");

        formBody.addLabel("login information");
        formBody.addItem("PARTNER","PARTNER").addText("PARTNER").setValue("PayPal");
        formBody.addItem().addText("VENDOR").setValue(ConfigurationManager.getProperty("paypal.vendor"));
        formBody.addItem().addText("USER").setValue(ConfigurationManager.getProperty("paypal.user"));
        formBody.addItem().addText("PWD").setValue(ConfigurationManager.getProperty("paypal.pwd"));
        formBody.addItem().addText("TENDER").setValue("C");
        formBody.addItem().addText("TRXTYPE").setValue("A");
        formBody.addItem().addText("CURRENCY").setValue("USD");
        formBody.addItem().addText("AMT").setValue("0.00");


        formBody.addLabel("customer info");
        formBody.addItem().addText("CREDITCARD").setValue("5105105105105100");
        formBody.addItem().addText("EXPDATE").setValue("1214");
        formBody.addItem().addText("CVV2").setValue("123");
        formBody.addItem().addText("BILLTOFIRSTNAME").setValue("John");
        formBody.addItem().addText("BILLTOLASTNAME").setValue("Doe");
        formBody.addItem().addText("BILLTOSTREET").setValue("123 Main St.");
        formBody.addItem().addText("BILLTOCITY").setValue("San Jose");

        formBody.addItem().addText("BILLTOSTATE").setValue("CA");
        formBody.addItem().addText("BILLTOZIP").setValue("95101");
        formBody.addItem().addText("BILLTOCOUNTRY").setValue("US");





        formBody.addItem().addButton("Pay Now");

        Request request = ObjectModelHelper.getRequest(objectModel);
        Division info = body.addDivision("PayPal", "paypal");
        info.addPara("request from paypal");

        info.addPara(request.getRequestURI());
        info.addPara(request.getAttributes().toString());
        info.addPara(request.getParameters().toString());


    }

    private static String generateToken(String secureTokenId)
    {
        String requestUrl = "https://pilot-payflowpro.paypal.com/";
        String secureToken=null;
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            javax.xml.parsers.DocumentBuilder db = dbf.newDocumentBuilder();
            PostMethod get = new PostMethod(requestUrl);

            get.addParameter("SECURETOKENID",secureTokenId);
            get.addParameter("CREATESECURETOKEN","Y");
            get.addParameter("SILENTTRAN","TRUE");
            get.addParameter("MODE","TEST");

            get.addParameter("PARTNER","PayPal");
            get.addParameter("VENDOR",ConfigurationManager.getProperty("paypal.vendor"));
            get.addParameter("USER",ConfigurationManager.getProperty("paypal.user"));
            get.addParameter("PWD", ConfigurationManager.getProperty("paypal.pwd"));

            get.addParameter("TENDER", "C");
            //setup the reference transaction
            get.addParameter("TRXTYPE", "A");
            get.addParameter("AMT", "0");
            //TODO:add currency from shopping cart
            get.addParameter("CURRENCY", "USD");
            switch (new HttpClient().executeMethod(get)) {
                case 200:
                case 201:
                case 202:
                    String string = get.getResponseBodyAsString();
                    String[] results = string.split("&");
                    for(String temp:results)
                    {
                        String[] result = temp.split("=");
                        if(result[0].contains("RESULT")&&!result[1].equals("0"))
                        {
                            //failed to get a secure token
                            log.error("Failed to get a secure token from paypal:"+string);
                            log.error("Failed to get a secure token from paypal:"+get);
                            break;
                        }
                        if(result[0].equals("SECURETOKEN"))
                        {
                            secureToken=result[1];
                        }
                    }


                    break;
                default:
                    log.error("get paypal secure token error");
            }

            get.releaseConnection();
        }
        catch (Exception e) {
            log.error("get paypal secure token error:"+e);
        }
          return secureToken;
    }
}
