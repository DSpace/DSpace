/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.identifier;

import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.identifier.dao.DOIDAO;
import org.dspace.identifier.doi.DOIIdentifierException;
import org.dspace.identifier.service.DOIService;
import org.springframework.beans.factory.annotation.Autowired;

import java.sql.SQLException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service implementation for the DOI object.
 * This class is responsible for all business logic calls for the DOI object and is autowired by spring.
 * This class should never be accessed directly.
 *
 * @author kevinvandevelde at atmire.com
 */
public class DOIServiceImpl implements DOIService {

    @Autowired(required = true)
    protected DOIDAO doiDAO;

    protected DOIServiceImpl()
    {

    }

    @Override
    public void update(Context context, DOI doi) throws SQLException {
        doiDAO.save(context, doi);
    }

    @Override
    public DOI create(Context context) throws SQLException {
        return doiDAO.create(context, new DOI());
    }

    @Override
    public DOI findByDoi(Context context, String doi) throws SQLException {
        return doiDAO.findByDoi(context, doi);
    }

    @Override
    public DOI findDOIByDSpaceObject(Context context, DSpaceObject dso) throws SQLException {
        return doiDAO.findDOIByDSpaceObject(context, dso);
    }

    @Override
    public DOI findDOIByDSpaceObject(Context context, DSpaceObject dso, List<Integer> statusToExclude) throws SQLException {
        return doiDAO.findDOIByDSpaceObject(context, dso, statusToExclude);
    }

    @Override
    public String DOIToExternalForm(String identifier) throws IdentifierException {
        if (null == identifier) 
            throw new IllegalArgumentException("Identifier is null.", new NullPointerException());
        if (identifier.isEmpty())
            throw new IllegalArgumentException("Cannot format an empty identifier.");
        if (identifier.startsWith(DOI.SCHEME))
            return DOI.RESOLVER + "/" + identifier.substring(DOI.SCHEME.length());
        if (identifier.startsWith("10.") && identifier.contains("/"))
            return DOI.RESOLVER + "/" + identifier;
        if (identifier.startsWith(DOI.RESOLVER + "/10."))
            return identifier;
        
        throw new IdentifierException(identifier + "does not seem to be a DOI.");
    }

    @Override
    public String DOIFromExternalFormat(String identifier) throws DOIIdentifierException {
        Pattern pattern = Pattern.compile("^" + DOI.RESOLVER + "/+(10\\..*)$");
        Matcher matcher = pattern.matcher(identifier);
        if (matcher.find())
        {
            return DOI.SCHEME + matcher.group(1);
        }

        throw new DOIIdentifierException("Cannot recognize DOI!",
                DOIIdentifierException.UNRECOGNIZED);
    }

    @Override
    public String formatIdentifier(String identifier) throws DOIIdentifierException {
        if (null == identifier) {
            throw new IllegalArgumentException("Identifier is null.", new NullPointerException());
        }
        if (identifier.startsWith(DOI.SCHEME)) {
            return identifier;
        }
        if (identifier.isEmpty()) {
            throw new IllegalArgumentException("Cannot format an empty identifier.");
        }
        if (identifier.startsWith("10.") && identifier.contains("/")) {
            return DOI.SCHEME + identifier;
        }
        if (identifier.startsWith(DOI.RESOLVER + "/10.")) {
            return DOI.SCHEME + identifier.substring(18);
        }
        throw new DOIIdentifierException(identifier + "does not seem to be a DOI.",
                DOIIdentifierException.UNRECOGNIZED);
    }

    @Override
    public List<DOI> getDOIsByStatus(Context context, List<Integer> statuses) throws SQLException{
        return doiDAO.findByStatus(context, statuses);
    }
    
    @Override
    public List<DOI> getSimilarDOIsNotInState(Context context, String doiPattern, List<Integer> statuses, boolean dsoIsNotNull)
            throws SQLException
    {
        return doiDAO.findSimilarNotInState(context, doiPattern, statuses, dsoIsNotNull);
    }
}
