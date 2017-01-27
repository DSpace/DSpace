/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.workflow.actions.processingaction;

import org.apache.log4j.Logger;
import org.datadryad.anywhere.AssociationAnywhere;
import org.datadryad.api.DryadOrganizationConcept;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.*;
import org.dspace.identifier.DOIIdentifierProvider;
import org.dspace.paymentsystem.PaymentSystemService;
import org.dspace.paymentsystem.PaymentService;
import org.dspace.paymentsystem.ShoppingCart;
import org.dspace.paymentsystem.Voucher;
import org.dspace.utils.DSpace;
import org.dspace.workflow.*;
import org.dspace.workflow.actions.ActionResult;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Date;


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
        int itemID = wfi.getItem().getID();
        log.info("Verifying payment status of Item " + itemID);

        try {
            PaymentSystemService paymentSystemService = new DSpace().getSingletonService(PaymentSystemService.class);
            ShoppingCart shoppingCart = paymentSystemService.getShoppingCartByItemId(c, itemID);
            // if cart is marked as completed, don't process this again
            if (shoppingCart.getStatus().equals(ShoppingCart.STATUS_COMPLETED)) {
                log.info("no additional payment processed for item " + itemID + ", cart already marked as complete");
                return new ActionResult(ActionResult.TYPE.TYPE_OUTCOME, ActionResult.OUTCOME_COMPLETE);
            }

            // if fee waiver is in place, transaction is paid
            if (shoppingCart.getCountry() != null && shoppingCart.getCountry().length() > 0) {
                log.info("processed fee waiver for Item " + itemID + ", country = " + shoppingCart.getCountry() + ", marking cart complete");
                return new ActionResult(ActionResult.TYPE.TYPE_OUTCOME, ActionResult.OUTCOME_COMPLETE);
            }


            // if a valid voucher is in place, transaction is paid
            Voucher voucher = Voucher.findById(c, shoppingCart.getVoucher());
            log.debug("voucher is " + voucher);
            if (voucher != null) {
                log.debug("voucher status " + voucher.getStatus());
                if (voucher.getStatus().equals(Voucher.STATUS_OPEN)) {
                    voucher.setStatus(Voucher.STATUS_USED);
                    voucher.update();
                    c.commit();
                    log.info("processed voucher for Item " + itemID + ", voucherID = " + voucher.getID() + ", marking cart complete");
                    return new ActionResult(ActionResult.TYPE.TYPE_OUTCOME, ActionResult.OUTCOME_COMPLETE);
                }
            }

            DryadOrganizationConcept organizationConcept = shoppingCart.getSponsoringOrganization(c);
            // if journal-based subscription is in place, transaction is paid
            if (organizationConcept != null) {
                if (shoppingCart.hasSubscription()) {
                    log.info("processed journal subscription for Item " + itemID + ", journal = " +
                             organizationConcept.getFullName() + ", marking cart complete");
                    log.debug("tally credit for journal = " + organizationConcept.getFullName());

                    if (organizationConcept.getCustomerID() != null) {
                        try {
                            String packageDOI = DOIIdentifierProvider.getDoiValue(wfi.getItem());
                            shoppingCart.setStatus(ShoppingCart.STATUS_COMPLETED);
                            Date date = new Date();
                            shoppingCart.setPaymentDate(date);
                            shoppingCart.update();
                            AssociationAnywhere.tallyCredit(c, organizationConcept.getCustomerID(), packageDOI);
                            paymentSystemService.sendPaymentApprovedEmail(c, wfi, shoppingCart);
                            return new ActionResult(ActionResult.TYPE.TYPE_OUTCOME, ActionResult.OUTCOME_COMPLETE);
                        } catch (Exception e) {
                            log.error(e.getMessage(), e);
                            paymentSystemService.sendPaymentErrorEmail(c, wfi, shoppingCart, "problem: credit not tallied successfully. \\n \\n " + e.getMessage());
                            return new ActionResult(ActionResult.TYPE.TYPE_OUTCOME, 2);
                        }
                    } else {
                        log.error("unable to tally credit for " + organizationConcept.getFullName() + " due to missing customerID");
                    }
                } else {
                    log.error("unable to tally credit due to missing concept " + shoppingCart.getJournal());
                }
            }

            // process payment via PayPal
            PaymentService paymentService = new DSpace().getSingletonService(PaymentService.class);
            if (paymentService.submitReferenceTransaction(c, wfi, request)) {
                log.info("processed PayPal payment for Item " + itemID + ", marking cart complete");
                return new ActionResult(ActionResult.TYPE.TYPE_OUTCOME, ActionResult.OUTCOME_COMPLETE);
            }


        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        //Send us to the re authorization of paypal payment
        log.info("no payment processed for Item " + itemID + ", sending to revalidation step");
        WorkflowEmailManager.notifyOfReAuthorizationPayment(c, wfi);
        return new ActionResult(ActionResult.TYPE.TYPE_OUTCOME, 1);

    }
}
