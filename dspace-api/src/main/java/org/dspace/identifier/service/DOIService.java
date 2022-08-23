/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.identifier.service;

import java.sql.SQLException;
import java.util.List;

import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.identifier.DOI;
import org.dspace.identifier.IdentifierException;
import org.dspace.identifier.doi.DOIIdentifierException;

/**
 * Service interface class for the {@link DOI} object.
 * The implementation of this class is responsible for all business logic calls
 * for the {@link DOI} object and is autowired by Spring.
 *
 * @author kevinvandevelde at atmire.com
 */
public interface DOIService {

    /**
     * Update a DOI in storage.
     *
     * @param context current DSpace session.
     * @param doi the DOI to persist.
     * @throws SQLException passed through.
     */
    public void update(Context context, DOI doi) throws SQLException;

    /**
     * Create a new DOI in storage.
     *
     * @param context current DSpace session.
     * @return the new DOI.
     * @throws SQLException passed through.
     */
    public DOI create(Context context) throws SQLException;

    /**
     * Find a specific DOI in storage.
     *
     * @param context current DSpace session.
     * @param doi string representation of the DOI.
     * @return the DOI object found.
     * @throws SQLException passed through, can mean none found.
     */
    public DOI findByDoi(Context context, String doi) throws SQLException;

    /**
     * Find the DOI assigned to a given DSpace Object.
     *
     * @param context current DSpace session.
     * @param dso The DSpace Object.
     * @return the DSO's DOI.
     * @throws SQLException passed through.
     */
    public DOI findDOIByDSpaceObject(Context context, DSpaceObject dso) throws SQLException;

    /**
     * Find the DOI assigned to a given DSpace Object, unless it has one of a
     * given set of statuses.
     *
     * @param context current DSpace context.
     * @param dso the DSpace Object.
     * @param statusToExclude uninteresting statuses.
     * @return the DSO's DOI.
     * @throws SQLException passed through.
     */
    public DOI findDOIByDSpaceObject(Context context, DSpaceObject dso, List<Integer> statusToExclude)
        throws SQLException;

    /**
     * This method helps to convert a DOI into a URL. It takes DOIs in one of
     * the following formats  and returns it as URL (f.e.
     * http://dx.doi.org/10.123/456). Allowed formats are:
     * <ul>
     * <li>doi:10.123/456</li>
     * <li>10.123/456</li>
     * <li>http://dx.doi.org/10.123/456</li>
     * </ul>
     *
     * @param identifier A DOI that should be returned in external form.
     * @return A String containing a URL to the official DOI resolver.
     * @throws IllegalArgumentException If identifier is null or an empty String.
     * @throws IdentifierException      If identifier could not be recognized as valid DOI.
     */
    public String DOIToExternalForm(String identifier)
        throws IdentifierException;

    /**
     * Convert an HTTP DOI URL (https://doi.org/10.something) to a "doi:" URI.
     * @param identifier HTTP URL
     * @return DOI URI
     * @throws DOIIdentifierException if {@link identifier} is not recognizable.
     */
    public String DOIFromExternalFormat(String identifier)
        throws DOIIdentifierException;

    /**
     * Recognize format of DOI and return it with leading doi-Scheme.
     *
     * @param identifier Identifier to format, following format are accepted:
     *                   f.e. 10.123/456, doi:10.123/456, http://dx.doi.org/10.123/456.
     * @return Given Identifier with DOI-Scheme, f.e. doi:10.123/456.
     * @throws IllegalArgumentException If identifier is empty or null.
     * @throws DOIIdentifierException   If DOI could not be recognized.
     */
    public String formatIdentifier(String identifier)
        throws DOIIdentifierException;

    /**
     * Find all DOIs that have one of a given set of statuses.
     * @param context current DSpace session.
     * @param statuses desired statuses.
     * @return all DOIs having any of the given statuses.
     * @throws SQLException passed through.
     */
    public List<DOI> getDOIsByStatus(Context context, List<Integer> statuses) throws SQLException;

    /**
     * Find all DOIs that are similar to the specified pattern and not in the
     * specified states.
     *
     * @param context      DSpace context
     * @param doiPattern   The pattern, e.g. "10.5072/123.%"
     * @param statuses     The statuses the DOI should <b>not</b> be in, @{link DOIIdentifierProvider.DELETED}.
     * @param dsoIsNotNull Boolean whether all DOIs should be excluded where the DSpaceObject is NULL.
     * @return null or a list of DOIs
     * @throws SQLException if database error
     */
    public List<DOI> getSimilarDOIsNotInState(Context context, String doiPattern, List<Integer> statuses,
                                              boolean dsoIsNotNull)
        throws SQLException;

    /**
     * Get the URL stem of the DOI resolver, e.g. "https://doi.org/".
     *
     * @return URL to the DOI resolver.
     */
    public String getResolver();
}
