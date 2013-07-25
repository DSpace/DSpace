/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.workflow.actions.processingaction;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DCValue;
import org.dspace.content.Item;
import org.dspace.core.*;
import org.dspace.paymentsystem.PaymentSystemService;
import org.dspace.paymentsystem.PaypalService;
import org.dspace.paymentsystem.ShoppingCart;
import org.dspace.utils.DSpace;
import org.dspace.workflow.*;
import org.dspace.workflow.actions.ActionResult;
import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.sql.SQLException;


/**
 *  This action completes the final payment if the shopping cart has a valid
 *  transaction id present. In the event htat the transaction should fail, this step will
 *  forward the user to the ReAuthorizationPayment step, which will provide them with
 *  an opportunity to initiate a new payment.
 *
 * @author Mark Diggory, mdiggory at atmire.com
 * @author Fabio Bolognesi, fabio at atmire.com
 * @author Lantian Gai, lantian at atmire.com
 */
public class FinalPaymentAction extends ProcessingAction {

    private static Logger log = Logger.getLogger(DryadReviewAction.class);



    @Override
    public void activate(Context c, WorkflowItem wfItem) {}

    @Override
    public ActionResult execute(Context c, WorkflowItem wfi, Step step, HttpServletRequest request) throws SQLException, AuthorizeException, IOException {

        try{

            PaypalService paypalService = new DSpace().getSingletonService(PaypalService.class);

            boolean approved = paypalService.submitReferenceTransaction(c,wfi,request);
            if(approved){
                sendPaymentApprovedEmail(c, wfi.getSubmitter().getEmail(), wfi);
                return new ActionResult(ActionResult.TYPE.TYPE_OUTCOME, ActionResult.OUTCOME_COMPLETE);
            }
            else
            {
                sendPaymentRejectEmail(c, wfi.getSubmitter().getEmail(), wfi);
            }

            //Send us to the re authorization of paypal payment
            return new ActionResult(ActionResult.TYPE.TYPE_OUTCOME, 1);


        } catch (Exception e){
            sendPaymentRejectEmail(c, wfi.getSubmitter().getEmail(), wfi);
            //return new ActionResult(ActionResult.TYPE.TYPE_ERROR);
            //Send us to the re authorization of paypal payment
            return new ActionResult(ActionResult.TYPE.TYPE_OUTCOME, 1);
        }
    }


    private void sendPaymentApprovedEmail(Context c, String emailAddress, WorkflowItem wfi) throws IOException, SQLException {
        Email email = ConfigurationManager.getEmail(I18nUtil.getEmailFilename(c.getCurrentLocale(), "payment_approved"));

        email.addRecipient(emailAddress);


        email.addArgument(wfi.getItem().getName());
        try {
            // Send the email -- Unless the journal is Evolution
            // TODO: make this configurable for each journal
            DCValue journals[] = wfi.getItem().getMetadata("prism", "publicationName", null, Item.ANY);
            String journalName =  (journals.length >= 1) ? journals[0].value : null;
            email.addArgument(journalName == null ? "" : journalName);

            String contentPatch=ConfigurationManager.getProperty("dspace.url");
            String link=contentPatch+"reAuthorization?workflowId="+wfi.getID();
            email.addArgument(link);
            //may need this if this is approved by admin
            //email.addArgument(c.getCurrentUser().getFullName());


            if(journalName !=null && !journalName.equals("Evolution") && !journalName.equals("Evolution*")) {
                log.debug("sending payment approved");
                email.send();
            } else {
                log.debug("skipping payment approved; journal is " + journalName);
            }
        } catch (MessagingException e) {
            log.error(LogManager.getHeader(c, "Error while email submitter about payment approved submission", "WorkflowItemId: " + wfi.getID()), e);
        }
    }

    private void sendPaymentRejectEmail(Context c, String emailAddress, WorkflowItem wfi) throws IOException, SQLException {
        Email email = ConfigurationManager.getEmail(I18nUtil.getEmailFilename(c.getCurrentLocale(), "payment_rejected"));

        email.addRecipient(emailAddress);

        email.addArgument(wfi.getItem().getName());
        try {
            // Send the email -- Unless the journal is Evolution
            // TODO: make this configurable for each journal
            //Add collection name
            DCValue journals[] = wfi.getItem().getMetadata("prism", "publicationName", null, Item.ANY);
            String journalName =  (journals.length >= 1) ? journals[0].value : null;
            email.addArgument(journalName == null ? "" : journalName);


            String error="Payment Authorization expired";
            email.addArgument(error);
            //http://localhost:8080/handle/10255/3/workflow?workflowID=9249&stepID=reAuthorizationPayment&actionID=reAuthorizationPaymentAction
            String contentPatch=ConfigurationManager.getProperty("dspace.url");
            //todo: give the reauthorizationpaymentaction link to user or to admin
            String link = wfi.getItem().getName();
            //String link=contentPatch+wfi.getItem().getCollections()[0].getHandle()+"/workflow?workflowID="+wfi.getID()+"&stepID=reAuthorizationPayment&actionID=reAuthorizationPaymentAction";
            email.addArgument(link);

            //May need if the action is excuted bt administrator  :{4}   Name of the rejector
            // email.addArgument(c.getCurrentUser().getFullName());

            if(journalName !=null && !journalName.equals("Evolution") && !journalName.equals("Evolution*")) {
                log.debug("sending  payment rejected");
                email.send();
            } else {
                log.debug("skipping  payment rejected; journal is " + journalName);
            }
        } catch (MessagingException e) {
            log.error(LogManager.getHeader(c, "Error while email submitter about  payment rejected", "WorkflowItemId: " + wfi.getID()), e);
        }
    }

}
