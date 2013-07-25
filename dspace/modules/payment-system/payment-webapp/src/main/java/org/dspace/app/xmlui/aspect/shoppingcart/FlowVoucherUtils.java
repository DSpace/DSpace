package org.dspace.app.xmlui.aspect.shoppingcart;

import org.apache.cocoon.environment.Request;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.dspace.app.xmlui.aspect.administrative.FlowResult;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.paymentsystem.PaymentSystemService;
import org.dspace.paymentsystem.Voucher;
import org.dspace.paymentsystem.VoucherValidationService;
import org.dspace.utils.DSpace;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;
import java.util.Random;

/**
 * User: lantian @ atmire . com
 * Date: 7/11/13
 * Time: 11:20 AM
 */
public class FlowVoucherUtils {
    private static final Message T_add_voucher_success_notice =
            new Message("default", "xmlui.administrative.FlowVoucherUtils.add_voucher_success_notice");


    public static FlowResult processEditVoucher(Context context,
                                                     Request request, Map ObjectModel, int voucherID)
            throws SQLException, AuthorizeException {

        FlowResult result = new FlowResult();
        result.setContinue(false); // default to failure
        // Get all our request parameters

        String status = request.getParameter("status");

        VoucherValidationService voucherValidationService = new DSpace().getSingletonService(VoucherValidationService.class);

        if (result.getErrors() == null) {
            // Grab the person in question
            Voucher voucher = voucherValidationService.findById(context, voucherID);
            String statusOriginal = voucher.getStatus();

            if (!statusOriginal.equals(status)) {
                voucher.setStatus(status);
            }


            voucher.update();
            context.commit();

            result.setContinue(true);
            result.setOutcome(true);
            // FIXME: rename this message
            result.setMessage(T_add_voucher_success_notice);
        }

        // Everything was fine
        return result;
    }

    public static FlowResult processAddVoucher(Context context, Request request, Map objectModel) throws SQLException, AuthorizeException {
        FlowResult result = new FlowResult();
        result.setContinue(false); // default to no continue

        String totalNumber = request.getParameter("totalNumber").trim();
        String explanation = request.getParameter("explanation").trim();
        String status = Voucher.STATUS_OPEN;
        Date creation = new Date();
        String customer = request.getParameter("customerCode").trim();
        EPerson generator =context.getCurrentUser();

        VoucherValidationService voucherValidationService = new DSpace().getSingletonService(VoucherValidationService.class);
        // If we have errors, the form needs to be resubmitted to fix those problems

        if (StringUtils.isEmpty(totalNumber)) {
            result.addError("totalNumber");
        }

        if (result.getErrors() == null) {
            // No errors, so we try to create the voucher from the data provided

            ArrayList<Voucher> newVoucher = voucherValidationService.createVouchers(context, status,creation,Integer.parseInt(totalNumber),explanation, customer, generator.getID());
            context.commit();
            // success
            result.setContinue(true);
            result.setOutcome(true);
            result.setMessage(T_add_voucher_success_notice);
            result.setParameter("voucher_id", newVoucher.get(0).getID());
        }

        return result;
    }
}
