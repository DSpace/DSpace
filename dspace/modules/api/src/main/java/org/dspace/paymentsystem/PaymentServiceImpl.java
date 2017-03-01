/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 * <p>
 * http://www.dspace.org/license/
 */
package org.dspace.paymentsystem;

import org.apache.cocoon.environment.Request;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.lang.StringUtils;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.apache.log4j.Logger;
import org.datadryad.api.DryadFunderConcept;
import org.datadryad.api.DryadOrganizationConcept;
import org.dspace.JournalUtils;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Button;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.DCValue;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.authority.Choices;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.submit.AbstractProcessingStep;
import org.dspace.utils.DSpace;
import org.dspace.workflow.DryadWorkflowUtils;
import org.dspace.workflow.WorkflowItem;

import javax.net.ssl.HttpsURLConnection;
import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import static org.dspace.app.xmlui.wing.AbstractWingTransformer.message;

/**
 *  Paypal Service for interacting with Payflow Pro API
 *
 * @author Mark Diggory, mdiggory at atmire.com
 * @author Fabio Bolognesi, fabio at atmire.com
 * @author Lantian Gai, lantian at atmire.com
 */
public class PaymentServiceImpl implements PaymentService {
    private static final Message T_funding_head = message("xmlui.submit.select.funding.head");
    private static final Message T_HEAD = message("xmlui.Submission.submit.Checkout.head");
    private static final Message T_HELP = message("xmlui.Submission.submit.Checkout.help");
    private static final Message T_funding_question = message("xmlui.Submission.submit.CheckoutStep.funding.question");
    private static final Message T_funding_valid = message("xmlui.Submission.submit.CheckoutStep.funding.valid");
    private static final Message T_funding_error = message("xmlui.Submission.submit.CheckoutStep.funding.error");
    private static final Message T_button_finalize = message("xmlui.Submission.submit.CheckoutStep.button.finalize");
    private static final Message T_button_proceed = message("xmlui.Submission.submit.CheckoutStep.button.proceed");

    private static final Message T_funding_desc1 = message("xmlui.Submission.submit.CheckoutStep.funding.desc1");
    private static final Message T_funding_desc2 = message("xmlui.submit.select.funding.desc2");

    private static final Message T_payment_note = message("xmlui.Submission.submit.Checkout.note");
    private static final Message T_voucher_error = message("xmlui.Submission.submit.Checkout.voucher.error");
    protected Logger log = Logger.getLogger(PaymentServiceImpl.class);

    private static final String PAYMENT_ERROR_VOUCHER = "voucher error";
    private static final String PAYMENT_ERROR_GRANT = "grant error";

    public static final String PAYPAL_AUTHORIZE = "A";
    public static final String PAYPAL_SALE = "S";

