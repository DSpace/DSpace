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
import org.dspace.core.*;
import org.dspace.paymentsystem.PaymentSystemService;
import org.dspace.paymentsystem.ShoppingCart;
import org.dspace.utils.DSpace;
import org.dspace.workflow.*;
import org.dspace.workflow.actions.ActionResult;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.sql.SQLException;


/**
 *  Final action to complete the workflow transaction details for shopping cart
 *
 * @author Mark Diggory, mdiggory at atmire.com
 * @author Lantian Gai, lantian at atmire.com
 */
public class CompletePaymentAction extends ProcessingAction {

    private static Logger log = Logger.getLogger(DryadReviewAction.class);

    //this class sets all the correct date for shopping cart before item got archived

    @Override
    public void activate(Context c, WorkflowItem wfItem) {}

    @Override
    public ActionResult execute(Context c, WorkflowItem wfi, Step step, HttpServletRequest request) throws SQLException, AuthorizeException, IOException {
        int itemID =  wfi.getItem().getID();
        log.info("Completing payment status of Item " + itemID);

        try{
            PaymentSystemService paymentSystemService = new DSpace().getSingletonService(PaymentSystemService.class);
            ShoppingCart shoppingCart = paymentSystemService.getShoppingCartByItemId(c,itemID);

            shoppingCart.setStatus(ShoppingCart.STATUS_COMPLETED);
            if(shoppingCart.getPaymentDate()==null)
            {
                java.util.Date date = new java.util.Date();
                shoppingCart.setPaymentDate(date);
            }
            shoppingCart.update();
            c.commit();


        } catch (Exception e){
            log.error(e.getMessage(),e);
        }

        //let the item go throught no matter what happended in this step
        return new ActionResult(ActionResult.TYPE.TYPE_OUTCOME, ActionResult.OUTCOME_COMPLETE);

    }
}
