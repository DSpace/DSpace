/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 * <p>
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.shoppingcart;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.cocoon.environment.Request;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.xmlui.aspect.administrative.FlowResult;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.core.Context;

import org.dspace.paymentsystem.*;
import org.dspace.utils.DSpace;

/**
 * FlowUtils to facilitate Javascript Actions for Shopping Cart User Interface
 *
 * @author Mark Diggory, mdiggory at atmire.com
 * @author Fabio Bolognesi, fabio at atmire.com
 * @author Lantian Gai, lantian at atmire.com
 */
public class FlowShoppingcartUtils {
    private static final Logger log = Logger.getLogger(FlowShoppingcartUtils.class);

    private static final Message T_edit_shoppingcart_success_notice =
            new Message("default", "xmlui.administrative.FlowShoppingcartUtils.edit_shoppingcart_success_notice");


    public static FlowResult processEditShoppingcart(Context context,
                                                     Request request, Map ObjectModel, int shoppingcartID) {
        FlowResult result = new FlowResult();
        try {
            result.setContinue(false); // default to failure
            // Get all our request parameters
            String voucherCode = request.getParameter("voucher");
            String country = request.getParameter("country");
            String basicFee = request.getParameter("basicFee");
            String surCharge = request.getParameter("surCharge");
            String transactionId = request.getParameter("transactionId");
            String secureToken = request.getParameter("secureToken");
            String status = request.getParameter("status");
            String note = request.getParameter("note");
            PaymentSystemService paymentSystemService = new DSpace().getSingletonService(PaymentSystemService.class);
            VoucherValidationService voucherValidationService = new DSpace().getSingletonService(VoucherValidationService.class);

            ShoppingCart shoppingCart = paymentSystemService.getShoppingCart(context, shoppingcartID);

            String countryOriginal = shoppingCart.getCountry();
            String transactionIdOriginal = shoppingCart.getTransactionId();
            String secureTokenOriginal = shoppingCart.getSecureToken();

            if (!StringUtils.isEmpty(note)) {
                shoppingCart.setNote(note);
            } else {
                shoppingCart.setNote(null);
            }
            if (!shoppingCart.getStatus().equals(status)) {
                // update status, update payment date to current date
                shoppingCart.setStatus(status);
                shoppingCart.setPaymentDate(new Date());
            }

            if (!StringUtils.isEmpty(voucherCode)) {
                // see if this is a valid voucher code
                Voucher voucher = Voucher.findByCode(context, voucherCode);
                if (voucher == null) {
                    result.addError("voucher_null");
                } else if (voucherValidationService.voucherUsed(context, voucherCode)) {
                    result.addError("voucher_used");
                } else {
                    shoppingCart.setVoucher(voucher.getID());
                }
            }

            if (country != null && !country.equals(countryOriginal)) {
                shoppingCart.setCountry(country);
            } else {
                if (country != null && country.length() == 0) {
                    shoppingCart.setCountry(null);
                }
            }

            if (surCharge != null && surCharge.length() > 0 && Double.parseDouble(surCharge) > 0) {
                shoppingCart.setSurcharge(Double.parseDouble(surCharge));
            } else {
                shoppingCart.setSurcharge(0.0);
            }

            if (basicFee != null && basicFee.length() > 0 && Double.parseDouble(basicFee) > 0) {
                shoppingCart.setBasicFee(Double.parseDouble(basicFee));
            } else {
                shoppingCart.setBasicFee(0.0);
            }

            if (StringUtils.isEmpty(transactionId)) {
                shoppingCart.setTransactionId(null);
            } else {
                if (!transactionId.equals(transactionIdOriginal)) {
                    shoppingCart.setTransactionId(transactionId);
                }
            }
            if (StringUtils.isEmpty(secureToken)) {
                shoppingCart.setSecureToken(null);
            } else {
                if (!secureToken.equals(secureTokenOriginal)) {
                    shoppingCart.setSecureToken(secureToken);
                }
            }
            shoppingCart.update();
            List<String> errors = result.getErrors();
            if (errors == null || errors.size() == 0) {
                shoppingCart.updateCartInternals(context);
                context.commit();

                result.setContinue(true);
                result.setOutcome(true);

                result.setMessage(T_edit_shoppingcart_success_notice);
            } else if (errors.size() > 0){
                log.error("Errors in shopping cart: " + errors.toString());
            }
        } catch (Exception e) {
            // catch and log all exceptions, otherwise they will go off to the javascript layer
            log.error("Unable to update shopping cart", e);
            result.addError("Exception occurred during update");
        }
        // Everything was fine
        return result;
    }

}
