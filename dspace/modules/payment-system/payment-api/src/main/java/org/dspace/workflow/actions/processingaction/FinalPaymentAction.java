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

            if(paypalService.submitReferenceTransaction(c,wfi,request)){
                return new ActionResult(ActionResult.TYPE.TYPE_OUTCOME, ActionResult.OUTCOME_COMPLETE);
            }


        } catch (Exception e){
            log.error(e.getMessage(),e);
        }

        //Send us to the re authorization of paypal payment
        return new ActionResult(ActionResult.TYPE.TYPE_OUTCOME, 1);

    }
}
