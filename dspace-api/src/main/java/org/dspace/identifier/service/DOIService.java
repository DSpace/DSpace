/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.identifier.service;

import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.identifier.DOI;
import org.dspace.identifier.IdentifierException;
import org.dspace.identifier.doi.DOIIdentifierException;

import java.sql.SQLException;
import java.util.List;

/**
 * Service interface class for the DOI object.
 * The implementation of this class is responsible for all business logic calls for the DOI object and is autowired by spring
 *
 * @author kevinvandevelde at atmire.com
 */
public interface DOIService {

    public void update(Context context, DOI doi) throws SQLException;

    public DOI create(Context context) throws SQLException;

    public DOI findByDoi(Context context, String doi) throws SQLException;

    public DOI findDOIByDSpaceObject(Context context, DSpaceObject dso) throws SQLException;

    public DOI findDOIByDSpaceObject(Context context, DSpaceObject dso, List<Integer> statusToExclude) throws SQLException;


    /**
     * This method helps to convert a DOI into a URL. It takes DOIs in one of
     * the following formats  and returns it as URL (f.e.
     * http://dx.doi.org/10.123/456). Allowed formats are:
     * <ul>
     *   <li>doi:10.123/456</li>
     *   <li>10.123/456</li>
     *   <li>http://dx.doi.org/10.123/456</li>
     * </ul>
     *
     * @param identifier  A DOI that should be returned in external form.
     * @return A String containing a URL to the official DOI resolver.
     * @throws IllegalArgumentException If identifier is null or an empty String.
     * @throws org.dspace.identifier.IdentifierException If identifier could not be recognized as valid DOI.
     */
    public String DOIToExternalForm(String identifier)
            throws IdentifierException;

    public String DOIFromExternalFormat(String identifier)
            throws DOIIdentifierException;

    /**
     * Recognize format of DOI and return it with leading doi-Scheme.
     * @param identifier Identifier to format, following format are accepted:
     *                   f.e. 10.123/456, doi:10.123/456, http://dx.doi.org/10.123/456.
     * @return Given Identifier with DOI-Scheme, f.e. doi:10.123/456.
     * @throws IllegalArgumentException If identifier is empty or null.
     * @throws org.dspace.identifier.doi.DOIIdentifierException If DOI could not be recognized.
     */
    public String formatIdentifier(String identifier)
            throws DOIIdentifierException;

    public List<DOI> getDOIsByStatus(Context context, List<Integer> statuses) throws SQLException;
    
    /**
     * Find all DOIs that are similar to the specified pattern ant not in the specified states.
     * @param context DSpace context
     * @param doiPattern The pattern, e.g. "10.5072/123.%"
     * @param statuses The statuses the DOI should <b>not</b> be in, @{link DOIIdentifierProvider.DELETED}.
     * @param dsoIsNotNull Boolean whether all DOIs should be excluded where the DSpaceObject is NULL.
     * @return null or a list of DOIs
     * @throws SQLException if database error
     */
    public List<DOI> getSimilarDOIsNotInState(Context context, String doiPattern, List<Integer> statuses, boolean dsoIsNotNull)
            throws SQLException;
}
