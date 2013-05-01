/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.shoppingcart;

import java.sql.SQLException;
import java.util.ArrayList;

import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.commons.lang.StringUtils;
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

/**
 * Administrative Edit Interface for Shopping Cart Records
 *
 * @author Mark Diggory, mdiggory at atmire.com
 * @author Fabio Bolognesi, fabio at atmire.com
 * @author Lantian Gai, lantian at atmire.com
 */
public class EditShoppingcartForm  extends AbstractDSpaceTransformer
{
    /** Language Strings */
    private static final Message T_dspace_home =
            message("xmlui.general.dspace_home");

    private static final Message T_submit_save =
            message("xmlui.general.save");

    private static final Message T_submit_cancel =
            message("xmlui.general.cancel");

    private static final Message T_title =
            message("xmlui.property.EditShoppingcartForm.title");

    private static final Message T_Shoppingcart_trail =
            message("xmlui.property.general.epeople_trail");

    private static final Message T_trail =
            message("xmlui.property.EditShoppingcartForm.trail");

    private static final Message T_head1 =
            message("xmlui.property.EditShoppingcartForm.head1");

    private static final Message T_name_exists =
            message("xmlui.property.EditShoppingcartForm.email_taken");

    private static final Message T_head2 =
            message("xmlui.property.EditShoppingcartForm.head2");

    private static final Message T_error_name_unique =
            message("xmlui.property.EditShoppingcartForm.error_email_unique");

    private static final Message T_error_name =
            message("xmlui.property.EditShoppingcartForm.error_name");

    private static final Message T_error_value =
            message("xmlui.property.EditShoppingcartForm.error_value");


    private static final Message T_special_help =
            message("xmlui.property.EditShoppingcartForm.special_help");

    private static final Message T_submit_delete =
            message("xmlui.property.EditShoppingcartForm.submit_delete");

    private static final Message T_submit_login_as =
            message("xmlui.property.EditShoppingcartForm.submit_login_as");

    private static final Message T_delete_constraint =
            message("xmlui.property.EditShoppingcartForm.delete_constraint");

    private static final Message T_constraint_last_conjunction =
            message("xmlui.property.EditShoppingcartForm.delete_constraint.last_conjunction");

    private static final Message T_constraint_item =
            message("xmlui.property.EditShoppingcartForm.delete_constraint.item");

    private static final Message T_constraint_workflowitem =
            message("xmlui.property.EditShoppingcartForm.delete_constraint.workflowitem");

    private static final Message T_constraint_tasklistitem =
            message("xmlui.property.EditShoppingcartForm.delete_constraint.tasklistitem");

    private static final Message T_constraint_unknown =
            message("xmlui.property.EditShoppingcartForm.delete_constraint.unknown");

    private static final Message T_member_head =
            message("xmlui.property.EditShoppingcartForm.member_head");

    private static final Message T_indirect_member =
            message("xmlui.property.EditShoppingcartForm.indirect_member");

    private static final Message T_member_none =
            message("xmlui.property.EditShoppingcartForm.member_none");

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







    private static final Message T_value =
            message("xmlui.Shoppingcart.EditProfile.value");

    private static final Message T_language =
            message("xmlui.Shoppingcart.EditProfile.language");

    private static final Message T_telephone =
            message("xmlui.Shoppingcart.EditProfile.telephone");

    private static final Message T_bitstream =
            message("xmlui.Shoppingcart.EditProfile.bitstream");
    private static final Message T_submit_delete_bitstream =
            message("xmlui.Shoppingcart.EditProfile.bitstream_delete");



    public void addPageMeta(PageMeta pageMeta) throws WingException
    {
        pageMeta.addMetadata("title").addContent(T_title);
        pageMeta.addTrailLink(contextPath + "/", T_dspace_home);
        pageMeta.addTrailLink(contextPath + "/simple-property",T_trail);
        pageMeta.addTrail().addContent(T_trail);
    }