    public String getSecureTokenId() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmssSSSSSSSSSS");
        return sdf.format(new Date());
    }

    //generate a secure token from paypal
    public String generateSecureToken(ShoppingCart shoppingCart, String secureTokenId, String transactionType, Context context) {
        String secureToken = null;
        ArrayList<BasicNameValuePair> queryParams = new ArrayList<BasicNameValuePair>();
        String requestUrl = ConfigurationManager.getProperty("payment-system", "paypal.payflow.link");

        try {
            Item item = Item.find(context, shoppingCart.getItem());
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
            try {

                userFirstName = item.getSubmitter().getFirstName();
                userLastName = item.getSubmitter().getLastName();
                userEmail = item.getSubmitter().getEmail();
                userName = item.getSubmitter().getFullName();
            } catch (Exception e) {
                log.error("can't get submitter's user name for paypal transaction");
            }
            String amount = "0.00";

            if (transactionType.equals(PAYPAL_SALE)) {
                //generate reauthorization form
                amount = Double.toString(shoppingCart.getTotal());
            }

            queryParams.add(new BasicNameValuePair("SECURETOKENID", secureTokenId));
            queryParams.add(new BasicNameValuePair("CREATESECURETOKEN", "Y"));
            queryParams.add(new BasicNameValuePair("MODE", ConfigurationManager.getProperty("payment-system", "paypal.mode")));
            queryParams.add(new BasicNameValuePair("PARTNER", ConfigurationManager.getProperty("payment-system", "paypal.partner")));
            queryParams.add(new BasicNameValuePair("VENDOR", ConfigurationManager.getProperty("payment-system", "paypal.vendor")));
            queryParams.add(new BasicNameValuePair("USER", ConfigurationManager.getProperty("payment-system", "paypal.user")));
            queryParams.add(new BasicNameValuePair("PWD", ConfigurationManager.getProperty("payment-system", "paypal.pwd")));
            queryParams.add(new BasicNameValuePair("TENDER", "C" ));
            queryParams.add(new BasicNameValuePair("TRXTYPE", transactionType));
            queryParams.add(new BasicNameValuePair("FIRSTNAME", userFirstName));
            queryParams.add(new BasicNameValuePair("LASTNAME", userLastName));
            queryParams.add(new BasicNameValuePair("COMMENT1", userName));
            queryParams.add(new BasicNameValuePair("COMMENT2", userEmail));
            queryParams.add(new BasicNameValuePair("AMT", amount));
            queryParams.add(new BasicNameValuePair("CURRENCY", shoppingCart.getCurrency()));
            String urlParameters = URLEncodedUtils.format(queryParams, "UTF-8");

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
            for (String temp : results) {
                String[] result = temp.split("=");
                if (result[0].contains("RESULT") && !result[1].equals("0")) {
                    //failed to get a secure token
                    log.error("Failed to get a secure token from paypal:" + response.toString());
                    break;
                }
                if (result[0].equals("SECURETOKEN")) {
                    secureToken = result[1];
                    break;
                }
            }


        } catch (Exception e) {
            log.error("get paypal secure token error:", e);
            return null;
        }

        return secureToken;
    }

    //charge the credit card stored as a reference transaction
    public boolean submitReferenceTransaction(Context c, WorkflowItem wfi, HttpServletRequest request) {
        PaymentSystemService paymentSystemService = new DSpace().getSingletonService(PaymentSystemService.class);

        try {
            ShoppingCart shoppingCart = paymentSystemService.getShoppingCartByItemId(c, wfi.getItem().getID());
            if (shoppingCart.getStatus().equals(ShoppingCart.STATUS_COMPLETED)) {
                //this shopping cart has already been charged
                return true;
            }
            Voucher voucher = Voucher.findById(c, shoppingCart.getVoucher());

            // check whether we're using the special voucher that simulates "payment failed"
            if (voucher != null && ConfigurationManager.getProperty("payment-system", "paypal.failed.voucher") != null) {
                String failedVoucher = ConfigurationManager.getProperty("payment-system", "paypal.failed.voucher");
                if (voucher.getCode().equals(failedVoucher) || voucher.getStatus().equals(Voucher.STATUS_USED)) {
                    log.debug("problem: 'payment failed' voucher has been used, rejecting payment");
                    paymentSystemService.sendPaymentErrorEmail(c, wfi, shoppingCart, "problem: voucher has been used, rejecting payment");
                    return false;
                }
            }

            if (shoppingCart.getTotal() == 0) {
                log.debug("shopping cart total is 0, not charging card");
                paymentSystemService.sendPaymentWaivedEmail(c, wfi, shoppingCart);
                //if the total is 0 , don't charge
                return true;
            } else {
                log.debug("charging card");
                return chargeCard(c, wfi, request, shoppingCart);
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

        if (shoppingCart.getTransactionId() == null) {
            String transactionIdAbsentError = "transaction id absent, cannot charge card";
            log.debug(transactionIdAbsentError);
            paymentSystemService.sendPaymentErrorEmail(c, wfi, shoppingCart, transactionIdAbsentError);
            return false;
        }
        if (shoppingCart.getStatus().equals(ShoppingCart.STATUS_COMPLETED)) {

            //all ready changed
            return true;
        }

        String requestUrl = ConfigurationManager.getProperty("payment-system", "paypal.payflow.link");
        try {
            PostMethod post = new PostMethod(requestUrl);

            //setup the reference transaction
            post.addParameter("TENDER", "C");
            post.addParameter("TRXTYPE", PaymentServiceImpl.PAYPAL_SALE);
            post.addParameter("PWD", ConfigurationManager.getProperty("payment-system", "paypal.pwd"));
            post.addParameter("AMT", Double.toString(shoppingCart.getTotal()));
            post.addParameter("VENDOR", ConfigurationManager.getProperty("payment-system", "paypal.vendor"));
            post.addParameter("PARTNER", ConfigurationManager.getProperty("payment-system", "paypal.partner"));
            post.addParameter("USER", ConfigurationManager.getProperty("payment-system", "paypal.user"));
            post.addParameter("ORIGID", shoppingCart.getTransactionId());

            //TODO:add currency from shopping cart
            post.addParameter("CURRENCY", shoppingCart.getCurrency());

            Item item = Item.find(c, shoppingCart.getItem());

            String userFirstName = "";

            String userLastName = "";

            String userEmail = "";

            String userName = "";

            try {
                userFirstName = item.getSubmitter().getFirstName();

                userLastName = item.getSubmitter().getLastName();

                userEmail = item.getSubmitter().getEmail();

                userName = item.getSubmitter().getFullName();

            } catch (Exception e)

            {

                log.error("cant get submitter's user name for paypal transaction");

            }

            post.addParameter("FIRSTNAME", userFirstName);

            post.addParameter("LASTNAME", userLastName);

            post.addParameter("COMMENT1", "Submitter's name :" + userName);

            post.addParameter("COMMENT2", "Submitter's email :" + userEmail);


            log.debug("paypal sale transaction url " + post);
            switch (new HttpClient().executeMethod(post)) {
                case 200:
                case 201:
                case 202:
                    String string = post.getResponseBodyAsString();
                    String[] results = string.split("&");
                    for (String temp : results) {
                        String[] result = temp.split("=");
                        //TODO: ignore the error from paypal server, add the error check after figure out the correct way to process the credit card info
                        if (result[0].contains("RESULT") && result[1].equals("0")) {
                            //successfull
                            log.debug("marking cart for item " + shoppingCart.getItem() +
                                      " complete, due to paypal result " + temp);
                            shoppingCart.setStatus(ShoppingCart.STATUS_COMPLETED);
                            Date date = new Date();
                            shoppingCart.setPaymentDate(date);
                            for (String s : results) {
                                String[] strings = s.split("=");
                                if (strings[0].contains("PNREF")) {
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
                            + post.getStatusCode() + ": " + post.getResponseBodyAsString();
                    log.error(result);
                    paymentSystemService.sendPaymentRejectedEmail(c, wfi, shoppingCart);
                    return false;
            }

            post.releaseConnection();
        } catch (Exception e) {
            log.error("error when submit paypal reference transaction: " + e.getMessage(), e);
            paymentSystemService.sendPaymentErrorEmail(c, wfi, null, "exception when submit reference transaction: " + e.getMessage());
            return false;
        }
        paymentSystemService.sendPaymentErrorEmail(c, wfi, shoppingCart, "chargeCard failed");
        return false;
    }

    public void generatePaypalForm(Division maindiv, ShoppingCart shoppingCart, String actionURL, String transactionType, Context context) throws WingException, SQLException {

        //return false if there is error in loading from paypal
        String secureTokenId = getSecureTokenId();
        String secureToken = generateSecureToken(shoppingCart, secureTokenId, transactionType, context);
        WorkspaceItem workspaceItem = null;
        if (secureToken == null) {
            EPerson ePerson = context.getCurrentUser();
            try {
                workspaceItem = WorkspaceItem.findByItemId(context, shoppingCart.getItem());
            } catch (Exception e) {
                log.error("couldn't find the item in the workspace, so block peopele other than admin" + e);
            }
            if (workspaceItem != null || AuthorizeManager.isAdmin(context, ePerson)) {
                showSkipPaymentButton(maindiv, "Unfortunately, Dryad has encountered a problem communicating with our payment processor. Please continue, and we will contact you regarding payment. Error code: Secure-null");
                shoppingCart.setNote("Paypal returned null secure token");
                shoppingCart.update();
            } else {
                //don't show the skip button if item is not in workspace steps and not admin users
                showHelpPaymentButton(maindiv, "Unfortunately, Dryad has encountered a problem communicating with our payment processor. Please contact administrator regarding payment. Error code: Secure-null");

            }
            log.error("PayPal Secure Token is null");

        } else {
            shoppingCart.setSecureToken(secureToken);
            shoppingCart.update();
            List list = maindiv.addDivision("paypal-iframe").addList("paypal-fields");
            list.addItem("secureTokenId", "").addContent(secureTokenId);
            list.addItem("secureToken", "").addContent(secureToken);
            list.addItem("testMode", "").addContent(ConfigurationManager.getProperty("payment-system", "paypal.mode"));
            list.addItem("link", "").addContent(ConfigurationManager.getProperty("payment-system", "paypal.link"));
        }
    }

    public void generateNoCostForm(Division actionsDiv, ShoppingCart shoppingCart, org.dspace.content.Item item, PaymentSystemConfigurationManager manager, PaymentSystemService paymentSystemService) throws WingException, SQLException {
        //Lastly add the finalize submission button

        Division finDiv = actionsDiv.addDivision("finalizedivision");

        if (shoppingCart.getStatus().equals(ShoppingCart.STATUS_VERIFIED)) {
            finDiv.addPara("data-label", "bold").addContent("Your payment information has been verified.");
        }
        if (shoppingCart.getStatus().equals(ShoppingCart.STATUS_COMPLETED)) {
            finDiv.addPara("data-label", "bold").addContent("Your DPC has been paid.");
        } else if (shoppingCart.getTotal() == 0) {
            finDiv.addPara("data-label", "bold").addContent("Your total due is $0.00.");
        } else {
            finDiv.addPara("data-label", "bold").addContent("You are not being charged");
        }


        finDiv.addHidden("show_button").setValue("Finalize and submit data for curation");
    }

    public void showSkipPaymentButton(Division mainDiv, String message) throws WingException {
        Division error = mainDiv.addDivision("error");
        error.addPara(message);
        error.addHidden("show_button").setValue("Skip payment and submit");
    }

    public void showHelpPaymentButton(Division mainDiv, String message) throws WingException {
        Division error = mainDiv.addDivision("error");
        error.addPara(message);
        //error.addHidden("show_button").setValue("Skip payment and submit");
    }

    public void addButtons(Division mainDiv) throws WingException {
        List buttons = mainDiv.addList("paypal-form-buttons");
        Button skipButton = buttons.addItem().addButton("skip_payment");
        skipButton.setValue("Submit");
        Button cancelButton = buttons.addItem().addButton(AbstractProcessingStep.CANCEL_BUTTON);
        cancelButton.setValue("Cancel");

    }

    //this method should generate a secure token from paypal and then generate a user credit card form
    public void generateUserForm(Context context, Division mainDiv, String actionURL, String knotId, String transactionType, Request request, Item item, DSpaceObject dso) throws WingException, SQLException {
        PaymentSystemConfigurationManager manager = new PaymentSystemConfigurationManager();
        PaymentSystemService paymentSystemService = new DSpace().getSingletonService(PaymentSystemService.class);
        ShoppingCart shoppingCart = null;
        Item dataPackage = DryadWorkflowUtils.getDataPackage(context, item);
        if (dataPackage != null) {
            item = dataPackage;
        }
        try {
            shoppingCart = paymentSystemService.getShoppingCartByItemId(context, item.getID());

            // look up any vouchers that have been applied:
            VoucherValidationService voucherValidationService = new DSpace().getSingletonService(VoucherValidationService.class);
            String voucherCode = null;
            Voucher voucher = Voucher.findById(context, shoppingCart.getVoucher());
            // if there's no voucher in the cart, check the request parameters for a voucher:
            if (voucher == null && request.getParameter("submit-voucher") != null) {
                voucherCode = request.getParameter("voucher");
                if (!voucherValidationService.voucherUsed(context, voucherCode)) {
                    voucher = Voucher.findByCode(context, voucherCode);
                    // put the voucher ID in the shopping cart so we can access it next time, if it's good.
                    shoppingCart.setVoucher(voucher.getID());
                }
                paymentSystemService.updateTotal(context, shoppingCart);
            }

            if (shoppingCart.getTotal() == 0 || shoppingCart.getStatus().equals(ShoppingCart.STATUS_COMPLETED)) {
                generateNoCostForm(mainDiv, shoppingCart, item, manager, paymentSystemService);
            } else {
                mainDiv.setHead(T_HEAD);
                mainDiv.addPara(T_HELP);
                Division creditcard = mainDiv.addDivision("creditcard");
                generatePaypalForm(creditcard, shoppingCart, actionURL, transactionType, context);
                mainDiv.addPara().addContent(T_payment_note);
            }
            mainDiv.addHidden("submission-continue").setValue(knotId);
            addButtons(mainDiv);
        } catch (Exception e) {
            //TODO: handle the exceptions
            showSkipPaymentButton(mainDiv, "errors in generating the payment form:" + e.getMessage());
            if (shoppingCart != null) {
                shoppingCart.setNote("Payment error: " + e.getMessage());
                shoppingCart.update();
            }
            log.error("Exception when entering the checkout step:", e);
            mainDiv.addHidden("submission-continue").setValue(knotId);
            addButtons(mainDiv);
        }
    }
}
