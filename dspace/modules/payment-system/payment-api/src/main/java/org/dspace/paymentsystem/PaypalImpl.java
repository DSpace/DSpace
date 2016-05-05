/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.paymentsystem;

import edu.harvard.hul.ois.mets.helper.DateTime;
import org.apache.cocoon.environment.Request;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.util.SubmissionInfo;
import org.dspace.app.xmlui.aspect.submission.FlowUtils;
import org.dspace.app.xmlui.utils.HandleUtil;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.*;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.*;
import org.dspace.content.Item;
import org.dspace.core.*;
import org.dspace.eperson.EPerson;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.submit.AbstractProcessingStep;
import org.dspace.utils.DSpace;
import org.dspace.workflow.WorkflowItem;

import javax.mail.MessagingException;
import javax.net.ssl.HttpsURLConnection;
import javax.servlet.http.HttpServletRequest;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.Random;

import org.dspace.app.xmlui.wing.Message;

/**
 *  Paypal Service for interacting with Payflow Pro API
 *
 * @author Mark Diggory, mdiggory at atmire.com
 * @author Fabio Bolognesi, fabio at atmire.com
 * @author Lantian Gai, lantian at atmire.com
 */
public class PaypalImpl implements PaypalService{

    protected Logger log = Logger.getLogger(PaypalImpl.class);

    public String getSecureTokenId(){
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmssSSSSSSSSSS");
       return sdf.format(new Date());


        //return DigestUtils.md5Hex(new Date().toString()); //"9a9ea8208de1413abc3d60c86cb1f4c5";
    }

    //generate a secure token from paypal
    public String generateSecureToken(ShoppingCart shoppingCart,String secureTokenId, String type,Context context){
        String secureToken=null;
        String requestUrl = ConfigurationManager.getProperty("payment-system","paypal.payflow.link");

        try {
            Item item =Item.find(context, shoppingCart.getItem());
            String url = requestUrl;
            URL obj = new URL(url);
            HttpsURLConnection con = (HttpsURLConnection) obj.openConnection();

            //add reuqest header
            con.setRequestMethod("POST");
            con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");


            String userFirstName = "";
            String userLastName = "";
            String userEmail = "";
            String userName = "";
            try{

                userFirstName = item.getSubmitter().getFirstName();
                userLastName = item.getSubmitter().getLastName();
                userEmail = item.getSubmitter().getEmail();
                userName = item.getSubmitter().getFullName();
            }catch (Exception e)
            {
                log.error("cant get submitter's user name for paypal transaction");
            }
            String amount = "0.00";

            if(type.equals("S")){
                //generate reauthorization form
                amount = Double.toString(shoppingCart.getTotal());
            }

            String urlParameters ="SECURETOKENID="+secureTokenId+"&CREATESECURETOKEN=Y"+"&MODE="+ConfigurationManager.getProperty("payment-system","paypal.mode")+"&PARTNER="+ConfigurationManager.getProperty("payment-system","paypal.partner")+"&VENDOR="+ConfigurationManager.getProperty("payment-system","paypal.vendor")+"&USER="+ConfigurationManager.getProperty("payment-system","paypal.user")+"&PWD="+ConfigurationManager.getProperty("payment-system","paypal.pwd")+"&TENDER="+type+"&TRXTYPE="+type+"&FIRSTNAME="+userFirstName+"&LASTNAME="+userLastName+"&COMMENT1="+userName+"&COMMENT2="+userEmail+"&AMT="+amount+"&CURRENCY="+shoppingCart.getCurrency();

            // Send post request
            con.setDoOutput(true);
            DataOutputStream wr = new DataOutputStream(con.getOutputStream());
            wr.writeBytes(urlParameters);
            wr.flush();
            wr.close();

            int responseCode = con.getResponseCode();
            System.out.println("\nSending 'POST' request to URL : " + url);
            System.out.println("Post parameters : " + urlParameters);
            System.out.println("Response Code : " + responseCode);

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            String[] results = response.toString().split("&");
            for(String temp:results)
            {
                String[] result = temp.split("=");
                if(result[0].contains("RESULT")&&!result[1].equals("0"))
                {
                    //failed to get a secure token
                    log.error("Failed to get a secure token from paypal:"+response.toString());
                    break;
                }
                if(result[0].equals("SECURETOKEN"))
                {
                    secureToken=result[1];
                    break;
                }
            }


//            if(ConfigurationManager.getProperty("payment-system","paypal.returnurl").length()>0)
//            get.addParameter("RETURNURL", ConfigurationManager.getProperty("payment-system","paypal.returnurl"));
        }
        catch (Exception e) {
            log.error("get paypal secure token error:", e);
            return null;
        }

        return secureToken;
    }
    //charge the credit card stored as a reference transaction
    public boolean submitReferenceTransaction(Context c,WorkflowItem wfi,HttpServletRequest request){
        PaymentSystemService paymentSystemService = new DSpace().getSingletonService(PaymentSystemService.class);

        try{
            ShoppingCart shoppingCart = paymentSystemService.getShoppingCartByItemId(c,wfi.getItem().getID());
            if(shoppingCart.getStatus().equals(ShoppingCart.STATUS_COMPLETED)){
                //this shopping cart has already been charged
                return true;
            }
            Voucher voucher = Voucher.findById(c,shoppingCart.getVoucher());

	    // check whether we're using the special voucher that simulates "payment failed"
            if(voucher!=null&&ConfigurationManager.getProperty("payment-system","paypal.failed.voucher")!=null)
            {
                String failedVoucher = ConfigurationManager.getProperty("payment-system","paypal.failed.voucher");
                 if(voucher.getCode().equals(failedVoucher)||voucher.getStatus().equals(Voucher.STATUS_USED))
                 {
                     log.debug("problem: 'payment failed' voucher has been used, rejecting payment");
                     paymentSystemService.sendPaymentErrorEmail(c, wfi, shoppingCart, "problem: voucher has been used, rejecting payment");
                     return false;
                 }
            }

            if(shoppingCart.getTotal()==0)
            {
                log.debug("shopping cart total is 0, not charging card");
                paymentSystemService.sendPaymentWaivedEmail(c, wfi, shoppingCart);
                //if the total is 0 , don't charge
                return true;
            }
            else
            {
                log.debug("charging card");
                return chargeCard(c, wfi, request,shoppingCart);
            }

        } catch (Exception e) {
            paymentSystemService.sendPaymentErrorEmail(c, wfi, null, "exception when submitting reference transaction " + e.getMessage());
            log.error("exception when submiting reference transaction ", e);
        }
        return false;
    }

