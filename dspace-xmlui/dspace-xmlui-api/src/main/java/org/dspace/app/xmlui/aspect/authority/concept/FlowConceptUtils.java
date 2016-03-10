/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.authority.concept;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.cocoon.environment.Request;
import org.apache.log4j.Logger;
import org.dspace.app.xmlui.aspect.administrative.FlowResult;
import org.dspace.app.xmlui.aspect.authority.FlowAuthorityMetadataValueUtils;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.authority.*;
import org.dspace.core.Constants;
import org.dspace.core.Context;


/**
 * Utility methods to processes actions on EPeople. These methods are used
 * exclusively from the administrative flow scripts.
 *
 * @author Scott Phillips
 */
public class FlowConceptUtils {

    /** log4j category */
    private static final Logger log = Logger.getLogger(FlowConceptUtils.class);

    /** Language Strings */
    private static final Message T_add_Concept_success_notice =
            new Message("default","xmlui.administrative.FlowConceptUtils.add_Concept_success_notice");

    private static final Message T_edit_Concept_success_notice =
            new Message("default","xmlui.administrative.FlowConceptUtils.edit_Concept_success_notice");

    private static final Message t_add_Concept_success_notice =
            new Message("default","xmlui.administrative.FlowConceptUtils.reset_password_success_notice");

    private static final Message t_delete_Concept_success_notice =
            new Message("default","xmlui.administrative.FlowConceptUtils.delete_Concept_success_notice");

    private static final Message t_delete_Concept_failed_notice =
            new Message("default","xmlui.administrative.FlowConceptUtils.delete_Concept_failed_notice");
    private static final Message t_delete_Term_success_notice =
            new Message("default","xmlui.administrative.FlowTermUtils.delete_Term_success_notice");

    private static final Message t_delete_Term_failed_notice =
            new Message("default","xmlui.administrative.FlowTermUtils.delete_Term_failed_notice");
    /**
     * Add a new Concept. This method will check that the email address,
     * first name, and last name are non empty. Also a check is performed
     * to see if the requested email address is already in use by another
     * user.
     *
     * @param context The current DSpace context
     * @param request The HTTP request parameters
     * @param objectModel Cocoon's object model
     * @return A process result's object.
     */
    public static FlowResult processAddConcept(Context context,String schemeId, Request request, Map objectModel) throws SQLException, AuthorizeException,NoSuchAlgorithmException
    {
        FlowResult result = new FlowResult();
        result.setContinue(false); // default to no continue
        Concept newConcept = null;
        Boolean topConcept    = (request.getParameter("topConcept") == null)  ? false : true;
        String language = request.getParameter("language");
        String status = request.getParameter("status");

        String value = request.getParameter("value");
        if(value==null||value.length()==0)
        {
            result.addError("value");
        }
        // No errors, so we try to create the Concept from the data provided
        if (result.getErrors() == null)
        {
            if(schemeId!=null&&schemeId.length()>0)
            {
                Scheme scheme = Scheme.find(context, Integer.parseInt(schemeId));
                newConcept = scheme.createConcept(context);
                newConcept.setStatus(context, status);
                newConcept.setLang(context, language);
                newConcept.setTopConcept(context, topConcept);
                Term newTerm = newConcept.createTerm(context, value,Term.prefer_term);
                context.commit();
                // success
                result.setContinue(true);
                result.setOutcome(true);
                result.setMessage(T_add_Concept_success_notice);
                result.setParameter("ConceptID", newConcept.getID());
            }
            else
            {
                result.addError("Please select a scheme to add a new concept");
            }
        }

        return result;
    }


