/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.authority.term;

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
import org.dspace.content.authority.Concept;

/**
 * Present the user with all the term metadata fields so that they
 * can describe the new term before being created. If the user's
 * input is incorrect in someway then they may be returning here with 
 * some fields in error. In particular there is a special case for the 
 * condition when the email-address entered is already in use by 
 * another user.
 *
 * @author Alexey Maslov
 */
public class AddTermForm extends AbstractDSpaceTransformer
{
    /** Language Strings */
    private static final Message T_dspace_home =
            message("xmlui.general.dspace_home");

    private static final Message T_term_trail =
            message("xmlui.administrative.term.general.epeople_trail");

    private static final Message T_title =
            message("xmlui.administrative.term.AddTermForm.title");

    private static final Message T_trail =
            message("xmlui.administrative.term.AddTermForm.trail");

    private static final Message T_head1 =
            message("xmlui.administrative.term.AddTermForm.head1");

    private static final Message T_email_taken =
            message("xmlui.administrative.term.AddTermForm.email_taken");

    private static final Message T_head2 =
            message("xmlui.administrative.term.AddTermForm.head2");

    private static final Message T_error_email_unique =
            message("xmlui.administrative.term.AddTermForm.error_email_unique");

    private static final Message T_error_email =
            message("xmlui.administrative.term.AddTermForm.error_email");

    private static final Message T_error_fname =
            message("xmlui.administrative.term.AddTermForm.error_fname");

    private static final Message T_error_lname =
            message("xmlui.administrative.term.AddTermForm.error_lname");

    private static final Message T_req_certs =
            message("xmlui.administrative.term.AddTermForm.req_certs");

    private static final Message T_can_log_in =
            message("xmlui.administrative.term.AddTermForm.can_log_in");

    private static final Message T_submit_create =
            message("xmlui.administrative.term.AddTermForm.submit_create");

    private static final Message T_submit_cancel =
            message("xmlui.general.cancel");


    /** Language string used from other aspects: */

    private static final Message T_email_address =
            message("xmlui.Term.EditProfile.email_address");

    private static final Message T_first_name =
            message("xmlui.Term.EditProfile.first_name");

    private static final Message T_last_name =
            message("xmlui.Term.EditProfile.last_name");

    private static final Message T_telephone =
            message("xmlui.Term.EditProfile.telephone");


    public void addPageMeta(PageMeta pageMeta) throws WingException
    {
        pageMeta.addMetadata("title").addContent(T_title);
        pageMeta.addTrailLink(contextPath + "/", T_dspace_home);
        pageMeta.addTrailLink(contextPath + "/admin/term",T_term_trail);
        pageMeta.addTrail().addContent(T_trail);
    }


    public void addBody(Body body) throws WingException, SQLException, AuthorizeException
    {
        // Get all our parameters
        Request request = ObjectModelHelper.getRequest(objectModel);
        String conceptId = parameters.getParameter("conceptID",null);
        String schemeId = parameters.getParameter("schemeID",null);
        String errorString = parameters.getParameter("errors",null);
        ArrayList<String> errors = new ArrayList<String>();
        if (errorString != null)
        {
            for (String error : errorString.split(","))
            {
                errors.add(error);
            }
        }
        String source = request.getParameter("source");
        String language = request.getParameter("language");
        String status = request.getParameter("status");
        String literalForm = request.getParameter("literalForm");

        // DIVISION: term-add
        Division add = null;
        String formUrl = "/admin/term";
        if(conceptId!=null)
        {
            formUrl = "/admin/concept";
        }
        if(schemeId!=null)
        {
            formUrl = "/admin/term";

        }
        add = body.addInteractiveDivision("term-add",formUrl,Division.METHOD_POST,"primary administrative term");

        add.setHead(T_head1);


        List identity = add.addList("identity",List.TYPE_FORM);

        if(conceptId!=null)
        {
            identity.setHead("Add term to concept:"+conceptId);
            identity.addItem().addHidden("concept").setValue(conceptId);
        }
        else{
            identity.setHead(T_head2);
        }

        identity.addLabel("Preferred Term");
        CheckBox preferred = identity.addItem().addCheckBox("preferred");
        preferred.addOption("1","Preferred Term");

        identity.addLabel("Source");
        identity.addItem().addText("source").setValue(source);
        identity.addLabel("Status");
        Select statusSelect=identity.addItem().addSelect("status");
        statusSelect.addOption(Concept.Status.ACCEPTED.name(), "Accepted");
        statusSelect.addOption(Concept.Status.CANDIDATE.name(), "Candidate");
        statusSelect.addOption(Concept.Status.WITHDRAWN.name(), "Withdraw");
        if(status!=null)
        {
            if(status.equals(Concept.Status.CANDIDATE.name()))
            {
                statusSelect.setOptionSelected(Concept.Status.CANDIDATE.name());
            }
            else if(status.equals(Concept.Status.WITHDRAWN.name()))
            {
                statusSelect.setOptionSelected(Concept.Status.WITHDRAWN.name());
            }
            else if(status.equals(Concept.Status.ACCEPTED.name()))
            {
                statusSelect.setOptionSelected(Concept.Status.ACCEPTED.name());
            }
        }
        identity.addLabel("Literal Form");
        Text literalFormText =identity.addItem().addText("literalForm");
        literalFormText.setValue(literalForm);
        if(errors.contains("literalForm")) {
            literalFormText.addError("Literal Form must be unique");
        }
        identity.addLabel("Language");
        Select languageSelect = identity.addItem().addSelect("lang");
        for(String lang: Locale.getISOLanguages()){
            languageSelect.addOption(lang,lang);
        }
        languageSelect.setOptionSelected("en");

        Item buttons = identity.addItem();
        buttons.addButton("submit_save").setValue(T_submit_create);
        buttons.addButton("submit_cancel").setValue(T_submit_cancel);

        add.addHidden("administrative-continue").setValue(knot.getId());
    }

}
