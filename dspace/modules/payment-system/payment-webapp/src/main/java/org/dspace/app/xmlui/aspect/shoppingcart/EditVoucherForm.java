package org.dspace.app.xmlui.aspect.shoppingcart;

/**
 * User: lantian @ atmire . com
 * Date: 7/11/13
 * Time: 11:20 AM
 */


import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Properties;

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


public class EditVoucherForm  extends AbstractDSpaceTransformer
{
    /** Language Strings */
    private static final Message T_dspace_home =
            message("xmlui.general.dspace_home");

    private static final Message T_submit_save =
            message("xmlui.general.save");

    private static final Message T_submit_cancel =
            message("xmlui.general.cancel");

    private static final Message T_title =
            message("xmlui.property.EditVoucherForm.title");

    private static final Message T_trail =
            message("xmlui.property.EditVoucherForm.trail");

    private static final Message T_head1 =
            message("xmlui.property.EditVoucherForm.head1");

    private static final Message T_name_exists =
            message("xmlui.property.EditVoucherForm.email_taken");
    private static final Message T_name_null =
            message("xmlui.property.EditVoucherForm.email_null");


    private static final Message T_head2 =
            message("xmlui.property.EditVoucherForm.head2");


    /** Language string used: */

    private static final Message T_name =
            message("xmlui.voucher.EditProfile.name");

    private static final Message T_name1 =
            message("xmlui.voucher.EditProfile.name1");

    private static final Message T_name2 =
            message("xmlui.voucher.EditProfile.name2");

    private static final Message T_name3 =
            message("xmlui.voucher.EditProfile.name3");
    private static final Message T_name4 =
            message("xmlui.voucher.EditProfile.name4");

    private static final Message T_name5 =
            message("xmlui.voucher.EditProfile.name5");
    private static final Message T_name6 =
            message("xmlui.voucher.EditProfile.name6");

    public void addPageMeta(PageMeta pageMeta) throws WingException
    {
        pageMeta.addMetadata("title").addContent(T_title);
        pageMeta.addTrailLink(contextPath + "/", T_dspace_home);
        pageMeta.addTrailLink(contextPath + "/admin/voucher",T_trail);
        pageMeta.addTrail().addContent(T_trail);
    }


    public void addBody(Body body) throws WingException, SQLException, AuthorizeException
    {
        // Get all our parameters
        boolean admin = AuthorizeManager.isCuratorOrAdmin(context);

        Request request = ObjectModelHelper.getRequest(objectModel);

        // Get our parameters;
        int voucherID = parameters.getParameterAsInteger("voucher_id",-1);
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
        Voucher voucher = Voucher.findById(context, voucherID);

        if (voucher == null)
        {
            throw new UIException("Unable to find voucher for id:" + voucherID);
        }

        String code = voucher.getCode();
        String status = voucher.getStatus();
        Date creationDate =  voucher.getCreation();
        String creation = creationDate.toString().toString();

        if (StringUtils.isNotEmpty(request.getParameter("status")))
        {
            status = request.getParameter("status");
        }

        // DIVISION: voucher-edit
        Division edit = body.addInteractiveDivision("voucher-edit",contextPath+"/admin/voucher",Division.METHOD_MULTIPART,"primary administrative voucher");
        edit.setHead(T_head1);


        if (errors.contains("code_null")) {
            Para problem = edit.addPara();
            problem.addHighlight("bold").addContent(T_name_null);
        }

        if (errors.contains("code_uniq")) {
            Para problem = edit.addPara();
            problem.addHighlight("bold").addContent(T_name_exists);
        }


        List identity = edit.addList("form",List.TYPE_FORM);
        identity.setHead(T_head2.parameterize(code));
        identity.addLabel(T_name2);
        identity.addItem().addContent(code);

        if (admin)
        {

            Select statusField = identity.addItem().addSelect("status");
            statusField.setLabel(T_name3);
            statusField.addOption(Voucher.STATUS_OPEN,Voucher.STATUS_OPEN);
            statusField.addOption(Voucher.STATUS_USED,Voucher.STATUS_USED);
            statusField.setOptionSelected(status);

            identity.addLabel(T_name4);
            identity.addItem(creation);

            identity.addLabel(T_name5);
            if(voucher.getCustomer()!=null)
            identity.addItem(voucher.getCustomer());
            else
            identity.addItem().addContent("");

            identity.addLabel(T_name6);
            if(voucher.getCustomerName()!=null)
            identity.addItem(voucher.getCustomerName());
            else
                identity.addItem().addContent("");

        }
        else
        {
            identity.addLabel(T_name2);
            identity.addItem(code);
            identity.addLabel(T_name3);
            identity.addItem(status);
            identity.addLabel(T_name4);
            identity.addItem(creation);

            identity.addLabel(T_name5);
            if(voucher.getCustomer()!=null)
            identity.addItem(voucher.getCustomer());
            else
                identity.addItem().addContent("");
            identity.addLabel(T_name6);
            if(voucher.getCustomerName()!=null)
            identity.addItem(voucher.getCustomerName());
            else
                identity.addItem().addContent("");
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