    public void addBody(Body body) throws WingException, SQLException, AuthorizeException
    {
        // Get all our parameters
        boolean admin = AuthorizeManager.isAdmin(context);

        Request request = ObjectModelHelper.getRequest(objectModel);

        // Get our parameters;
        int shoppingcartID = parameters.getParameterAsInteger("shoppingcart_id",-1);
        String errorString = parameters.getParameter("errors",null);
        ArrayList<String> errors = new ArrayList<String>();
        if (errorString != null)
        {
            for (String error : errorString.split(","))
            {
                errors.add(error);
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

        String secureToken = shoppingcart.getSecureToken();
        String transactionId = shoppingcart.getTransactionId();
        String country =  shoppingcart.getCountry();
        Integer depositorId = shoppingcart.getDepositor();
        EPerson depositor = EPerson.find(context,depositorId);
        String currency = shoppingcart.getCurrency();
        String status = shoppingcart.getStatus();
        Double total = shoppingcart.getTotal();
        String voucher = shoppingcart.getVoucher();

        if (StringUtils.isNotEmpty(request.getParameter("secureToken")) )
        {
            secureToken = request.getParameter("secureToken");
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
        if (StringUtils.isNotEmpty(request.getParameter("total")))
        {
            total = Double.parseDouble(request.getParameter("total"));
        }
        if (StringUtils.isNotEmpty(request.getParameter("voucher")))
        {
            voucher = request.getParameter("voucher");
        }




        // DIVISION: shoppingcart-edit
        Division edit = body.addInteractiveDivision("shoppingcart-edit",contextPath+"/shoppingcart",Division.METHOD_MULTIPART,"primary administrative shoppingcart");
        edit.setHead(T_head1);


        if (errors.contains("shoppingcart_country")) {
            Para problem = edit.addPara();
            problem.addHighlight("bold").addContent(T_name_exists);
        }

        if (errors.contains("shoppingcart_currency")) {
            Para problem = edit.addPara();
            problem.addHighlight("bold").addContent(T_name_exists);
        }


        List identity = edit.addList("form",List.TYPE_FORM);
        identity.setHead(T_head2.parameterize(title));


        identity.addLabel(T_name);
        identity.addItem(title);
        identity.addLabel(T_name1);
        identity.addItem(eperson.getFullName());


        if (admin)
        {
            Text voucherField = identity.addItem().addText("voucher");
            voucherField.setLabel(T_name2);
            voucherField.setValue(voucher);

            Text secureTokenField = identity.addItem().addText("secureToken");
            secureTokenField.setLabel(T_name3);
            secureTokenField.setValue(secureToken);

            Text transactionIdField = identity.addItem().addText("transactionId");
            transactionIdField.setLabel(T_name4);
            transactionIdField.setValue(transactionId);

            Text statusField = identity.addItem().addText("status");
            statusField.setRequired();
            statusField.setLabel(T_name5);
            statusField.setValue(status);

            Text currencyField = identity.addItem().addText("currency");
            currencyField.setRequired();
            currencyField.setLabel(T_name6);
            currencyField.setValue(currency);

            Text totalField = identity.addItem().addText("total");
            totalField.setRequired();
            totalField.setLabel(T_name7);
            totalField.setValue(Double.toString(total));

            Text countryField = identity.addItem().addText("country");
            countryField.setRequired();
            countryField.setLabel(T_name8);
            countryField.setValue(country);
//            if (errors.contains("property_name_uniq"))
//            {
//                name.addError(T_name_exists);
//            }
//            else if (errors.contains("property_name"))
//            {
//                name.addError(T_error_name);
//            }

        }
        else
        {
            identity.addLabel(T_name2);
            identity.addItem(voucher);
            identity.addLabel(T_name3);
            identity.addItem(secureToken);
            identity.addLabel(T_name4);
            identity.addItem(transactionId);
            identity.addLabel(T_name5);
            identity.addItem(status);
            identity.addLabel(T_name6);
            identity.addItem(currency);
            identity.addLabel(T_name7);
            identity.addItem(Double.toString(total));
            identity.addLabel(T_name8);
            identity.addItem(country);
        }
        Item buttons = identity.addItem();
        buttons.addButton("submit_cancel").setValue(T_submit_cancel);
         if(admin)
         {
             buttons.addButton("submit_save").setValue(T_submit_save);
         }
        edit.addHidden("administrative-continue").setValue(knot.getId());
    }

}