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
import org.dspace.content.clarin.ClarinVerificationToken;
import org.dspace.core.Context;

/**
 * Service interface class for the ClarinVerificationToken object.
 * The implementation of this class is responsible for all business logic calls for the ClarinVerificationToken object
 * and is autowired by spring
 *
 * @author Milan Majchrak (milan.majchrak at dataquest.sk)
 */
public interface ClarinVerificationTokenService {

    /**
     * Create a new clarin verification token.
     *
     * @param context @param context DSpace context object
     * @return the newly created clarin verification token.
     * @throws SQLException       if database error
     */
    ClarinVerificationToken create(Context context) throws SQLException;

    /**
     * Find the clarin verification token object by the id
     *
     * @param context DSpace context object
     * @param valueId id of the searching clarin license object
     * @return found clarin verification token object or null
     * @throws SQLException if database error
     */
    ClarinVerificationToken find(Context context, int valueId) throws SQLException;

    /**
     * Find all clarin verification token objects
     * @param context DSpace context object
     * @return List of the clarin verification token objects or null
     * @throws SQLException if database error
     * @throws AuthorizeException if the user is not the admin
     */
    List<ClarinVerificationToken> findAll(Context context) throws SQLException, AuthorizeException;

    /**
     * Find the clarin verification token object by the token
     * @param context DSpace context object
     * @param token of the searching clarin license object
     * @return found clarin verification token object or null
     * @throws SQLException if database error
     */
    ClarinVerificationToken findByToken(Context context, String token) throws SQLException;

    /**
     * Find the clarin verification token object by the token
     * @param context DSpace context object
     * @param netID of the searching clarin license object
     * @return found clarin verification token object or null
     * @throws SQLException if database error
     */
    ClarinVerificationToken findByNetID(Context context, String netID) throws SQLException;

    /**
     * Remove the clarin verification token from DB
     * @param context DSpace context object
     * @param clarinUserRegistration object to delete
     * @throws SQLException if database error
     */
    void delete(Context context, ClarinVerificationToken clarinUserRegistration)
            throws SQLException;

    /**
     * Update the clarin verification token object. The object is found by id then updated
     * @param context DSpace context object
     * @param newClarinVerificationToken object with fresh data to update
     * @throws SQLException if database error
     */
    void update(Context context, ClarinVerificationToken newClarinVerificationToken) throws SQLException;
}
