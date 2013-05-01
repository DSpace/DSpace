/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.paymentsystem;

import org.apache.cocoon.environment.Request;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.log4j.Logger;
import org.dspace.app.util.SubmissionInfo;
import org.dspace.app.xmlui.aspect.submission.FlowUtils;
import org.dspace.app.xmlui.utils.HandleUtil;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.*;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.*;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.submit.AbstractProcessingStep;
import org.dspace.utils.DSpace;
import org.dspace.workflow.WorkflowItem;

import javax.servlet.http.HttpServletRequest;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.dspace.app.xmlui.wing.Message;

/**
 *  Paypal Service for interacting with Payflow Pro API
 *
 * @author Mark Diggory, mdiggory at atmire.com
 * @author Fabio Bolognesi, fabio at atmire.com
 * @author Lantian Gai, lantian at atmire.com
 */
public class PaypalImpl implements PaypalService{

    protected Logger log = Logger.getLogger(PaypalService.class);

    public String getSecureTokenId(){
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmssSSSSSSSSSS");
       return sdf.format(new Date());


        //return DigestUtils.md5Hex(new Date().toString()); //"9a9ea8208de1413abc3d60c86cb1f4c5";
    }

    //generate a secure token from paypal
    public String generateSecureToken(ShoppingCart shoppingCart,String secureTokenId,String itemID, String type){
        String secureToken=null;
        String requestUrl = ConfigurationManager.getProperty("payment-system","paypal.payflow.link");

        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            javax.xml.parsers.DocumentBuilder db = dbf.newDocumentBuilder();
            PostMethod get = new PostMethod(requestUrl);

            get.addParameter("SECURETOKENID",secureTokenId);
            get.addParameter("CREATESECURETOKEN","Y");
            get.addParameter("MODE","TEST");
            get.addParameter("PARTNER","PayPal");

            get.addParameter("VENDOR",ConfigurationManager.getProperty("payment-system","paypal.vendor"));
            get.addParameter("USER",ConfigurationManager.getProperty("payment-system","paypal.user"));
            get.addParameter("PWD", ConfigurationManager.getProperty("payment-system","paypal.pwd"));
            //get.addParameter("RETURNURL", URLEncoder.encode("http://us.atmire.com:8080/submit-paypal-checkout"));
            if(ConfigurationManager.getProperty("payment-system","paypal.returnurl").length()>0)
            get.addParameter("RETURNURL", ConfigurationManager.getProperty("payment-system","paypal.returnurl"));
            get.addParameter("TENDER", "C");
            get.addParameter("TRXTYPE", type);
            if(type.equals("S")){
                get.addParameter("AMT", Double.toString(shoppingCart.getTotal()));
            }
            else
            {
                get.addParameter("AMT", "0");
            }
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
            return null;
        }

