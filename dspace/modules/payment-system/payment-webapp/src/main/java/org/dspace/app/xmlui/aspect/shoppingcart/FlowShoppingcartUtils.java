/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.shoppingcart;

import java.sql.SQLException;
import java.util.Map;

import org.apache.cocoon.environment.Request;
import org.apache.commons.lang.StringUtils;
import org.dspace.app.xmlui.aspect.administrative.FlowResult;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.authorize.AuthorizeException;
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

    private static final Message T_edit_shoppingcart_success_notice =
            new Message("default", "xmlui.administrative.FlowShoppingcartUtils.add_shoppingcart_success_notice");


    public static FlowResult processEditShoppingcart(Context context,
                                                       Request request, Map ObjectModel, int shoppingcartID)
            throws SQLException, AuthorizeException {

        FlowResult result = new FlowResult();
        result.setContinue(false); // default to failure
        // Get all our request parameters
        String voucher = request.getParameter("voucher");
        String total = request.getParameter("total");
        String country = request.getParameter("country");
        String currency = request.getParameter("currency");
        String transactionId = request.getParameter("transactionId");
        PaymentSystemService paymentSystemService = new DSpace().getSingletonService(PaymentSystemService.class);


        // If we have errors, the form needs to be resubmitted to fix those problems
        // currency and country should be null
        if (StringUtils.isEmpty(country)) {
            result.addError("country");
        }
        if (StringUtils.isEmpty(currency)) {
            result.addError("currency");
        }

        if (result.getErrors() == null) {
            // Grab the person in question
            ShoppingCart shoppingCart = paymentSystemService.getTransaction(context, shoppingcartID);


            String voucherOriginal = shoppingCart.getVoucher();
            Double totalOriginal = shoppingCart.getTotal();
            String countryOriginal = shoppingCart.getCountry();
            String currencyOriginal = shoppingCart.getCurrency();
            String transactionIdOriginal = shoppingCart.getTransactionId();

            if (!voucherOriginal.equals(voucher)) {
                shoppingCart.setVoucher(voucher);
            }

            if (totalOriginal!=Double.parseDouble(total)) {
                shoppingCart.setTotal(Double.parseDouble(total));
            }
            if (country != null && !countryOriginal.equals(country)) {
                shoppingCart.setCountry(country);
            }
            if (currency != null && !currencyOriginal.equals(currency)) {
                shoppingCart.setCurrency(currency);
            }

            if (transactionId!=null&&!transactionIdOriginal.equals(transactionId)) {
                shoppingCart.setTransactionId(transactionId);
            }

            shoppingCart.update();
            context.commit();

            result.setContinue(true);
            result.setOutcome(true);
            // FIXME: rename this message
            result.setMessage(T_edit_shoppingcart_success_notice);
        }

        // Everything was fine
        return result;
    }
}
