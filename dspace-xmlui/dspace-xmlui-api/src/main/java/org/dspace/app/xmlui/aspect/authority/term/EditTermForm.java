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

import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.*;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.authority.Concept;
import org.dspace.content.authority.Term;

/**
 * Edit an existing Term, display all the term's metadata
 * along with two special options two reset the term's
 * password and delete this user. 
 *
 * @author Alexey Maslov
 */
public class EditTermForm extends AbstractDSpaceTransformer
{
    /** Language Strings */
    private static final Message T_dspace_home =
            message("xmlui.general.dspace_home");

    private static final Message T_submit_save =
            message("xmlui.general.save");

    private static final Message T_submit_cancel =
            message("xmlui.general.cancel");

    private static final Message T_title =
            message("xmlui.administrative.term.EditTermForm.title");

    private static final Message T_term_trail =
            message("xmlui.administrative.term.general.epeople_trail");

    private static final Message T_trail =
            message("xmlui.administrative.term.EditTermForm.trail");

    private static final Message T_head1 =
            message("xmlui.administrative.term.EditTermForm.head1");

    private static final Message T_literalForm_taken =
            message("xmlui.administrative.term.EditTermForm.literalForm_taken");

    private static final Message T_head2 =
            message("xmlui.administrative.term.EditTermForm.head2");

    private static final Message T_error_literalForm_unique =
            message("xmlui.administrative.term.EditTermForm.error_literalForm_unique");

    private static final Message T_error_literalForm =
            message("xmlui.administrative.term.EditTermForm.error_literalForm");

    private static final Message T_error_status =
            message("xmlui.administrative.term.EditTermForm.error_status");

    private static final Message T_error_source =
            message("xmlui.administrative.term.EditTermForm.error_source");

    private static final Message T_move_term =
            message("xmlui.administrative.term.EditTermForm.move_term");

    private static final Message T_move_term_help =
            message("xmlui.administrative.term.EditTermForm.move_term_help");

    private static final Message T_submit_delete =
            message("xmlui.administrative.term.EditTermForm.submit_delete");

    private static final Message T_submit_login_as =
            message("xmlui.administrative.term.EditTermForm.submit_login_as");

    /** Language string used: */

    private static final Message T_literalForm_address =
            message("xmlui.Term.EditProfile.literalForm_address");

    private static final Message T_status =
            message("xmlui.Term.EditProfile.status");

    private static final Message T_source =
            message("xmlui.Term.EditProfile.source");

    private static final Message T_identifier =
            message("xmlui.Term.EditProfile.identifier");
    private static final Message T_lang =
            message("xmlui.Term.EditProfile.language");




    public void addPageMeta(PageMeta pageMeta) throws WingException
    {
        pageMeta.addMetadata("title").addContent(T_title);
        pageMeta.addTrailLink(contextPath + "/", T_dspace_home);
        int termID = parameters.getParameterAsInteger("term",-1);
        try{
            Term term = Term.find(context, termID);
            if(term!=null)
            {
                pageMeta.addTrailLink(contextPath + "/term/"+term.getID(), term.getLiteralForm());
            }
        }catch (Exception e)
        {
            return;
        }
        pageMeta.addTrail().addContent(T_trail);
    }


    public void addBody(Body body) throws WingException, SQLException, AuthorizeException
    {
        // Get all our parameters
        boolean admin = AuthorizeManager.isAdmin(context);

        Request request = ObjectModelHelper.getRequest(objectModel);

        // Get our parameters;
        int termID = parameters.getParameterAsInteger("term",-1);

        String conceptId = parameters.getParameter("conceptID",null);
        String schemeId = parameters.getParameter("schemeID",null);
        String formUrl = "/admin/term";
        if(conceptId!=null)
        {
            formUrl = "/admin/concept";
        }
        if(schemeId!=null)
        {
            formUrl = "/admin/term";

        }
        String errorString = parameters.getParameter("errors",null);
        ArrayList<String> errors = new ArrayList<String>();
        if (errorString != null)
        {
            for (String error : errorString.split(","))
            {
                errors.add(error);
            }
        }

        // Grab the person in question
        Term term = Term.find(context, termID);

        if (term == null)
        {
            throw new UIException("Unable to find term for id:" + termID);
        }

        String literalForm = term.getLiteralForm();
        String status = term.getStatus();
        String source  = term.getSource();
        String identifier = term.getIdentifier();
        String language = term.getLang();

        if (request.getParameter("literalForm") != null)
        {
            literalForm = request.getParameter("literalForm_address");
        }

        if (request.getParameter("status") != null)
        {
            status = request.getParameter("status");
        }
        if (request.getParameter("source") != null)
        {
            source = request.getParameter("source");
        }

        if (request.getParameter("lang") != null)
        {
            language = request.getParameter("lang");
        }


        // DIVISION: term-edit
        Division edit = body.addInteractiveDivision("term-edit",formUrl,Division.METHOD_POST,"primary administrative term");
        edit.setHead(T_head1);


        if (errors.contains("term_literalForm_key")) {
            Para problem = edit.addPara();
            problem.addHighlight("bold").addContent(T_literalForm_taken);
        }


        List identity = edit.addList("form",List.TYPE_FORM);
        identity.setHead(T_head2.parameterize(term.getName()));
        identity.addItem().addHidden("termId").setValue(term.getID());
        if (admin)
        {
            Text literalFormText = identity.addItem().addText("literalForm");
            literalFormText.setRequired();
            literalFormText.setLabel(T_literalForm_address);
            literalFormText.setValue(literalForm);
            if (errors.contains("term_literalForm_key"))
            {
                literalFormText.addError(T_error_literalForm_unique);
            }
            else if (errors.contains("literalForm"))
            {
                literalFormText.addError(T_error_literalForm);
            }
        }
        else
        {
            identity.addLabel(T_literalForm_address);
            identity.addItem(literalForm);
        }

        if (admin)
        {
            identity.addLabel("Status");
            Select statusSelect=identity.addItem().addSelect("status");
            statusSelect.addOption(Concept.Status.ACCEPTED.name(), "Accepted");
            statusSelect.addOption(Concept.Status.CANDIDATE.name(), "Candidate");
            statusSelect.addOption(Concept.Status.WITHDRAWN.name(),"Withdraw");
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
            if (errors.contains("status"))
            {
                statusSelect.addError(T_error_status);
            }
        }
        else
        {
            identity.addLabel(T_status);
            identity.addItem(status);
        }

        if (admin)
        {
            Text sourceText = identity.addItem().addText("source");
            sourceText.setRequired();
            sourceText.setLabel(T_source);
            sourceText.setValue(source);
            if (errors.contains("source"))
            {
                sourceText.addError(T_error_source);
            }
        }
        else
        {
            identity.addLabel(T_source);
            identity.addItem(source);
        }

        identity.addLabel(T_identifier);
        identity.addItem(identifier);


        if (admin)
        {
            Text langText = identity.addItem().addText("lang");
            langText.setLabel(T_lang);
            langText.setValue(language);
        }
        else
        {
            identity.addLabel(T_lang);
            identity.addItem(language);
        }


        Item buttons = identity.addItem();
        if (admin)
        {
            buttons.addButton("submit_save").setValue(T_submit_save);
            buttons.addButton("submit_delete").setValue(T_submit_delete);
        }
        buttons.addButton("submit_cancel").setValue(T_submit_cancel);

        edit.addHidden("administrative-continue").setValue(knot.getId());
    }

}
