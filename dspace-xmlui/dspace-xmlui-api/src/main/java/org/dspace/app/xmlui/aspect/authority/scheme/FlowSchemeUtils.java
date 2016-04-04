/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.authority.scheme;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.cocoon.environment.Request;
import org.apache.log4j.Logger;
import org.dspace.app.xmlui.aspect.administrative.FlowResult;
import org.dspace.app.xmlui.aspect.authority.AuthorityUtils;
import org.dspace.app.xmlui.aspect.authority.FlowAuthorityMetadataValueUtils;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.authority.Concept;
import org.dspace.content.authority.Scheme;
import org.dspace.content.authority.Scheme2Concept;
import org.dspace.core.Constants;
import org.dspace.core.Context;


/**
 * Utility methods to processes actions on EPeople. These methods are used
 * exclusively from the administrative flow scripts.
 *
 * @author Scott Phillips
 */
public class FlowSchemeUtils {

    /** log4j category */
    private static final Logger log = Logger.getLogger(FlowSchemeUtils.class);

    /** Language Strings */
    private static final Message T_add_Scheme_success_notice =
            new Message("default","xmlui.administrative.FlowSchemeUtils.add_Scheme_success_notice");

    private static final Message T_edit_Scheme_success_notice =
            new Message("default","xmlui.administrative.FlowSchemeUtils.edit_Scheme_success_notice");

    private static final Message T_reset_password_success_notice =
            new Message("default","xmlui.administrative.FlowSchemeUtils.reset_password_success_notice");

    private static final Message t_delete_Scheme_success_notice =
            new Message("default","xmlui.administrative.FlowSchemeUtils.delete_Scheme_success_notice");

    private static final Message t_delete_Scheme_failed_notice =
            new Message("default","xmlui.administrative.FlowSchemeUtils.delete_Scheme_failed_notice");
    private static final Message t_delete_Term_success_notice =
            new Message("default","xmlui.administrative.FlowTermUtils.delete_Term_success_notice");

    private static final Message t_delete_Term_failed_notice =
            new Message("default","xmlui.administrative.FlowTermUtils.delete_Term_failed_notice");
    private static final Message t_delete_identifier_exists_notice =
            new Message("default","xmlui.administrative.FlowTermUtils.identifier_exists_notice");
    /**
     * Add a new Scheme. This method will not set metadata on scheme
     *
     * @param context The current DSpace context
     * @param request The HTTP request parameters
     * @param objectModel Cocoon's object model
     * @return A process result's object.
     */
    public static FlowResult processAddScheme(Context context, Request request, Map objectModel) throws SQLException, AuthorizeException
    {
        FlowResult result = new FlowResult();
        result.setContinue(false); // default to no continue

        String language = request.getParameter("language");
        String status = request.getParameter("status");

        if (result.getErrors() == null)
        {
            Scheme newScheme = AuthorityUtils.createNewScheme(objectModel, status, language);
            context.commit();
            // success
            result.setContinue(true);
            result.setOutcome(true);
            result.setMessage(T_add_Scheme_success_notice);
            result.setParameter("SchemeID", newScheme.getID());
        }

        return result;
    }


    /**
     * Edit an Scheme's metadata and concept
     * @return A process result's object.
     */
    public static FlowResult processEditScheme(Context context,
                                               Request request, Map ObjectModel, int schemeID)
            throws SQLException, AuthorizeException
    {

        FlowResult result = new FlowResult();
        result.setContinue(false); // default to failure

        // Get all our request parameters

        String status = request.getParameter("status");
        String identifier = request.getParameter("identifier");
        String language = request.getParameter("lang");
        //boolean certificate = (request.getParameter("certificate") != null) ? true : false;

        // No errors, so we edit the Scheme with the data provided
        if (result.getErrors() == null)
        {
            // Grab the person in question
            Scheme schemeModified = Scheme.find(context, schemeID);
            String originalStatus = schemeModified.getStatus();
            if (originalStatus == null || !originalStatus.equals(status)) {
                schemeModified.setStatus(context, status);
            }
            String originalLang = schemeModified.getLang();
            if (originalLang == null || !originalLang.equals(language)) {
                schemeModified.setLang(context, language);
            }

            context.commit();

            result.setContinue(true);
            result.setOutcome(true);
            // FIXME: rename this message
            result.setMessage(T_edit_Scheme_success_notice);
        }

        // Everything was fine
        return result;
    }