    @Override
    public boolean chargeCard(Context c, WorkflowItem wfi, HttpServletRequest request, ShoppingCart shoppingCart) {
        PaymentSystemService paymentSystemService = new DSpace().getSingletonService(PaymentSystemService.class);
//this method should get the reference code and submit it to paypal to do the actural charge process

        if(shoppingCart.getTransactionId()==null){
            String transactionIdAbsentError = "transaction id absent, cannot charge card";
            log.debug(transactionIdAbsentError);
            paymentSystemService.sendPaymentErrorEmail(c, wfi, shoppingCart, transactionIdAbsentError);
            return false;
        }
        if(shoppingCart.getStatus().equals(ShoppingCart.STATUS_COMPLETED))
        {

            //all ready changed
            return true;
        }

        String requestUrl = ConfigurationManager.getProperty("payment-system", "paypal.payflow.link");
        try {
            PostMethod post = new PostMethod(requestUrl);

            //setup the reference transaction
            post.addParameter("TENDER", "C");
            post.addParameter("TRXTYPE", "S");
            post.addParameter("PWD", ConfigurationManager.getProperty("payment-system","paypal.pwd"));
            post.addParameter("AMT", Double.toString(shoppingCart.getTotal()));
            post.addParameter("VENDOR",ConfigurationManager.getProperty("payment-system","paypal.vendor"));
            post.addParameter("PARTNER",ConfigurationManager.getProperty("payment-system","paypal.partner"));
            post.addParameter("USER", ConfigurationManager.getProperty("payment-system","paypal.user"));
            post.addParameter("ORIGID", shoppingCart.getTransactionId());

            //TODO:add currency from shopping cart
            post.addParameter("CURRENCY", shoppingCart.getCurrency());

            Item item = Item.find(c,shoppingCart.getItem());

            String userFirstName = "";

            String userLastName = "";

            String userEmail = "";

            String userName = "";

            try{
                userFirstName = item.getSubmitter().getFirstName();

                userLastName = item.getSubmitter().getLastName();

                userEmail = item.getSubmitter().getEmail();

                userName = item.getSubmitter().getFullName();

            }catch (Exception e)

            {

                log.error("cant get submitter's user name for paypal transaction");

            }

            post.addParameter("FIRSTNAME",userFirstName);

            post.addParameter("LASTNAME",userLastName);

            post.addParameter("COMMENT1","Submitter's name :"+userName);

            post.addParameter("COMMENT2","Submitter's email :"+userEmail);




            log.debug("paypal sale transaction url " + post);
            switch (new HttpClient().executeMethod(post)) {
                case 200:
                case 201:
                case 202:
                    String string = post.getResponseBodyAsString();
                    String[] results = string.split("&");
                    for(String temp:results)
                    {
                        String[] result = temp.split("=");
                        //TODO: ignore the error from paypal server, add the error check after figure out the correct way to process the credit card info
                        if(result[0].contains("RESULT")&&result[1].equals("0"))
                        {
                            //successfull
                            shoppingCart.setStatus(ShoppingCart.STATUS_COMPLETED);
                            Date date= new Date();
                            shoppingCart.setPaymentDate(date);
                            for(String s:results)
                            {
                                String[] strings = s.split("=");
                                if(strings[0].contains("PNREF"))
                                {
                                    shoppingCart.setTransactionId(strings[1]);
                                    break;
                                }
                            }

                            shoppingCart.update();
                            paymentSystemService.sendPaymentApprovedEmail(c, wfi, shoppingCart);
                            return true;
                        }

                    }
                    break;
                default:
                    String result = "Paypal Reference Transaction Failure: "
                            + post.getStatusCode() +  ": " + post.getResponseBodyAsString();
                    log.error(result);
                    paymentSystemService.sendPaymentRejectedEmail(c, wfi, shoppingCart);
                    return false;
            }

            post.releaseConnection();
        }
        catch (Exception e) {
            log.error("error when submit paypal reference transaction: "+e.getMessage(), e);
            paymentSystemService.sendPaymentErrorEmail(c, wfi, null, "exception when submit reference transaction: " + e.getMessage());
            return false;
        }
        paymentSystemService.sendPaymentErrorEmail(c, wfi, shoppingCart, "chargeCard failed");
        return false;
    }

