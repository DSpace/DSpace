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
import org.dspace.utils.DSpace;



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

    private static final Message T_title =
            message("xmlui.property.EditShoppingcartForm.title");

    private static final Message T_trail =
            message("xmlui.property.EditShoppingcartForm.trail");

    private static final Message T_head1 =
            message("xmlui.property.EditShoppingcartForm.head1");

    private static final Message T_currency_null =
            message("xmlui.property.EditShoppingcartForm.currency_null");

    private static final Message T_country_null =
            message("xmlui.property.EditShoppingcartForm.country_null");

    private static final Message T_head2 =
            message("xmlui.property.EditShoppingcartForm.head2");

    /** Language string used: */

    private static final Message T_name =
            message("xmlui.Shoppingcart.EditProfile.name");

    private static final Message T_name1 =
            message("xmlui.Shoppingcart.EditProfile.name1");

    private static final Message T_name2 =
            message("xmlui.Shoppingcart.EditProfile.name2");

    private static final Message T_name3 =
            message("xmlui.Shoppingcart.EditProfile.name3");
    private static final Message T_name4 =
            message("xmlui.Shoppingcart.EditProfile.name4");
    private static final Message T_name5 =
            message("xmlui.Shoppingcart.EditProfile.name5");
    private static final Message T_name6 =
            message("xmlui.Shoppingcart.EditProfile.name6");
    private static final Message T_name7 =
            message("xmlui.Shoppingcart.EditProfile.name7");
    private static final Message T_name8 =
            message("xmlui.Shoppingcart.EditProfile.name8");

    private static final Message T_name9 =
            message("xmlui.Shoppingcart.EditProfile.name9");
    private static final Message T_name10 =
            message("xmlui.Shoppingcart.EditProfile.name10");
    private static final Message T_name11 =
            message("xmlui.Shoppingcart.EditProfile.name11");



    private static final Message T_voucher_used =
            message("xmlui.Shoppingcart.EditProfile.voucher_used");
    private static final Message T_voucher_null =
            message("xmlui.Shoppingcart.EditProfile.voucher_null");



    public void addPageMeta(PageMeta pageMeta) throws WingException
    {
        pageMeta.addMetadata("title").addContent(T_title);
        pageMeta.addTrailLink(contextPath + "/", T_dspace_home);
        pageMeta.addTrailLink(contextPath + "/admin/shoppingcart",T_trail);
        pageMeta.addTrail().addContent(T_trail);
    }


    public void addBody(Body body) throws WingException, SQLException, AuthorizeException
    {
        // Get all our parameters
        boolean admin = AuthorizeManager.isCuratorOrAdmin(context);

        Request request = ObjectModelHelper.getRequest(objectModel);

        Properties countries  = PaymentSystemConfigurationManager.getAllCountryProperty();
        Properties currencies  = PaymentSystemConfigurationManager.getAllCurrencyProperty();
        PaymentSystemService paymentSystemService = new DSpace().getSingletonService(PaymentSystemService.class);

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
        String title = null;

        try{
            DCValue[] name = item.getDC("title", null, org.dspace.content.Item.ANY);
            if(name==null||name.length==0)
            {
                title ="Untitled";
            }
            else
            if(item==null)
            {
                title= "Item not found";
            }
            else
            {
                title = name[0].value;
            }

        }catch (Exception e)
        {
            title = "UnKnown";
            System.out.println(e.getMessage());
        }

        String secureToken = shoppingcart.getSecureToken()==null? shoppingcart.getSecureToken(): "";
        String transactionId = shoppingcart.getTransactionId()==null? shoppingcart.getTransactionId(): "";
        String country =  shoppingcart.getCountry();
        Integer depositorId = shoppingcart.getDepositor();
        EPerson depositor = EPerson.find(context,depositorId);
        String currency = shoppingcart.getCurrency();
        String status = shoppingcart.getStatus();
        Double total = shoppingcart.getTotal();
        Integer voucherId = shoppingcart.getVoucher();
        String voucherCode = "";
        Voucher voucher = null;
        if(voucherId!=null&&voucherId>0)
        {
            voucher = Voucher.findById(context,voucherId);
            voucherCode = voucher.getCode();
        }

        if (StringUtils.isNotEmpty(request.getParameter("transactionId")))
        {
            transactionId = request.getParameter("transactionId");
        }
        if (StringUtils.isNotEmpty(request.getParameter("country")))
        {
            country = request.getParameter("country");
        }

        if (StringUtils.isNotEmpty(request.getParameter("currency")))
        {
            currency =  request.getParameter("currency");
        }

        if (StringUtils.isNotEmpty(request.getParameter("status")) )
        {
            status = request.getParameter("status");
        }
        if (StringUtils.isNotEmpty(request.getParameter("voucher")))
        {
           voucherCode = request.getParameter("voucher");

        }

        String basicFee = Double.toString(PaymentSystemConfigurationManager.getCurrencyProperty(currency));
        Double basicFee1 = shoppingcart.getBasicFee();
        if(!basicFee1.equals(new Double(-1)))
        {
            basicFee = Double.toString(basicFee1);
        }


        String surCharge = Double.toString(PaymentSystemConfigurationManager.getNotIntegratedJournalFeeProperty(currency));
        Double surCharge1 = shoppingcart.getSurcharge();
        if(!surCharge1.equals(new Double(-1)))
        {
            surCharge = Double.toString(surCharge1);
        }

        String noInteg = Double.toString(PaymentSystemConfigurationManager.getSizeFileFeeProperty(currency));
        Double noInteg1 = shoppingcart.getNoInteg();
        if(!noInteg1.equals(new Double(-1)))
        {
            noInteg = Double.toString(noInteg1);
        }


        if (StringUtils.isNotEmpty(request.getParameter("basicFee")))
        {
            basicFee = request.getParameter("basicFee");

        }
        if (StringUtils.isNotEmpty(request.getParameter("surCharge")))
        {
            surCharge = request.getParameter("surCharge");

        }
        if (StringUtils.isNotEmpty(request.getParameter("noInteg")))
        {
            noInteg = request.getParameter("noInteg");

        }


        // DIVISION: shoppingcart-edit
        Division edit = body.addInteractiveDivision("shoppingcart-edit",contextPath+"/admin/shoppingcart",Division.METHOD_MULTIPART,"primary administrative shoppingcart");
        edit.setHead(T_head1);

        if (errors.contains("country")) {
            Para problem = edit.addPara();
            problem.addHighlight("bold").addContent(T_country_null);
        }
        if (errors.contains("currency")) {
            Para problem = edit.addPara();
            problem.addHighlight("bold").addContent(T_currency_null);
        }
        if (errors.contains("voucher_null")) {
            Para problem = edit.addPara();
            problem.addHighlight("bold").addContent(T_voucher_null);
        }
        if (errors.contains("voucher_used")) {
            Para problem = edit.addPara();
            problem.addHighlight("bold").addContent(T_voucher_used);
        }


        List identity = edit.addList("form",List.TYPE_FORM);
        identity.setHead(T_head2.parameterize(title));


        identity.addLabel(T_name);
        identity.addItem().addContent(title);
        identity.addLabel(T_name1);
        identity.addItem().addContent(depositor.getFullName());
        identity.addLabel(T_name3);
        if(secureToken!=null) {
            identity.addItem().addContent(secureToken);
        }
        else {
            identity.addItem().addContent("");
        }
        if (admin)
        {
            Text voucherField = identity.addItem().addText("voucher");
            voucherField.setLabel(T_name2);
            voucherField.setValue(voucherCode);

            Text transactionIdField = identity.addItem().addText("transactionId");
            transactionIdField.setLabel(T_name4);
            transactionIdField.setValue(transactionId);

            Select statusField = identity.addItem().addSelect("status");
            statusField.setRequired();
            statusField.setLabel(T_name5);
            statusField.addOption(ShoppingCart.STATUS_COMPLETED,ShoppingCart.STATUS_COMPLETED);
            statusField.addOption(ShoppingCart.STATUS_DENIlED,ShoppingCart.STATUS_DENIlED);
            statusField.addOption(ShoppingCart.STATUS_OPEN,ShoppingCart.STATUS_OPEN);
            statusField.addOption(ShoppingCart.STATUS_VERIFIED,ShoppingCart.STATUS_VERIFIED);
            statusField.setOptionSelected(status);

            Select currencyField = identity.addItem().addSelect("currency");
            currencyField.setRequired();
            currencyField.setLabel(T_name6);
            for(String currencyTemp: currencies.stringPropertyNames())
            {
                if(currency.equals(currencyTemp))
                {
                    currencyField.addOption(true, currencyTemp, currencyTemp);
                }
                else
                {
                    currencyField.addOption(false, currencyTemp, currencyTemp);
                }
            }
            currencyField.setOptionSelected(currency);

            Select countryField = identity.addItem().addSelect("country");
            countryField.setRequired();
            countryField.setLabel(T_name8);
            countryField.addOption("","Select Your Country");
            for(String countryTemp: countries.stringPropertyNames())
            {
                if(country!=null&&country.length()>0&&country.equals(countryTemp))
                {
                    countryField.addOption(true, countryTemp, countryTemp);
                }
                else
                {
                    countryField.addOption(false, countryTemp, countryTemp);
                }
            }
            countryField.setOptionSelected(country);

            identity.addLabel(T_name9);
            identity.addItem().addText("basicFee").setValue(basicFee);
            identity.addLabel(T_name10);
            identity.addItem().addText("noInteg").setValue(noInteg);
            identity.addLabel(T_name11);
            identity.addItem().addText("surCharge").setValue(surCharge);

        }
        else
        {
            identity.addLabel(T_name2);
            identity.addItem().addContent(voucher.getCode());
            identity.addLabel(T_name4);
            identity.addItem().addContent(transactionId);
            identity.addLabel(T_name5);
            identity.addItem().addContent(status);
            identity.addLabel(T_name6);
            identity.addItem().addContent(currency);
            identity.addLabel(T_name8);
            identity.addItem().addContent(country);

            identity.addLabel(T_name9);
            identity.addItem().addContent(basicFee);
            identity.addLabel(T_name10);
            identity.addItem().addContent(noInteg);
            identity.addLabel(T_name11);
            identity.addItem().addContent(surCharge);
        }
        identity.addLabel("Order date");
        if(shoppingcart.getOrderDate()!=null)
            identity.addItem().addContent(shoppingcart.getOrderDate().toString());
        identity.addLabel("Payment date");
        if(shoppingcart.getPaymentDate()!=null)
            identity.addItem().addContent(shoppingcart.getPaymentDate().toString());
        identity.addLabel("Notes");
        if(shoppingcart.getNote()!=null)
            identity.addItem("note","note").addTextArea("note").setValue(shoppingcart.getNote());
        else
            identity.addItem("note","note").addTextArea("note");

        identity.addLabel(T_name7);
        identity.addItem().addContent(Double.toString(total));
        Item buttons = identity.addItem();
        buttons.addButton("submit_cancel").setValue(T_submit_cancel);
        if(admin)
        {
            buttons.addButton("submit_save").setValue(T_submit_save);
        }
        edit.addHidden("administrative-continue").setValue(knot.getId());



    }

}
