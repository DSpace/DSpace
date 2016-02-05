/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.authority.scheme;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Locale;

import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;

import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.*;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.authority.AuthorityObject;

/**
 * Present the user with all the scheme metadata fields so that they
 * can describe the new scheme before being created. If the user's
 * input is incorrect in someway then they may be returning here with 
 * some fields in error. In particular there is a special case for the 
 * condition when the email-address entered is already in use by 
 * another user.
 *
 * @author Alexey a
 */
public class AddSchemeForm extends AbstractDSpaceTransformer
{
    /** Language Strings */
    private static final Message T_dspace_home =
            message("xmlui.general.dspace_home");

    private static final Message T_scheme_trail =
            message("xmlui.administrative.scheme.general.scheme_trail");

    private static final Message T_title =
            message("xmlui.administrative.scheme.AddSchemeForm.title");

    private static final Message T_trail =
            message("xmlui.administrative.scheme.AddSchemeForm.trail");

    private static final Message T_head1 =
            message("xmlui.administrative.scheme.AddSchemeForm.head1");

    private static final Message T_email_taken =
            message("xmlui.administrative.scheme.AddSchemeForm.email_taken");

    private static final Message T_head2 =
            message("xmlui.administrative.scheme.AddSchemeForm.head2");


    private static final Message T_top_scheme =
            message("xmlui.administrative.scheme.AddSchemeForm.top_scheme");

    private static final Message T_submit_create =
            message("xmlui.administrative.scheme.AddSchemeForm.submit_create");

    private static final Message T_submit_cancel =
            message("xmlui.general.cancel");


    /** Language string used from other aspects: */

    private static final Message T_email_address =
            message("xmlui.Scheme.EditProfile.email_address");

    private static final Message T_first_name =
            message("xmlui.Scheme.EditProfile.first_name");

    private static final Message T_last_name =
            message("xmlui.Scheme.EditProfile.last_name");

    private static final Message T_telephone =
            message("xmlui.Scheme.EditProfile.telephone");


    public void addPageMeta(PageMeta pageMeta) throws WingException
    {
        pageMeta.addMetadata("title").addContent(T_title);
        pageMeta.addTrailLink(contextPath + "/", T_dspace_home);
        pageMeta.addTrailLink(contextPath + "/admin/scheme",T_scheme_trail);
        pageMeta.addTrail().addContent(T_trail);
    }


    public void addBody(Body body) throws WingException, SQLException, AuthorizeException
    {
        // Get all our parameters
        Request request = ObjectModelHelper.getRequest(objectModel);

        String errorString = parameters.getParameter("errors",null);
        ArrayList<String> errors = new ArrayList<String>();
        if (errorString != null&&errorString.length()>0)
        {
            for (String error : errorString.split(","))
            {
                errors.add(error);
            }
        }

        String language = request.getParameter("language");
        String status = request.getParameter("status");


        // DIVISION: scheme-add
        Division add = body.addInteractiveDivision("scheme-add",contextPath+"/admin/scheme",Division.METHOD_POST,"primary thesaurus scheme");

        add.setHead(T_head1);


        List attribute = add.addList("identity",List.TYPE_FORM);
        attribute.setHead(T_head2);

        attribute.addLabel("Status");
        attribute.addItem().addText("status").setValue(status);
        attribute.addLabel("Language");
        Select languageSelect = attribute.addItem().addSelect("lang");
        for(String lang: Locale.getISOLanguages()){
            languageSelect.addOption(lang,lang);
        }
        languageSelect.setOptionSelected("en");

        Item buttons = attribute.addItem();
        buttons.addButton("submit_save").setValue(T_submit_create);
        buttons.addButton("submit_cancel").setValue(T_submit_cancel);

        add.addHidden("administrative-continue").setValue(knot.getId());
    }

}