    public void generatePaypalForm(Division maindiv,ShoppingCart shoppingCart,String actionURL,String type,Context context) throws WingException,SQLException {

        //return false if there is error in loading from paypal
        String secureTokenId = getSecureTokenId();
        String secureToken = generateSecureToken(shoppingCart,secureTokenId,type,context);
        WorkspaceItem workspaceItem=null;
        if(secureToken==null){
            EPerson ePerson = context.getCurrentUser();
            try{
                workspaceItem=WorkspaceItem.findByItemId(context,shoppingCart.getItem());
            }
            catch (Exception e)
            {
                log.error("couldn't find the item in the workspace, so block peopele other than admmin"+e);
            }
            if(workspaceItem!=null||AuthorizeManager.isAdmin(context,ePerson))
            {
                showSkipPaymentButton(maindiv,"Unfortunately, Dryad has encountered a problem communicating with our payment processor. Please continue, and we will contact you regarding payment. Error code: Secure-null");

            }
            else
            {
                //don't show the skip button if item is not in workspace steps and not admin users
                showHelpPaymentButton(maindiv,"Unfortunately, Dryad has encountered a problem communicating with our payment processor. Please contact administrator regarding payment. Error code: Secure-null");

            }
            log.error("PayPal Secure Token is null");

        }
        else{
            shoppingCart.setSecureToken(secureToken);
            shoppingCart.update();
            List list= maindiv.addDivision("paypal-iframe").addList("paypal-fields");
            list.addItem("secureTokenId","").addContent(secureTokenId);
            list.addItem("secureToken","").addContent(secureToken);
            list.addItem("testMode","").addContent(ConfigurationManager.getProperty("payment-system","paypal.mode"));
            list.addItem("link","").addContent(ConfigurationManager.getProperty("payment-system","paypal.link"));
        }
    }

    public void generateVoucherForm(Division form,String voucherCode,String actionURL,String knotId) throws WingException{

        List list = form.addList("voucher-list");
        list.addLabel("Voucher Code");
        list.addItem().addText("voucher").setValue(voucherCode);
        list.addItem().addButton("submit-voucher").setValue("Apply");

    }

