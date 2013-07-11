package org.dspace.app.xmlui.aspect.shoppingcart;

/**
 * User: lantian @ atmire . com
 * Date: 7/11/13
 * Time: 11:39 AM
 */

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Random;

import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;

import org.apache.commons.lang.RandomStringUtils;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.*;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.paymentsystem.Voucher;

/**
 * Present the user with all the voucher metadata fields so that they
 * can describe the new voucher before being created. If the user's
 * input is incorrect in someway then they may be returning here with
 * some fields in error. In particular there is a special case for the
 * condition when the email-address entered is already in use by
 * another user.
 *
 * @author Alexey Maslov
 */
public class AddVoucherForm extends AbstractDSpaceTransformer
{
    /** Language Strings */
    private static final Message T_dspace_home =
            message("xmlui.general.dspace_home");

    private static final Message T_voucher_trail =
            message("xmlui.voucher.general.voucher_trail");

    private static final Message T_title =
            message("xmlui.voucher.AddVoucherForm.title");

    private static final Message T_trail =
            message("xmlui.voucher.AddVoucherForm.trail");

    private static final Message T_head1 =
            message("xmlui.voucher.AddVoucherForm.head1");

    private static final Message T_head2 =
            message("xmlui.voucher.AddVoucherForm.head2");

    private static final Message T_error_name_null =
            message("xmlui.voucher.AddVoucherForm.error_name_null");

    private static final Message T_submit_create =
            message("xmlui.voucher.AddVoucherForm.submit_create");

    private static final Message T_submit_cancel =
            message("xmlui.general.cancel");

    private static final Message T_number =
            message("xmlui.voucher.AddVoucherForm.number");

    private static final Message T_customer =
            message("xmlui.voucher.AddVoucherForm.customer");

    private static final Message T_expiration =
            message("xmlui.voucher.AddVoucherForm.explanation");


    public void addPageMeta(PageMeta pageMeta) throws WingException
    {
        pageMeta.addMetadata("title").addContent(T_title);
        pageMeta.addTrailLink(contextPath + "/", T_dspace_home);
        pageMeta.addTrailLink(contextPath + "/voucher",T_voucher_trail);
        pageMeta.addTrail().addContent(T_trail);
    }


    public void addBody(Body body) throws WingException, SQLException, AuthorizeException
    {
        // Get all our parameters
        Request request = ObjectModelHelper.getRequest(objectModel);

        String errorString = parameters.getParameter("errors",null);
        ArrayList<String> errors = new ArrayList<String>();
        if (errorString != null)
        {
            for (String error : errorString.split(","))
            {
                errors.add(error);
            }
        }
        Division add = null;

        add = body.addInteractiveDivision("voucher-add",contextPath+"/voucher",Division.METHOD_MULTIPART,"primary administrative voucher");


        String customerCode  = request.getParameter("customerCode");
        String customerName = null;
        if(customerCode!=null){

            //todo:find the customer
            customerName = "customer";
        }

        add.setHead(T_head1);

        List identity = add.addList("identity",List.TYPE_FORM);
        identity.setHead(T_head2);

        identity.addLabel(T_customer);
        Text codeField = identity.addItem().addText("customerCode");
        codeField.setValue(customerCode);
        codeField.setHelp(customerName);

        identity.addLabel(T_number);
        Text totalNumberField = identity.addItem().addText("totalNumber");


        if (errors.contains("totalNumber")) {
            Para problem = add.addPara();
            problem.addHighlight("bold").addContent(T_error_name_null);
        }

        identity.addLabel(T_expiration);
        Text explanation = identity.addItem().addText("explanation");

        Item buttons = identity.addItem();
        buttons.addButton("submit_save").setValue(T_submit_create);
        buttons.addButton("submit_cancel").setValue(T_submit_cancel);

        identity.addItemXref("reset","reset");
        add.addHidden("administrative-continue").setValue(knot.getId());

    }

}

