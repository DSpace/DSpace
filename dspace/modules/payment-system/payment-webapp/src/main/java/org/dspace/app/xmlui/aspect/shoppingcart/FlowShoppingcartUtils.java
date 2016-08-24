/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.shoppingcart;

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
            new Message("default", "xmlui.administrative.FlowShoppingcartUtils.add_shoppingcart_success_notice");


    public static FlowResult processEditShoppingcart(Context context,
                                                     Request request, Map ObjectModel, int shoppingcartID) {
        FlowResult result = new FlowResult();
        try {
            result.setContinue(false); // default to failure
            // Get all our request parameters
            String voucherCode = request.getParameter("voucher");
            String country = request.getParameter("country");
            String currency = request.getParameter("currency");
            String basicFee = request.getParameter("basicFee");
            String surCharge = request.getParameter("surCharge");
            String transactionId = request.getParameter("transactionId");
            PaymentSystemService paymentSystemService = new DSpace().getSingletonService(PaymentSystemService.class);
            VoucherValidationService voucherValidationService = new DSpace().getSingletonService(VoucherValidationService.class);
            Voucher voucher = null;
            if (!StringUtils.isEmpty(voucherCode)) {
                voucher = Voucher.findByCode(context,voucherCode);
            }
            
            
            ShoppingCart shoppingCart = paymentSystemService.getShoppingCart(context, shoppingcartID);
            
            String countryOriginal = shoppingCart.getCountry();
            String currencyOriginal = shoppingCart.getCurrency();
            String transactionIdOriginal = shoppingCart.getTransactionId();
            
            // If we have errors, the form needs to be resubmitted to fix those problems
            // currency should not be null
            if (StringUtils.isEmpty(currency)) {
                result.addError("currency");
            }
            if (!StringUtils.isEmpty(voucherCode)) {
                if(voucher==null)
                    {
                        result.addError("voucher_null");
                    }
                else
                    {
                        if(!voucherValidationService.validate(context,voucher.getID(),shoppingCart))
                            {
                                result.addError("voucher_used");
                            }
                    }
            }
            
            String note = request.getParameter("note");
            if (!StringUtils.isEmpty(note)){
                shoppingCart.setNote(note);
            }
            else
                {
                    shoppingCart.setNote(null);
                }
            
            if (result.getErrors() == null) {
                if (!StringUtils.isEmpty(voucherCode)) {
                    
                    if(!voucherValidationService.voucherUsed(context,voucherCode))
                        {
                            shoppingCart.setVoucher(voucher.getID());
                        }
                }
                else
                    {
                        //delete the voucher
                        shoppingCart.setVoucher(null);
                    }
                
                if (country != null &&!country.equals(countryOriginal)) {
                    shoppingCart.setCountry(country);
                }
                else
                    {
                        if(country!=null&&country.length()==0)
                            {
                                shoppingCart.setCountry(null);
                            }
                    }
                
                if (currency != null && !currency.equals(currencyOriginal)) {
                    paymentSystemService.setCurrency(shoppingCart,currency);
                }
                else{
                    //only when the currency doesn't change then change the individual rate
                    if (surCharge != null && surCharge.length()>0 && Double.parseDouble(surCharge)>0) {
                        shoppingCart.setSurcharge(Double.parseDouble(surCharge));
                    }
                    else
                        {
                            shoppingCart.setSurcharge(new Double(0.0));
                        }

                    if (basicFee != null && basicFee.length()>0 && Double.parseDouble(basicFee)>0) {
                        shoppingCart.setBasicFee(Double.parseDouble(basicFee));
                    }
                    else
                        {
                            shoppingCart.setBasicFee(new Double(0.0));
                        }
                }
                
                if (StringUtils.isEmpty(transactionId)){
                    shoppingCart.setTransactionId(null);
                }
                else
                    {
                        if(!transactionId.equals(transactionIdOriginal)) {
                            shoppingCart.setTransactionId(transactionId);
                        }
                    }
                
                paymentSystemService.updateTotal(context,shoppingCart);
                context.commit();
                
                result.setContinue(true);
                result.setOutcome(true);
                
                result.setMessage(T_edit_shoppingcart_success_notice);
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
