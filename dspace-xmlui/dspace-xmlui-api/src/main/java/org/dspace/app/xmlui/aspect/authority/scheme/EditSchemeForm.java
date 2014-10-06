/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.authority.scheme;

import java.sql.SQLException;
import java.util.*;

import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.*;
import org.dspace.app.xmlui.wing.element.Item;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.authority.Concept;
import org.dspace.content.authority.Scheme;

/**
 * Edit an existing Scheme, display all the scheme's metadata
 * along with two special options two reset the scheme's
 * password and delete this user. 
 *
 * @author Alexey Maslov
 */
public class EditSchemeForm extends AbstractDSpaceTransformer
{
    /** Language Strings */
    private static final Message T_dspace_home =
            message("xmlui.general.dspace_home");

    private static final Message T_submit_save =
            message("xmlui.general.save");

    private static final Message T_submit_cancel =
            message("xmlui.general.cancel");

    private static final Message T_title =
            message("xmlui.administrative.scheme.EditSchemeForm.title");

    private static final Message T_scheme_trail =
            message("xmlui.administrative.scheme.general.scheme_trail");

    private static final Message T_trail =
            message("xmlui.administrative.scheme.EditSchemeForm.trail");

    private static final Message T_head1 =
            message("xmlui.administrative.scheme.EditSchemeForm.head1");

    private static final Message T_literalForm_taken =
            message("xmlui.administrative.scheme.EditSchemeForm.literalForm_taken");

    private static final Message T_head2 =
            message("xmlui.administrative.scheme.EditSchemeForm.head2");

    private static final Message T_error_literalForm_unique =
            message("xmlui.administrative.scheme.EditSchemeForm.error_literalForm_unique");

    private static final Message T_error_literalForm =
            message("xmlui.administrative.scheme.EditSchemeForm.error_literalForm");

    private static final Message T_error_status =
            message("xmlui.administrative.scheme.EditSchemeForm.error_status");

    private static final Message T_error_source =
            message("xmlui.administrative.scheme.EditSchemeForm.error_source");

    private static final Message T_move_concept =
            message("xmlui.administrative.scheme.EditSchemeForm.move_concept");

    private static final Message T_move_concept_help =
            message("xmlui.administrative.scheme.EditSchemeForm.move_concept_help");

    /** Language string used: */

    private static final Message T_status =
            message("xmlui.Scheme.EditProfile.status");


    private static final Message T_identifier =
            message("xmlui.Scheme.EditProfile.identifier");
    private static final Message T_lang =
            message("xmlui.Scheme.EditProfile.language");

    private static final Message T_search_column1 =
            message("xmlui.aspect.authority.scheme.ManageSchemeMain.search_column1");

    private static final Message T_search_column2 =
            message("xmlui.aspect.authority.scheme.ManageSchemeMain.search_column2");

    private static final Message T_search_column3 =
            message("xmlui.aspect.authority.scheme.ManageSchemeMain.search_column3");

    private static final Message T_search_column4 =
            message("xmlui.aspect.authority.scheme.ManageSchemeMain.search_column4");

    private static final Message T_submit_delete =
            message("xmlui.aspect.authority.scheme.ManageSchemeMain.submit_delete");

    public void addPageMeta(PageMeta pageMeta) throws WingException
    {
        pageMeta.addMetadata("title").addContent(T_title);
        pageMeta.addTrailLink(contextPath + "/", T_dspace_home);
        int schemeID = parameters.getParameterAsInteger("scheme",-1);
        try{
            Scheme scheme = Scheme.find(context, schemeID);
            if(scheme!=null)
            {
                pageMeta.addTrailLink(contextPath + "/scheme/"+scheme.getID(), scheme.getIdentifier());
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
        int schemeID = parameters.getParameterAsInteger("scheme",-1);
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
        Scheme scheme = Scheme.find(context, schemeID);

        if (scheme == null)
        {
            throw new UIException("Unable to find scheme for id:" + schemeID);
        }

        String status = scheme.getStatus();
        String identifier = scheme.getIdentifier();
        String language = scheme.getLang();

        if (request.getParameter("status") != null)
        {
            status = request.getParameter("status");
        }

        if (request.getParameter("lang") != null)
        {
            language = request.getParameter("lang");
        }


        // DIVISION: scheme-edit
        Division edit = body.addInteractiveDivision("scheme-edit",contextPath+request.getRequestURI(),Division.METHOD_POST,"primary thesaurus scheme");
        edit.setHead(T_head1);


        if (errors.contains("scheme_literalForm_key")) {
            Para problem = edit.addPara();
            problem.addHighlight("bold").addContent(T_literalForm_taken);
        }


        List identity = edit.addList("form",List.TYPE_FORM);
        identity.setHead(T_head2.parameterize(scheme.getIdentifier()));
        identity.addItem().addHidden("schemeId").setValue(scheme.getID());
        if (admin)
        {
            Text statusText = identity.addItem().addText("status");
            statusText.setLabel(T_status);
            statusText.setValue(status);
        }
        else
        {
            identity.addLabel(T_status);
            identity.addItem(status);
        }

        identity.addLabel(T_identifier);
        identity.addItem(identifier);


        if (admin)
        {
            Select langText = identity.addItem().addSelect("lang");
            langText.setLabel(T_lang);
            for(String lang: Locale.getISOLanguages()){
                langText.addOption(lang,lang);
            }
            langText.setOptionSelected(language);
        }
        else
        {
            identity.addLabel(T_lang);
            identity.addItem(language);
        }

        // Get list of member groups
        Concept[] concepts = scheme.getConcepts();
        Table table = edit.addTable("scheme-search-table", concepts.length + 1, 1);
        //table.setHead("Concept in current scheme");
        //Row header = table.addRow(Row.ROLE_HEADER);
        //Check if a system administrator
        boolean isSystemAdmin = AuthorizeManager.isAdmin(this.context);

//        if(isSystemAdmin)
//        {
//            header.addCell().addContent(T_search_column1);
//        }
//        header.addCell().addContent(T_search_column2);
//        header.addCell().addContent("Date Created");
//        header.addCell().addContent("Identifier");
//        CheckBox selectConcept;
//
//        if(isSystemAdmin)
//        {
//            header.addCell().addContent("actions");
//        }
//        for(Concept concept : concepts)
//        {
//            Row row = table.addRow();
//            if(isSystemAdmin)
//            {
//                selectConcept= row.addCell().addCheckBox("select_schemes");
//                selectConcept.addOption(concept.getID());
//            }
//            row.addCellContent(Integer.toString(concept.getID()));
//            if(isSystemAdmin){
//                row.addCell().addXref("/concept/"+concept.getID(), concept.getCreated().toString());
//                row.addCell().addXref("/concept/"+concept.getID(), concept.getPreferredLabel());
//                Cell actionCell = row.addCell() ;
//                actionCell.addXref("/admin/scheme/"+schemeID+"/concept/"+concept.getID()+"/edit?administrative-continue="+knot.getId(), "Edit");
//                actionCell.addXref("/admin/scheme/"+schemeID+"/concept/"+concept.getID()+"/remove?administrative-continue="+knot.getId(), "Remove");
//            }
//            else
//            {
//                row.addCell().addContent(concept.getCreated().toString());
//                row.addCell().addContent(concept.getStatus());
//            }
//        }




        Item buttons = identity.addItem();
        if (admin)
        {
            buttons.addButton("submit_save").setValue(T_submit_save);
            buttons.addButton("submit_delete").setValue(T_submit_delete);
            buttons.addButton("submit_cancel").setValue("Cancel");
        }

        edit.addHidden("administrative-continue").setValue(knot.getId());
    }






}
