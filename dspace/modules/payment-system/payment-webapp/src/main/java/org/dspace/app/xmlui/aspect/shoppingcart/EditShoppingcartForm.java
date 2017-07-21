package org.dspace.app.xmlui.aspect.shoppingcart;

/**
 * User: lantian @ atmire . com
 * Date: 7/10/13
 * Time: 12:29 PM
 */


import java.sql.SQLException;
import java.util.Properties;

import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.datadryad.api.DryadFunderConcept;
import org.datadryad.api.DryadJournalConcept;
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
        edit.setHead("Editing shopping cart " + shoppingcartID + " for item " + itemId);

        List identity = edit.addList("form", List.TYPE_FORM);
        identity.setHead("Item Information");

        String errorString = parameters.getParameter("errors",null);
        if (errorString != null && !"".equals(errorString)) {
            Para problem = edit.addPara();
            problem.addHighlight("bold").addContent("Error editing cart: " + errorString);
        }

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
        StringBuilder sponsorName = new StringBuilder();
        if (sponsorConcept != null) {
            sponsorName.append(sponsorConcept.getFullName());
            if (DryadJournalConcept.conceptIsValidJournal(sponsorConcept.getUnderlyingConcept())) {
                sponsorName.append(" (journal concept ");
            } else if (DryadFunderConcept.conceptIsValidFunder(sponsorConcept.getUnderlyingConcept())) {
                sponsorName.append(" (funder concept ");
            } else {
                sponsorName.append(" (concept ");
            }
            sponsorName.append(sponsorConcept.getConceptID()).append(")");
        } else {
            sponsorName.append("not sponsored");
            if (!"".equals(shoppingcart.getSponsorName())) {
                sponsorName.append(" (listed journal is ").append(shoppingcart.getSponsorName()).append(")");
            }
        }
        paymentWaiver.addLabel(T_sponsor);
        paymentWaiver.addItem().addContent(sponsorName.toString());

        // Voucher
        Integer voucherId = shoppingcart.getVoucher();
        String voucherCode = "";
        Voucher voucher = Voucher.findById(context, voucherId);
        if (voucher != null) {
            voucherCode = voucher.getCode();
        }

        // Fee-waiver country
        String country = shoppingcart.getCountry();
        if (StringUtils.isNotEmpty(request.getParameter("country"))) {
            country = request.getParameter("country");
        }

        if (admin) {
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

        String secureToken = shoppingcart.getSecureToken() == null ? "" : shoppingcart.getSecureToken();
        String transactionId = shoppingcart.getTransactionId() == null ? "" : shoppingcart.getTransactionId();
        if (StringUtils.isNotEmpty(request.getParameter("transactionId"))) {
            transactionId = request.getParameter("transactionId");
        }

        if (admin) {
            depositorInfo.addLabel(T_depositor);
            depositorInfo.addItem().addContent(depositor.getFullName());

            Text transactionIdField = depositorInfo.addItem().addText("transactionId");
            transactionIdField.setLabel(T_transaction);
            transactionIdField.setValue(transactionId);

            Text secureTokenField = depositorInfo.addItem().addText("secureToken");
            secureTokenField.setLabel(T_token);
            secureTokenField.setValue(secureToken);

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
