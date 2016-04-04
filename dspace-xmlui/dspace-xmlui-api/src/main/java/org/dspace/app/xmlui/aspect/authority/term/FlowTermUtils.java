/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.authority.term;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.cocoon.environment.Request;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.xmlui.aspect.administrative.FlowResult;
import org.dspace.app.xmlui.aspect.authority.FlowAuthorityMetadataValueUtils;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.authority.Concept;
import org.dspace.content.authority.Term;
import org.dspace.core.Constants;
import org.dspace.core.Context;


/**
 * Utility methods to processes actions on EPeople. These methods are used
 * exclusively from the administrative flow scripts.
 *
 * @author Scott Phillips
 */
public class FlowTermUtils {

    /** log4j category */
    private static final Logger log = Logger.getLogger(FlowTermUtils.class);

    /** Language Strings */
    private static final Message T_add_Term_success_notice =
            new Message("default","xmlui.administrative.FlowTermUtils.add_Term_success_notice");

    private static final Message T_edit_Term_success_notice =
            new Message("default","xmlui.administrative.FlowTermUtils.edit_Term_success_notice");

    private static final Message T_reset_password_success_notice =
            new Message("default","xmlui.administrative.FlowTermUtils.reset_password_success_notice");

    private static final Message t_delete_Term_success_notice =
            new Message("default","xmlui.administrative.FlowTermUtils.delete_Term_success_notice");

    private static final Message t_delete_Term_failed_notice =
            new Message("default","xmlui.administrative.FlowTermUtils.delete_Term_failed_notice");

    /**
     * Add a new Term. This method will check that the email address,
     * first name, and last name are non empty. Also a check is performed
     * to see if the requested email address is already in use by another
     * user.
     *
     * @param context The current DSpace context
     * @param request The HTTP request parameters
     * @param objectModel Cocoon's object model
     * @return A process result's object.
     */
    public static FlowResult processAddTerm(Context context, String conceptId,Request request, Map objectModel) throws SQLException, AuthorizeException
    {
        FlowResult result = new FlowResult();
        result.setContinue(false); // default to no continue

        // Get all our request parameters
        String literalForm = request.getParameter("literalForm");

        // If we have errors, the form needs to be resubmitted to fix those problems
        if (StringUtils.isEmpty(literalForm)|| Term.findByLiteralForm(context, literalForm)!=null)
        {
            result.addError("literalForm");
        }
        else
        {
            literalForm = literalForm.trim();
        }
        String source = request.getParameter("source");
        String language = request.getParameter("language");
        String status = request.getParameter("status");
        // No errors, so we try to create the Term from the data provided
        if (result.getErrors() == null)
        {
            if(conceptId!=null&&conceptId.length()>0)
            {
                Concept concept = Concept.find(context, Integer.parseInt(conceptId));
                Term newTerm = concept.createTerm(context, literalForm,Term.prefer_term);

                context.commit();
                // success
                result.setContinue(true);
                result.setOutcome(true);
                result.setMessage(T_add_Term_success_notice);
                result.setParameter("termID", newTerm.getID());
            }
            else
            {
                //create term without concept
                Term newTerm = Term.create(context);
                newTerm.setStatus(context, status);
                newTerm.setLang(context, language);
                newTerm.setSource(context, source);
                newTerm.setCreated(context, newTerm.getCreated());
                newTerm.setLastModified(context, newTerm.getCreated());
                context.commit();
                // success
                result.setContinue(true);
                result.setOutcome(true);
                result.setMessage(T_add_Term_success_notice);
                result.setParameter("termID", newTerm.getID());
            }
        }

        return result;
    }


    /**
     * Edit an Term's metadata, the email address, first name, and last name are all
     * required. The user's email address can be updated but it must remain unique, if
     * the email address already exists then the an error is produced.
     *
     * @param context The current DSpace context
     * @param request The HTTP request parameters
     * @param ObjectModel Cocoon's object model
     * @return A process result's object.
     */
    public static FlowResult processEditTerm(Context context,
                                             Request request, Map ObjectModel, int termID)
            throws SQLException, AuthorizeException
    {

        FlowResult result = new FlowResult();
        result.setContinue(false); // default to failure

        // Get all our request parameters
        String literalForm = request.getParameter("literalForm");
        String status = request.getParameter("status");
        String source  = request.getParameter("source");
        String identifier = request.getParameter("identifier");
        String language = request.getParameter("lang");
        //boolean certificate = (request.getParameter("certificate") != null) ? true : false;


        // If we have errors, the form needs to be resubmitted to fix those problems
        if (StringUtils.isEmpty(literalForm))
        {
            result.addError("literalForm");
        }


        // No errors, so we edit the Term with the data provided
        if (result.getErrors() == null)
        {
            // Grab the person in question
            Term termModified = Term.find(context, termID);
//
//            // Make sure the email address we are changing to is unique
            String originalLiteralForm = termModified.getLiteralForm();
            if (originalLiteralForm == null || !originalLiteralForm.equals(literalForm))
            {
                Term potentialDupicate = Term.findByLiteralForm(context, literalForm);

                if (potentialDupicate == null)
                {
                    termModified.setLiteralForm(context, literalForm);
                }
                else if (potentialDupicate.equals(termModified))
                {
                    // set a special field in error so that the transformer can display a pretty error.
                    result.addError("term_literalForm_key");
                    return result;
                }
            }
            String originalSource = termModified.getSource();
            if (originalSource == null || !originalSource.equals(source)) {
                termModified.setSource(context, source);
            }
            String originalStatus = termModified.getStatus();
            if (originalStatus == null || !originalStatus.equals(status)) {
                termModified.setStatus(context, status);
            }
            String originalLang = termModified.getLang();
            if (originalLang == null || !originalLang.equals(language)) {
                termModified.setLang(context, language);
            }

            context.commit();

            result.setContinue(true);
            result.setOutcome(true);
            // FIXME: rename this message
            result.setMessage(T_edit_Term_success_notice);
        }

        // Everything was fine
        return result;
    }




    /**
     * Delete the epeople specified by the epeopleIDs parameter. This assumes that the
     * deletion has been confirmed.
     *
     * @param context The current DSpace context
     * @param termIds The unique id of the Term being edited.
     * @return A process result's object.
     */
    public static FlowResult processDeleteTerm(Context context, String[] termIds) throws NumberFormatException, SQLException, AuthorizeException
    {
        FlowResult result = new FlowResult();

        List<String> unableList = new ArrayList<String>();
        for (String id : termIds)
        {
            Term termDeleted = Term.find(context, Integer.valueOf(id));
            try {
                termDeleted.delete(context);
            }
            catch (Exception epde)
            {
                int termDeletedId = termDeleted.getID();
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

    public static FlowResult processEditTermMetadata(Context context, String id,Request request){
        return FlowAuthorityMetadataValueUtils.processEditMetadata(context, Constants.TERM, id, request);
    }

    public static FlowResult doDeleteMetadataFromTerm(Context context, String id,Request request)throws SQLException, AuthorizeException, UIException, IOException {
        return FlowAuthorityMetadataValueUtils.doDeleteMetadata(context, Constants.TERM,id, request);
    }

    public static FlowResult doAddMetadataToTerm(Context context, int id, Request request) throws SQLException, AuthorizeException, UIException, IOException
    {
        return FlowAuthorityMetadataValueUtils.doAddMetadata(context, Constants.TERM,id, request);
    }
}
