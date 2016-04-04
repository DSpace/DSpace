package org.dspace.app.xmlui.aspect.authority;

import org.apache.cocoon.environment.http.HttpEnvironment;
import org.apache.log4j.Logger;
import org.dspace.app.xmlui.utils.ContextUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.authority.*;
import org.dspace.core.Constants;
import org.dspace.core.Context;

import javax.servlet.http.HttpServletRequest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.Map;

import java.util.Date;
/**
 * User: lantian @ atmire . com
 * Date: 3/12/14
 * Time: 3:31 PM
 */
public class AuthorityUtils {

    /** log4j category */
    private static final Logger log = Logger.getLogger(AuthorityUtils.class);

    public static Concept createNewConcept(Map objectModel,Boolean topConcept,String status,String language,String identifier,String value) throws
            SQLException, AuthorizeException,NoSuchAlgorithmException
    {
        final HttpServletRequest request = (HttpServletRequest) objectModel.get(HttpEnvironment.HTTP_REQUEST_OBJECT);
        Context context = ContextUtil.obtainContext(objectModel);
        String schemeId = request.getParameter("scheme");
        // Need to create new concept
        if(schemeId!=null) {
            Scheme scheme = (Scheme) AuthorityObject.find(context, Constants.SCHEME, Integer.parseInt(schemeId));
            Concept concept = scheme.createConcept(context, value);
            context.commit();
            return concept;
        }
        else
        {
            return null;
        }
    }


    public static Term createNewTerm(Map objectModel,String literalForm,String status,String source,String language) throws
            SQLException, AuthorizeException
    {
        final HttpServletRequest request = (HttpServletRequest) objectModel.get(HttpEnvironment.HTTP_REQUEST_OBJECT);
        Context context = ContextUtil.obtainContext(objectModel);
        String conceptId = request.getParameter("concept");
        if(conceptId!=null){
            Concept concept = (Concept) AuthorityObject.find(context,Constants.CONCEPT,Integer.parseInt(conceptId));
            Term term = concept.createTerm(context, literalForm,Term.prefer_term);
            term.setStatus(context, status);
            term.setLang(context, language);
            term.setSource(context, source);
            context.commit();

            return term;
        }else
        {
            return null;
        }

    }


    public static Concept2Term createNewConcept2Term(Map objectModel,Integer role_id,Integer concept_id,Integer term_id) throws
            SQLException, AuthorizeException
    {
        final HttpServletRequest request = (HttpServletRequest) objectModel.get(HttpEnvironment.HTTP_REQUEST_OBJECT);
        Context context = ContextUtil.obtainContext(objectModel);

        // Need to create new concept2Term

        Concept2Term concept2Term = Concept2Term.create(context);
        Date date = new Date();
        concept2Term.setRoleId(role_id);
        concept2Term.setConceptId(concept_id);
        concept2Term.setTermId(term_id);
        context.commit();

        return concept2Term;
    }

    public static Scheme createNewScheme(Map objectModel,String status,String language) throws
            SQLException, AuthorizeException
    {
        final HttpServletRequest request = (HttpServletRequest) objectModel.get(HttpEnvironment.HTTP_REQUEST_OBJECT);
        Context context = ContextUtil.obtainContext(objectModel);

        // Need to create new concept

        Scheme scheme = Scheme.create(context);
        Date date = new Date();
        scheme.setLastModified(context, date);
        scheme.setCreated(context, date);
        scheme.setLang(context, language);
        //concept.setTopConcept(topConcept);
        scheme.setStatus(context, status);
        context.commit();
        // Give site auth a chance to set/override appropriate fields
        //AuthenticationManager.initEPerson(context, request, eperson);

        return scheme;
    }

}