        return secureToken;
    }
    //charge the credit card stored as a reference transaction
    public boolean submitReferenceTransaction(Context c,WorkflowItem wfi,HttpServletRequest request){

        try{
            PaymentSystemService paymentSystemService = new DSpace().getSingletonService(PaymentSystemService.class);
            ShoppingCart shoppingCart = paymentSystemService.getTransactionByItemId(c,wfi.getItem().getID());
            if(shoppingCart.getVoucher()!=null&&ConfigurationManager.getProperty("payment-system","paypal.failed.voucher")!=null)
            {
                 if(shoppingCart.getVoucher().equals(ConfigurationManager.getProperty("payment-system","paypal.failed.voucher")))
                 {
                     return false;
                 }
            }
            if(shoppingCart!=null){
                if(shoppingCart.getTotal()==0)
                {
                    //if the total is 0 , don't charge
                    return true;
                }
                else
                {
                    return chargeCard(c, wfi, request,shoppingCart);
                }
            }
            else{
                //todo:doesn't have a shopping cart for this item, should return it back to item submission process to create a shoppingcart
            }

        }catch (Exception e)
        {
            log.error("exception when submit reference transaction"+e.getMessage());
        }
        return false;
    }

    public boolean getReferenceTransaction(Context context,WorkflowItem workItem,HttpServletRequest request){
        //return verifyCreditCard
        verifyCreditCard(context,workItem.getItem(),request);
        //todo:debug to be true
        return true;
    }
    public boolean getReferenceTransaction(Context context,WorkspaceItem workItem,HttpServletRequest request){
        //return verifyCreditCard
        verifyCreditCard(context,workItem.getItem(),request);
        //todo:debug to be true
        return true;
    }

    //generate a reference transaction from paypal
    public boolean verifyCreditCard(Context context,Item item, HttpServletRequest request){


        ShoppingCart shoppingCart=null;

        String referenceId=null;
        String cardNumber = request.getParameter("CREDITCARD");
        String CVV2 = request.getParameter("CVV2");
        String expDate = request.getParameter("EXPDATE");
        String firstName = request.getParameter("BILLTOFIRSTNAME");
        String lastName = request.getParameter("BILLTOLASTNAME");
        String street = request.getParameter("BILLTOSTREET");
        String city = request.getParameter("BILLTOCITY");
        String country = request.getParameter("BILLTOCOUNTRY");
        String state = request.getParameter("BILLTOSTATE");
        String zip = request.getParameter("BILLTOZIP");

        String requestUrl = ConfigurationManager.getProperty("payment-system","paypal.link");
        try {
            String secureToken=request.getParameter("SECURETOKEN");
            String secureTokenId=request.getParameter("SECURETOKENID");
            PaymentSystemService paymentSystemService =  new DSpace().getSingletonService(PaymentSystemService.class);
            shoppingCart= paymentSystemService.getTransactionByItemId(context,item.getID());
            shoppingCart = paymentSystemService.getTransactionByItemId(context, item.getID());


            if(secureToken!=null&&secureTokenId!=null&&shoppingCart!=null){
                DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                javax.xml.parsers.DocumentBuilder db = dbf.newDocumentBuilder();
                PostMethod get = new PostMethod(requestUrl);

                get.addParameter("SECURETOKENID",secureTokenId);
                get.addParameter("SECURETOKEN",secureToken);

                get.addParameter("SILENTTRAN","TRUE");

                get.addParameter("PARTNER","PayPal");
                get.addParameter("VENDOR",ConfigurationManager.getProperty("payment-system","paypal.vendor"));
                get.addParameter("USER",ConfigurationManager.getProperty("payment-system","paypal.user"));
                get.addParameter("PWD", ConfigurationManager.getProperty("payment-system","paypal.pwd"));
                //setup the reference transaction
                get.addParameter("TENDER", "C");
                get.addParameter("TRXTYPE", "A");
                get.addParameter("VERBOSITY", "HIGH");
                get.addParameter("AMT", "0");
                get.addParameter("CREDITCARD",cardNumber);
                get.addParameter("CVV2",CVV2);
                get.addParameter("EXPDATE",expDate);
                get.addParameter("BILLTOFIRSTNAME",firstName);
                get.addParameter("BILLTOLASTNAME",lastName);
                get.addParameter("BILLTOSTREET",street);
                get.addParameter("BILLTOSTATE",state);
                get.addParameter("BILLTOCITY",city);
                get.addParameter("BILLTOCOUNTRY",country);
                get.addParameter("BILLTOZIP",zip);


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
                            //TODO: ignore the error from paypal server, add the error check after figure out the correct way to process the credit card info
//                        if(result[0].contains("RESULT")&&!result[1].equals("0"))
//                        {
                            //failed to pass the credit card authorization check
//                            log.error("Failed to get a reference transaction from paypal:"+string);
//                            log.error("Failed to get a reference transaction from paypal:"+get);
//                            return false;
//                        }
                            //always return true so we can go through the workflow
                            if(result[0].contains("PNREF"))
                            {
                                shoppingCart.setTransactionId(result[1]);
                                shoppingCart.update();
                                return true;
                            }
                        }
                        break;
                    default:
                        log.error("get paypal reference transaction");
                        return false;
                }

                get.releaseConnection();
            }
            else{
                log.error("get paypal reference transaction error or shopping cart error:"+secureToken+secureTokenId+shoppingCart);
                return false;
            }
        }
        catch (Exception e) {
            log.error("get paypal reference transaction:"+e);
            return false;
        }
        return false;
    }



    @Override
    public boolean chargeCard(Context c, WorkflowItem wfi, HttpServletRequest request, ShoppingCart shoppingCart) {
        //this method should get the reference code and submit it to paypal to do the actural charge process

        if(shoppingCart.getTransactionId()==null){
            return false;
        }
       // TRXTYPE=S&TENDER=C&PWD=x1y2z3&PARTNER=PayPal&VENDOR=SuperMerchant&USER=SuperMerchant&ORIGID=VXYZ01234567&AMT=34.00
        String requestUrl = ConfigurationManager.getProperty("payment-system","paypal.payflow.link");
        try {



            PostMethod get = new PostMethod(requestUrl);

            //setup the reference transaction
            get.addParameter("TENDER", "C");
            get.addParameter("TRXTYPE", "S");
            get.addParameter("PWD", ConfigurationManager.getProperty("payment-system","paypal.pwd"));
            get.addParameter("AMT", Double.toString(shoppingCart.getTotal()));
            get.addParameter("VENDOR",ConfigurationManager.getProperty("payment-system","paypal.vendor"));
            get.addParameter("PARTNER","PayPal");
            get.addParameter("USER", ConfigurationManager.getProperty("payment-system","paypal.user"));
            get.addParameter("ORIGID", shoppingCart.getTransactionId());

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
                        //TODO: ignore the error from paypal server, add the error check after figure out the correct way to process the credit card info
                        if(result[0].contains("RESULT")&&result[1].equals("0"))
                        {
                            //successfull
                            shoppingCart.setStatus(ShoppingCart.STATUS_COMPLETED);
                            shoppingCart.update();
                            return true;
                        }

                    }
                    break;
                default:
                    log.error("get paypal reference transaction");
                    return false;
            }

            get.releaseConnection();
        }
        catch (Exception e) {
            log.error("error when submit paypal reference transaction:"+e);
            return false;
        }
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void generatePaypalForm(Division maindiv,ShoppingCart shoppingCart,String actionURL,String type) throws WingException,SQLException {

        //return false if there is error in loading from paypal
        String secureTokenId = getSecureTokenId();
        String secureToken = generateSecureToken(shoppingCart,secureTokenId,Integer.toString(shoppingCart.getItem()),type);

        if(secureToken==null){
            addSkipPaymentButton(maindiv,"Secure token is null");

        }
        else{
            shoppingCart.setSecureToken(secureToken);
            shoppingCart.update();
            List list= maindiv.addDivision("paypal-iframe").addList("paypal-fields");
            list.addItem("secureTokenId","").addContent(secureTokenId);
            list.addItem("secureToken","").addContent(secureToken);
            list.addItem("link","").addContent(ConfigurationManager.getProperty("payment-system","paypal.link"));
        }
    }

    public void generateVoucherForm(Division form,ShoppingCart shoppingCart,String actionURL,String knotId) throws WingException{

        List list=form.addList("voucher-list");
        list.addLabel("Voucher Code");
        list.addItem().addText("voucher").setValue(shoppingCart.getVoucher());
        list.addItem().addButton("submit-voucher").setValue("Apply");

    }
    public void generateNoCostForm( Division actionsDiv,ShoppingCart shoppingCart, org.dspace.content.Item item,PaymentSystemConfigurationManager manager,PaymentSystemService paymentSystemService) throws WingException, SQLException {
        //Lastly add the finalize submission button

        Division finDiv = actionsDiv.addDivision("finalizedivision");

        finDiv.addPara("help-text", "bold").addContent("General help text");

        if(shoppingCart.getStatus().equals(ShoppingCart.STATUS_VERIFIED))
        {
            finDiv.addPara("data-label", "bold").addContent("Your payment information has been verified.");
        }
        if(shoppingCart.getStatus().equals(ShoppingCart.STATUS_COMPLETED))
        {
            finDiv.addPara("data-label", "bold").addContent("Your card has been charged.");
        }
        else if(shoppingCart.getTotal()==0)
        {
           finDiv.addPara("data-label", "bold").addContent("Your total due is 0.00.");
        }
        else
        {
            finDiv.addPara("data-label", "bold").addContent("You are not being charged");
        }


        finDiv.addHidden("show_button").setValue("submit next");
    }

    public void addSkipPaymentButton(Division mainDiv,String message)throws WingException{
        Division error = mainDiv.addDivision("error");
        error.addPara(message);
        error.addHidden("show_button").setValue("Skip payment and submit");
    }

    public void addButtons(Division mainDiv)throws WingException{
        List buttons = mainDiv.addList("paypal-form-buttons");
        Button skipButton = buttons.addItem().addButton("skip_payment");
        skipButton.setValue("Submit");
        Button cancleButton = buttons.addItem().addButton(AbstractProcessingStep.CANCEL_BUTTON);
        cancleButton.setValue("Cancel");

    }

    //this methord should genearte a secure token from paypal and then generate a user crsedit card form
    public void generateUserForm(Context context,Division mainDiv,String actionURL,String knotId,String type,Request request, Item item, DSpaceObject dso) throws WingException, SQLException{
        PaymentSystemConfigurationManager manager = new PaymentSystemConfigurationManager();
        PaymentSystemService payementSystemService = new DSpace().getSingletonService(PaymentSystemService.class);
        PaypalService paypalService = new DSpace().getSingletonService(PaypalService.class);

        try{
            //create new transaction or update transaction id with item
            String previous_email = request.getParameter("login_email");
            EPerson eperson = EPerson.findByEmail(context,previous_email);
                    ShoppingCart shoppingCart = getShoppingCart(context,dso,item,payementSystemService,eperson);
            if(request.getParameter("submit-voucher")!=null)
            {    //user is using the voucher code
                String voucherCode = request.getParameter("voucher");
                shoppingCart.setVoucher(voucherCode);
                shoppingCart.setTotal(payementSystemService.calculateTransactionTotal(context,shoppingCart,""));
                shoppingCart.update();
            }
            if(shoppingCart.getTotal()==0||shoppingCart.getStatus().equals(ShoppingCart.STATUS_COMPLETED))
            {
                paypalService.generateNoCostForm(mainDiv, shoppingCart,item, manager, payementSystemService);
            }
            else
            {

                Radio methodSelection = mainDiv.addDivision("paymeny-selection").addList("payment-method").addItem().addRadio("payment-method");
                methodSelection.addOption("voucher").addContent("Use voucher code");
                methodSelection.addOption("creditcard").addContent("Use credit card");
                Division voucher = mainDiv.addDivision("voucher");

                paypalService.generateVoucherForm(voucher,shoppingCart,actionURL,knotId);
                Division creditcard = mainDiv.addDivision("creditcard");
                paypalService.generatePaypalForm(creditcard,shoppingCart,actionURL,type);

            }


        }catch (Exception e)
        {
            //TODO: handle the exceptions
            paypalService.addSkipPaymentButton(mainDiv,"errors in generate the payment form");
            log.error("Exception when entering the checkout step:", e);
        }


        mainDiv.addHidden("submission-continue").setValue(knotId);
        mainDiv.addPara().addContent("NOTE : Proceed only if your submission is finalized. After submitting, a Dryad curator will review your submission. After this review, your data will be archived in Dryad, and your payment will be processed.");
        paypalService.addButtons(mainDiv);

    }
    private ShoppingCart getShoppingCart(Context context,DSpaceObject dso, Item item, PaymentSystemService payementSystemService,EPerson eperson) throws AuthorizeException, SQLException, PaymentSystemException, IOException {
        ShoppingCart transaction=null;
        if(item!=null){
            transaction = payementSystemService.getTransactionByItemId(context,item.getID());
            if(transaction==null)
            {
                //first time create the transaction
                transaction= payementSystemService.createNewTrasaction(context, dso,item.getID(), eperson.getID(), ShoppingCart.COUNTRY_US, ShoppingCart.CURRENCY_US, ShoppingCart.STATUS_OPEN);
            }

        }

        return transaction;
    }
}