    public void generateNoCostForm( Division actionsDiv,ShoppingCart shoppingCart, org.dspace.content.Item item,PaymentSystemConfigurationManager manager,PaymentSystemService paymentSystemService) throws WingException, SQLException {
        //Lastly add the finalize submission button

        Division finDiv = actionsDiv.addDivision("finalizedivision");

        if(shoppingCart.getStatus().equals(ShoppingCart.STATUS_VERIFIED))
        {
            finDiv.addPara("data-label", "bold").addContent("Your payment information has been verified.");
        }
        if(shoppingCart.getStatus().equals(ShoppingCart.STATUS_COMPLETED))
        {
            finDiv.addPara("data-label", "bold").addContent("You have already paid for this submission.");
        }
        else if(shoppingCart.getTotal()==0)
        {
           finDiv.addPara("data-label", "bold").addContent("Your total due is 0.00.");
        }
        else if(!shoppingCart.getCurrency().equals("USD"))
        {
            finDiv.addPara("data-label", "bold").addContent("Dryad's payment processing system currently only supports transactions in US dollars. We expect to enable transactions in other currencies within a few days. If you wish to complete your transaction in US dollars, please change the currency setting above. Otherwise, please complete your submission without entering payment information. We will contact you for payment details before your data is published.");
        }
        else
        {
            finDiv.addPara("data-label", "bold").addContent("You are not being charged");
        }


        finDiv.addHidden("show_button").setValue("Finalize and submit data for curation");
    }

    public void showSkipPaymentButton(Division mainDiv,String message)throws WingException{
        Division error = mainDiv.addDivision("error");
        error.addPara(message);
        error.addHidden("show_button").setValue("Skip payment and submit");
    }

    public void showHelpPaymentButton(Division mainDiv,String message)throws WingException{
        Division error = mainDiv.addDivision("error");
        error.addPara(message);
        //error.addHidden("show_button").setValue("Skip payment and submit");
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
        //mainDiv.setHead("Checkout");
        String errorMessage = request.getParameter("encountError");
        try{
            //create new transaction or update transaction id with item
            String previous_email = request.getParameter("login_email");
            EPerson eperson = EPerson.findByEmail(context,previous_email);
            ShoppingCart shoppingCart = payementSystemService.getShoppingCartByItemId(context,item.getID());
            if(shoppingCart.getStatus().equals(ShoppingCart.STATUS_COMPLETED))
            {
                  //shopping cart already paid, not need to generate a form
                paypalService.generateNoCostForm(mainDiv, shoppingCart,item, manager, payementSystemService);
            }
            else{

            VoucherValidationService voucherValidationService = new DSpace().getSingletonService(VoucherValidationService.class);
            String voucherCode = "";
            if(request.getParameter("submit-voucher")!=null)
            {    //user is using the voucher code
                voucherCode = request.getParameter("voucher");
                if(voucherCode!=null&&voucherCode.length()>0){
                    if(!voucherValidationService.voucherUsed(context,voucherCode)) {
                        Voucher voucher = Voucher.findByCode(context,voucherCode);
                        shoppingCart.setVoucher(voucher.getID());
                        payementSystemService.updateTotal(context,shoppingCart,null);
                    }
                    else
                    {
                        errorMessage = "The voucher code is not valid:can't find the voucher code or the voucher code has been used";
                    }
                }
                else
                {
                    shoppingCart.setVoucher(null);
                    payementSystemService.updateTotal(context,shoppingCart,null);
                }

            }

            if(shoppingCart.getTotal()==0||shoppingCart.getStatus().equals(ShoppingCart.STATUS_COMPLETED)||!shoppingCart.getCurrency().equals("USD"))
            {
                paypalService.generateNoCostForm(mainDiv, shoppingCart,item, manager, payementSystemService);
            }
            else
            {

                Division voucher = mainDiv.addDivision("voucher");
                if(errorMessage!=null&&errorMessage.length()>0) {
                    voucher.addPara("voucher-error","voucher-error").addHighlight("bold").addContent(errorMessage);

                }

                Voucher voucher1 = Voucher.findById(context,shoppingCart.getVoucher());
                if(voucher1!=null){
                    paypalService.generateVoucherForm(voucher,voucher1.getCode(),actionURL,knotId);
                }
                else if(voucherCode!=null&&voucherCode.length()>0){
                    paypalService.generateVoucherForm(voucher,voucherCode,actionURL,knotId);
                }
                else{
                    paypalService.generateVoucherForm(voucher,null,actionURL,knotId);
                }
                Division creditcard = mainDiv.addDivision("creditcard");
                paypalService.generatePaypalForm(creditcard,shoppingCart,actionURL,type,context);

            }

            }
        }catch (Exception e)
        {
            //TODO: handle the exceptions
            paypalService.showSkipPaymentButton(mainDiv,"errors in generate the payment form:"+e.getMessage());
            log.error("Exception when entering the checkout step:", e);
        }


        mainDiv.addHidden("submission-continue").setValue(knotId);
        mainDiv.addPara().addContent("NOTE : Proceed only if your submission is finalized. After submitting, a Dryad curator will review your submission. After this review, your data will be archived in Dryad, and your payment will be processed.");
        paypalService.addButtons(mainDiv);

    }

}