    /**
     * Detele scheme
     * todo:need delete concept ?
     */
    public static FlowResult processDeleteScheme(Context context, String[] schemeIds) throws NumberFormatException, SQLException, AuthorizeException
    {
        FlowResult result = new FlowResult();

        List<String> unableList = new ArrayList<String>();
        for (String id : schemeIds)
        {
            Scheme schemeDeleted = Scheme.find(context, Integer.valueOf(id));
            try {
                schemeDeleted.delete(context);
            }
            catch (Exception epde)
            {
                int schemeDeletedId = schemeDeleted.getID();
                unableList.add(Integer.toString(schemeDeletedId));
            }
        }

        if (unableList.size() > 0)
        {
            result.setOutcome(false);
            result.setMessage(t_delete_Scheme_failed_notice);

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
            result.setMessage(t_delete_Scheme_success_notice);
        }


        return result;
    }
    /*
    remove the concept from scheme
    */
    public static FlowResult doDeleteConceptFromScheme(Context context,String schemeIds, String[] conceptIds) throws NumberFormatException, SQLException, AuthorizeException
    {
        FlowResult result = new FlowResult();
        Scheme schemeDeleted = Scheme.find(context, Integer.valueOf(schemeIds));
        List<String> unableList = new ArrayList<String>();
        for (String id : conceptIds)
        {
            Concept concept = Concept.find(context, Integer.valueOf(id));
            try {
                schemeDeleted.removeConcept(context, concept);
            }
            catch (Exception epde)
            {
                int schemeDeletedId = schemeDeleted.getID();
                unableList.add(Integer.toString(schemeDeletedId));
            }
        }

        if (unableList.size() > 0)
        {
            result.setOutcome(false);
            result.setMessage(t_delete_Scheme_failed_notice);

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
            context.commit();
            result.setOutcome(true);
            result.setMessage(t_delete_Scheme_success_notice);
        }


        return result;
    }

    /*
    remove the concept from scheme
    */
    public static void addConcept2Scheme(Context context,String schemeId, String conceptId) throws NumberFormatException, SQLException, AuthorizeException
    {
        Scheme scheme = Scheme.find(context, Integer.parseInt(schemeId));
        Concept concept = Concept.find(context, Integer.parseInt(conceptId));
        scheme.addConcept(context, concept);
        context.commit();
    }


    public static FlowResult processEditSchemeMetadata(Context context, String schemeId,Request request){
        return FlowAuthorityMetadataValueUtils.processEditMetadata(context, Constants.SCHEME, schemeId, request);
    }

    public static FlowResult doDeleteMetadataFromScheme(Context context, String schemeId,Request request)throws SQLException, AuthorizeException, UIException, IOException {
        return FlowAuthorityMetadataValueUtils.doDeleteMetadata(context, Constants.SCHEME,schemeId, request);
    }

    public static FlowResult doAddMetadataToScheme(Context context, int schemeId, Request request) throws SQLException, AuthorizeException, UIException, IOException
    {
        return FlowAuthorityMetadataValueUtils.doAddMetadata(context, Constants.SCHEME,schemeId, request);
    }


    public static FlowResult processDeleteConcept(Context context,String schemeid, String[] conceptIds) throws NumberFormatException, SQLException, AuthorizeException
    {
        FlowResult result = new FlowResult();
        List<String> unableList = new ArrayList<String>();
        for (String id : conceptIds)
        {
            Scheme2Concept relationDeleted = Scheme2Concept.findBySchemeAndConcept(context, Integer.parseInt(schemeid), Integer.valueOf(id));
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
}