    /**
     * Edit an Concept's metadata, the email address, first name, and last name are all
     * required. The user's email address can be updated but it must remain unique, if
     * the email address already exists then the an error is produced.
     *
     * @param context The current DSpace context
     * @param request The HTTP request parameters

     * @return A process result's object.
     */
    public static FlowResult processEditConcept(Context context,
                                                Request request, Map ObjectModel, int conceptID)
            throws SQLException, AuthorizeException
    {

        FlowResult result = new FlowResult();
        result.setContinue(false); // default to failure

        // Get all our request parameters

        String status = request.getParameter("status");
        Boolean topConcept    = (request.getParameter("topConcept") == null)  ? false : true;
        String identifier = request.getParameter("identifier");
        String language = request.getParameter("lang");
        //boolean certificate = (request.getParameter("certificate") != null) ? true : false;

        // No errors, so we edit the Concept with the data provided
        if (result.getErrors() == null)
        {
            // Grab the person in question
            Concept conceptModified = Concept.find(context, conceptID);
            Boolean originalTopConcept = conceptModified.getTopConcept();
            if (originalTopConcept == null || !originalTopConcept.equals(topConcept)) {
                conceptModified.setTopConcept(context, topConcept);
            }
            String originalStatus = conceptModified.getStatus();
            if (originalStatus == null || !originalStatus.equals(status)) {
                conceptModified.setStatus(context, status);
            }
            String originalLang = conceptModified.getLang();
            if (originalLang == null || !originalLang.equals(language)) {
                conceptModified.setLang(context, language);
            }

            context.commit();

            result.setContinue(true);
            result.setOutcome(true);
            // FIXME: rename this message
            result.setMessage(T_edit_Concept_success_notice);
        }

        // Everything was fine
        return result;
    }





    /**
     * Delete the epeople specified by the epeopleIDs parameter. This assumes that the
     * deletion has been confirmed.
     *
     * @param context The current DSpace context
     * @param conceptIds The unique id of the Concept being edited.
     * @return A process result's object.
     */
    public static FlowResult processDeleteConcept(Context context, String[] conceptIds) throws NumberFormatException, SQLException, AuthorizeException
    {
        FlowResult result = new FlowResult();

        List<String> unableList = new ArrayList<String>();
        for (String id : conceptIds)
        {
            Concept conceptDeleted = Concept.find(context, Integer.valueOf(id));
            try {
                conceptDeleted.delete(context);
            }
            catch (Exception epde)
            {
                int conceptDeletedId = conceptDeleted.getID();
                unableList.add(Integer.toString(conceptDeletedId));
            }
        }

        if (unableList.size() > 0)
        {
            result.setOutcome(false);
            result.setMessage(t_delete_Concept_failed_notice);

            String characters = null;
            for(String unable : unableList )
            {
                if (characters == null)
                {
                    characters = unable;
                }
                else
                {
                    characters += ", " + unable;
                }
            }

            result.setCharacters(characters);
        }
        else
        {
            result.setOutcome(true);
            result.setMessage(t_delete_Concept_success_notice);
        }


        return result;
    }


    public static String[] getPreferredTerms(Context context, int groupID) throws SQLException
    {
        // New group, just return an empty list
        if (groupID < 0)
        {
            return new String[0];
        }

        Concept group = Concept.find(context, groupID);

        if (group == null)
        {
            return new String[0];
        }

        Term[] epeople = group.getPreferredTerms();

        String[] epeopleIDs = new String[epeople.length];
        for (int i=0; i < epeople.length; i++)
        {
            epeopleIDs[i] = String.valueOf(epeople[i].getID());
        }

        return epeopleIDs;
    }

    public static String[] getParentConcepts(Context context, int groupID) throws SQLException
    {
        if (groupID < 0)
        {
            return new String[0];
        }

        Concept group = Concept.find(context, groupID);

        if (group == null)
        {
            return new String[0];
        }

        Concept[] groups = group.getParentConcepts();

        String[] groupIDs = new String[groups.length];
        for (int i=0; i < groups.length; i++)
        {
            groupIDs[i] = String.valueOf(groups[i].getID());
        }

        return groupIDs;
    }
    public static FlowResult processAddTerm(Context context,int conceptId,Request request)
            throws SQLException, AuthorizeException
    {

        FlowResult result = new FlowResult();
        result.setContinue(false); // default to failure
        Concept concept = (Concept) AuthorityObject.find(context, Constants.CONCEPT, conceptId);

        // Get all our request parameters
        String literalForm = request.getParameter("literalForm");
        String source =  request.getParameter("source");
        String status =  request.getParameter("status");
        String lang =  request.getParameter("lang");
        String preferred = request.getParameter("preferred");
        if (result.getErrors() == null)
        {
            if(preferred==null)
            {
                //add it as alt term
                preferred=Integer.toString(Term.alternate_term);
            }
            Term term = concept.createTerm(context, literalForm, Integer.parseInt(preferred));
            java.util.Date date = new java.util.Date();
            term.setCreated(context, date);

            if (literalForm == null) {
                term.setLiteralForm(context, literalForm);
            }

            if (source == null ) {
                term.setSource(context, source);
            }

            if (status == null ) {
                term.setStatus(context, status);
            }

            if (lang == null ) {
                term.setLang(context, lang);
            }

            term.setLastModified(context, date);
            context.commit();

            result.setContinue(true);
            result.setOutcome(true);
            // FIXME: rename this message
            result.setMessage(T_edit_Concept_success_notice);
        }

        // Everything was fine
        return result;
    }

