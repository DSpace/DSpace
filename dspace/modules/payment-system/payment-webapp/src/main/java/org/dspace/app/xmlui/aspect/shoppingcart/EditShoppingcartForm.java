package org.dspace.app.xmlui.aspect.shoppingcart;

/**
 * User: lantian @ atmire . com
 * Date: 7/10/13
 * Time: 12:29 PM
 */


import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Properties;

import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.datadryad.api.DryadOrganizationConcept;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.*;
import org.dspace.app.xmlui.wing.element.Item;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.*;
import org.dspace.eperson.EPerson;
import org.dspace.paymentsystem.*;



public class EditShoppingcartForm  extends AbstractDSpaceTransformer
{
    private static final Logger log = Logger.getLogger(EditShoppingcartForm.class);
    
    /** Language Strings */
    private static final Message T_dspace_home =
            message("xmlui.general.dspace_home");

    private static final Message T_submit_save =
            message("xmlui.general.save");

    private static final Message T_submit_cancel =
            message("xmlui.general.cancel");

    private static final Message T_trail =
            message("xmlui.property.EditShoppingcartForm.trail");

    private static final Message T_head1 =
            message("xmlui.property.EditShoppingcartForm.head1");

    private static final Message T_country_null =
            message("xmlui.property.EditShoppingcartForm.country_null");

    private static final Message T_item_title =
            message("xmlui.property.EditShoppingcart.title");

    private static final Message T_depositor =
            message("xmlui.property.EditShoppingcart.depositor");

    private static final Message T_sponsor =
            message("xmlui.property.EditShoppingcart.sponsor");
    private static final Message T_voucher =
            message("xmlui.property.EditShoppingcart.voucher");
    private static final Message T_token =
            message("xmlui.property.EditShoppingcart.token");
    private static final Message T_transaction =
            message("xmlui.property.EditShoppingcart.transaction");
    private static final Message T_status =
            message("xmlui.property.EditShoppingcart.status");
    private static final Message T_total =
            message("xmlui.property.EditShoppingcart.total");
    private static final Message T_country =
            message("xmlui.property.EditShoppingcart.country");

    private static final Message T_basic_fee =
            message("xmlui.property.EditShoppingcart.basic_fee");
    private static final Message T_surcharge =
            message("xmlui.property.EditShoppingcart.surcharge");


    private static final Message T_voucher_used =
            message("xmlui.Shoppingcart.EditProfile.voucher_used");
    private static final Message T_voucher_null =
            message("xmlui.Shoppingcart.EditProfile.voucher_null");



    public void addPageMeta(PageMeta pageMeta) throws WingException
    {
        pageMeta.addMetadata("title").addContent(T_item_title);
        pageMeta.addTrailLink(contextPath + "/", T_dspace_home);
        pageMeta.addTrailLink(contextPath + "/admin/shoppingcart",T_trail);
        pageMeta.addTrail().addContent(T_trail);
    }


