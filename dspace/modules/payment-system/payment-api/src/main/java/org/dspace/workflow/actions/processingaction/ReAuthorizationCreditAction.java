package org.dspace.workflow.actions.processingaction;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.core.*;
import org.dspace.paymentsystem.PaymentSystemService;
import org.dspace.paymentsystem.ShoppingCart;
import org.dspace.utils.DSpace;
import org.dspace.workflow.Step;
import org.dspace.workflow.WorkflowItem;
import org.dspace.workflow.actions.ActionResult;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.sql.SQLException;


/**
 * User: lantian @ atmire . com
 * Date: 7/28/14
 * Time: 4:26 PM
 */
public class ReAuthorizationCreditAction extends ProcessingAction  {
    private static Logger log = Logger.getLogger(DryadReviewAction.class);



    @Override
    public void activate(Context c, WorkflowItem wfItem) {
        boolean test=true;
        log.debug("here");
    }

    @Override
    public ActionResult execute(Context c, WorkflowItem wfi, Step step, HttpServletRequest request) throws SQLException, AuthorizeException, IOException {

        try{
            Item item = wfi.getItem();
            PaymentSystemService paymentSystemService = new DSpace().getSingletonService(PaymentSystemService.class);
            ShoppingCart shoppingCart = paymentSystemService.getShoppingCartByItemId(c,item.getID());
            if(shoppingCart.getStatus().equals(ShoppingCart.STATUS_COMPLETED))
            {
                return new ActionResult(ActionResult.TYPE.TYPE_OUTCOME, ActionResult.OUTCOME_COMPLETE);
            }


        } catch (Exception e){
            log.error(e.getMessage(),e);
        }
        return new ActionResult(ActionResult.TYPE.TYPE_OUTCOME, 1);

    }




}