    public static FlowResult processEditConceptMetadata(Context context, String id,Request request){
        return FlowAuthorityMetadataValueUtils.processEditMetadata(context, Constants.CONCEPT, id, request);
    }

    public static FlowResult doDeleteMetadataFromConcept(Context context, String id,Request request)throws SQLException, AuthorizeException, UIException, IOException {
        return FlowAuthorityMetadataValueUtils.doDeleteMetadata(context, Constants.CONCEPT,id, request);
    }

    public static FlowResult doAddMetadataToConcept(Context context, int id, Request request) throws SQLException, AuthorizeException, UIException, IOException
    {
        return FlowAuthorityMetadataValueUtils.doAddMetadata(context, Constants.CONCEPT,id, request);
    }

    public static void addTerm2Concept (Context context,String conceptId, String termId,Request request)throws SQLException,AuthorizeException{
        Concept concept = Concept.find(context, Integer.parseInt(conceptId));
        Term term = Term.find(context, Integer.parseInt(termId));
        boolean preferred = (request.getParameter("preferred") != null) ? true : false;
        int role_id = 2;
        if(preferred)
        {
            role_id = 1;
        }
        concept.addTerm(context, term,role_id);
        context.commit();
    }
    public static FlowResult processDeleteTerm(Context context, String conceptId,String[] termIds)throws SQLException,AuthorizeException{
        FlowResult result = new FlowResult();
        List<String> unableList = new ArrayList<String>();
        for (String id : termIds)
        {
            Concept2Term relationDeleted = Concept2Term.findByConceptAndTerm(context,Integer.parseInt(conceptId), Integer.valueOf(id));
            try {
                relationDeleted.delete(context);
            }
            catch (Exception epde)
            {
                int termDeletedId = relationDeleted.getRelationID();
                unableList.add(Integer.toString(termDeletedId));
            }
        }

        if (unableList.size() > 0)
        {
            result.setOutcome(false);
            result.setMessage(t_delete_Term_failed_notice);

            String characters = null;
            for(String unable : unableList )
            {
                if (characters == null)
                {
                    characters = unable;
                }
                else
                {
                    characters += ", " + unable;
                }
            }

            result.setCharacters(characters);
        }
        else
        {
            result.setOutcome(true);
            result.setMessage(t_delete_Term_success_notice);
        }


        return result;
    }


    public static FlowResult doAddConceptToConcept(Context context,String conceptId,Request request){

        FlowResult result = new FlowResult();
        result.setOutcome(false);
        String[] conceptIds = request.getParameterValues("select_concepts");
        ArrayList<String> error = new ArrayList<String>();
        try{
            Concept concept = (Concept)AuthorityObject.find(context,Constants.CONCEPT,Integer.parseInt(conceptId));
            for(String secondConceptId : conceptIds)
            {
                if(!conceptId.equals(secondConceptId)) {
                    Concept secondConcept = (Concept)AuthorityObject.find(context,Constants.CONCEPT,Integer.parseInt(secondConceptId));
                    if(secondConcept!=null){
                        concept.addChildConcept(context, secondConcept, Integer.parseInt(request.getParameter("roleId")));
                        context.commit();
                        result.setOutcome(true);
                        result.setMessage(t_add_Concept_success_notice);
                    }
                    else
                    {

                        error.add("need to select a concept");
                    }
                }
                else
                {
                    error.add("Please select a different concept");

                }
            }

        }catch (Exception e)
        {

        }
        result.setErrors(error);
        if(error==null||error.size()==0)
        {
            result.setContinue(true);
        }
        return result;
    }
}