    public void addBody(Body body) throws WingException, SQLException, AuthorizeException
    {
        // Get all our parameters
        boolean admin = AuthorizeManager.isAdmin(context);

        Request request = ObjectModelHelper.getRequest(objectModel);

        Properties countries = PaymentSystemConfigurationManager.getAllCountryProperty();

        // Get our parameters;
        int shoppingcartID = parameters.getParameterAsInteger("shoppingcart_id",-1);
        String errorString = parameters.getParameter("errors",null);
        ArrayList<String> errors = new ArrayList<String>();
        if (errorString != null)
        {
            for (String error : errorString.split(","))
            {
                errors.add(error);
                log.debug("Error listed in parameter: " + error);
            }
        }

        // Grab the property in question
        ShoppingCart shoppingcart = ShoppingCart.find(context, shoppingcartID);

        if (shoppingcart == null)
        {
            throw new UIException("Unable to find shoppingcart for id:" + shoppingcartID);
        }
        Integer itemId = shoppingcart.getItem();
        org.dspace.content.Item item = org.dspace.content.Item.find(context, itemId);

        String basicFee = Double.toString(shoppingcart.getBasicFee());
        if (StringUtils.isNotEmpty(request.getParameter("basicFee"))) {
            basicFee = request.getParameter("basicFee");
        }
        String surCharge = Double.toString(shoppingcart.getSurcharge());
        if (StringUtils.isNotEmpty(request.getParameter("surCharge"))) {
            surCharge = request.getParameter("surCharge");

        }

        // DIVISION: shoppingcart-edit
        Division edit = body.addInteractiveDivision("shoppingcart-edit",contextPath+"/admin/shoppingcart",Division.METHOD_MULTIPART,"primary administrative shoppingcart");
        edit.setHead(T_head1);

        if (errors.contains("country")) {
            Para problem = edit.addPara();
            problem.addHighlight("bold").addContent(T_country_null);
        }
        if (errors.contains("voucher_null")) {
            Para problem = edit.addPara();
            problem.addHighlight("bold").addContent(T_voucher_null);
        }
        if (errors.contains("voucher_used")) {
            Para problem = edit.addPara();
            problem.addHighlight("bold").addContent(T_voucher_used);
        }

        List identity = edit.addList("form", List.TYPE_FORM);
        identity.setHead("Shopping cart " + shoppingcartID + " for item " + itemId);

        String title = "";
        try {
            if (item == null) {
                title = "Item not found";
            } else {
                DCValue[] name = item.getDC("title", null, org.dspace.content.Item.ANY);
                if (name == null || name.length == 0) {
                    title = "Untitled";
                } else {
                    title = name[0].value;
                }
            }
        } catch (Exception e) {
            title = "Unknown";
            System.out.println(e.getMessage());
        }
        identity.addLabel(T_item_title);
        identity.addItem().addContent(title);

        String status = shoppingcart.getStatus();
        if (StringUtils.isNotEmpty(request.getParameter("status"))) {
            status = request.getParameter("status");
        }

        if (admin) {
            Select statusField = identity.addItem().addSelect("status");
            statusField.setRequired();
            statusField.setLabel(T_status);
            statusField.addOption(ShoppingCart.STATUS_COMPLETED, ShoppingCart.STATUS_COMPLETED);
            statusField.addOption(ShoppingCart.STATUS_DENIED, ShoppingCart.STATUS_DENIED);
            statusField.addOption(ShoppingCart.STATUS_OPEN, ShoppingCart.STATUS_OPEN);
            statusField.addOption(ShoppingCart.STATUS_VERIFIED, ShoppingCart.STATUS_VERIFIED);
            statusField.setOptionSelected(status);
        } else {
            identity.addLabel(T_status);
            identity.addItem().addContent(status);
        }
        identity.addLabel("Order date");
        if (shoppingcart.getOrderDate() != null)
            identity.addItem().addContent(shoppingcart.getOrderDate().toString());
        identity.addLabel("Payment date");
        if (shoppingcart.getPaymentDate() != null)
            identity.addItem().addContent(shoppingcart.getPaymentDate().toString());

        // Options that cause user to not pay DPC:
        List paymentWaiver = edit.addList("payment_waiver", List.TYPE_FORM);
        paymentWaiver.setHead("Payment Waiver Information");

        // Sponsor
        DryadOrganizationConcept sponsorConcept = shoppingcart.getSponsoringOrganization(context);
        String sponsorName = "";
        String subscription = "none";
        String sponsorConceptID = "";
        String sponsorCustomerID = "";
        if (sponsorConcept != null) {
            sponsorName = sponsorConcept.getFullName();
            subscription = sponsorConcept.getPaymentPlan();
            if ("".equals(subscription)) {
                subscription = "none";
            }
            sponsorConceptID = String.valueOf(sponsorConcept.getConceptID());
            sponsorCustomerID = sponsorConcept.getCustomerID();
        }

        // Voucher
        Integer voucherId = shoppingcart.getVoucher();
        String voucherCode = "";
        Voucher voucher = null;
        if (voucherId != null && voucherId > 0) {
            voucher = Voucher.findById(context, voucherId);
            voucherCode = voucher.getCode();
        }
        if (StringUtils.isNotEmpty(request.getParameter("voucher"))) {
            voucherCode = request.getParameter("voucher");
        }

        // Fee-waiver country
        String country = shoppingcart.getCountry();
        if (StringUtils.isNotEmpty(request.getParameter("country"))) {
            country = request.getParameter("country");
        }

        if (admin) {
            Text sponsorField = paymentWaiver.addItem().addText("sponsor");
            sponsorField.setLabel(T_sponsor);
            sponsorField.setValue(sponsorName);

            Text voucherField = paymentWaiver.addItem().addText("voucher");
            voucherField.setLabel(T_voucher);
            voucherField.setValue(voucherCode);

            Select countryField = paymentWaiver.addItem().addSelect("country");
            countryField.setRequired();
            countryField.setLabel(T_country);
            countryField.addOption("", "Select fee-waiver country");
            for (String countryTemp : countries.stringPropertyNames()) {
                if (country != null && country.length() > 0 && country.equals(countryTemp)) {
                    countryField.addOption(true, countryTemp, countryTemp);
                }
                else
                {
                    countryField.addOption(false, countryTemp, countryTemp);
                }
            }
            countryField.setOptionSelected(country);
        } else {
            paymentWaiver.addLabel(T_sponsor);
            paymentWaiver.addItem().addContent(sponsorName);
            paymentWaiver.addLabel(T_voucher);
            paymentWaiver.addItem().addContent(voucherCode);
            paymentWaiver.addLabel(T_country);
            paymentWaiver.addItem().addContent(country);
        }

        // Options for if the depositor is paying the DPC:
        List depositorInfo = edit.addList("depositor_info", List.TYPE_FORM);
        depositorInfo.setHead("Depositor Payment Information");
        Integer depositorId = shoppingcart.getDepositor();
        EPerson depositor = EPerson.find(context, depositorId);

        Double total = shoppingcart.getTotal();

        String secureToken = shoppingcart.getSecureToken() == null ? shoppingcart.getSecureToken() : "";
        String transactionId = shoppingcart.getTransactionId() == null ? shoppingcart.getTransactionId() : "";
        if (StringUtils.isNotEmpty(request.getParameter("transactionId"))) {
            transactionId = request.getParameter("transactionId");
        }

        if (admin) {
            depositorInfo.addLabel(T_depositor);
            depositorInfo.addItem().addContent(depositor.getFullName());

            Text transactionIdField = depositorInfo.addItem().addText("transactionId");
            transactionIdField.setLabel(T_transaction);
            transactionIdField.setValue(transactionId);

            if (secureToken != null) {
                depositorInfo.addLabel(T_token);
                depositorInfo.addItem().addContent(secureToken);
            }

            depositorInfo.addLabel(T_basic_fee);
            depositorInfo.addItem().addText("basicFee").setValue(basicFee);
            depositorInfo.addLabel(T_surcharge);
            depositorInfo.addItem().addText("surCharge").setValue(surCharge);
        } else {
            depositorInfo.addLabel(T_depositor);
            depositorInfo.addItem().addContent(depositor.getFullName());
            depositorInfo.addLabel(T_basic_fee);
            depositorInfo.addItem().addContent(basicFee);
            depositorInfo.addLabel(T_surcharge);
            depositorInfo.addItem().addContent(surCharge);
        }
        depositorInfo.addLabel(T_total);
        depositorInfo.addItem().addContent(Double.toString(total));

        // Final notes about the cart:
        List additionalInfo = edit.addList("additional_info", List.TYPE_FORM);
        additionalInfo.setHead("Additional Information");

        additionalInfo.addLabel("Notes");
        if (shoppingcart.getNote() != null)
            additionalInfo.addItem("note", "note").addTextArea("note").setValue(shoppingcart.getNote());
        else
            additionalInfo.addItem("note", "note").addTextArea("note");

        Item buttons = additionalInfo.addItem();
        buttons.addButton("submit_cancel").setValue(T_submit_cancel);
        if(admin)
        {
            buttons.addButton("submit_save").setValue(T_submit_save);
        }
        edit.addHidden("administrative-continue").setValue(knot.getId());



    }

}
