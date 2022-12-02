/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.service.clarin;

import java.sql.SQLException;
import java.util.List;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.clarin.ClarinLicenseLabel;
import org.dspace.core.Context;

/**
 * Service interface class for the Clarin License Label object.
 * The implementation of this class is responsible for all business logic calls for the Clarin License Label object
 * and is autowired by spring
 *
 * @author Milan Majchrak (milan.majchrak at dataquest.sk)
 */
public interface ClarinLicenseLabelService {

    /**
     * Create a new clarin license label. Authorization is done inside this method.
     * @param context DSpace context object
     * @return the newly created clarin license label
     * @throws SQLException if database error
     * @throws AuthorizeException the user in not admin
     */
    ClarinLicenseLabel create(Context context) throws SQLException, AuthorizeException;

    /**
     * Create a new clarin license label. Authorization is done inside this method.
     * @param context DSpace context object
     * @param clarinLicenseLabel new clarin license label object data
     * @return the newly created clarin license label
     * @throws SQLException if database error
     * @throws AuthorizeException the user in not admin
     */
    ClarinLicenseLabel create(Context context, ClarinLicenseLabel clarinLicenseLabel) throws SQLException,
            AuthorizeException;

    /**
     * Find the clarin license label object by id
     * @param context DSpace context object
     * @param valueId id of the searching clarin license label object
     * @return found clarin license label object or null
     * @throws SQLException if database error
     */
    ClarinLicenseLabel find(Context context, int valueId) throws SQLException;

    /**
     * Find all clarin license label objects
     * @param context DSpace context object
     * @return list of all clarin license label objects
     * @throws SQLException if database error
     * @throws AuthorizeException the user in not admin
     */
    List<ClarinLicenseLabel> findAll(Context context) throws SQLException, AuthorizeException;

    /**
     * Delete the clarin license label by id. The id is retrieved from passed clarin license label object.
     * @param context DSpace context object
     * @param clarinLicenseLabel object to delete
     * @throws SQLException if database error
     * @throws AuthorizeException the user in not admin
     */
    void delete(Context context, ClarinLicenseLabel clarinLicenseLabel) throws SQLException, AuthorizeException;

    /**
     * Update the clarin license label object by id. The id is retrieved from passed clarin license label object.
     * @param context DSpace context object
     * @param newClarinLicenseLabel with new clarin license label object values
     * @throws SQLException if database error
     * @throws AuthorizeException the user is not admin
     */
    void update(Context context, ClarinLicenseLabel newClarinLicenseLabel) throws SQLException, AuthorizeException;
}
