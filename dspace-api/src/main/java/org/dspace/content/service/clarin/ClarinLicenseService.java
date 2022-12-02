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
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.content.clarin.ClarinLicense;
import org.dspace.core.Context;

/**
 * Service interface class for the Clarin License object.
 * The implementation of this class is responsible for all business logic calls for the Clarin License object
 * and is autowired by spring
 *
 * @author Milan Majchrak (milan.majchrak at dataquest.sk)
 */
public interface ClarinLicenseService {

    /**
     * Create a new clarin license. Authorization is done inside this method.
     *
     * @param context @param context DSpace context object
     * @return the newly created clarin license
     * @throws SQLException       if database error
     * @throws AuthorizeException the user in not admin
     */
    ClarinLicense create(Context context) throws SQLException, AuthorizeException;

    /**
     * Create a new clarin license. Authorization is done inside this method.
     *
     * @param context       DSpace context object
     * @param clarinLicense new clarin license object data
     * @return the newly created clarin license
     * @throws SQLException       if database error
     * @throws AuthorizeException the user in not admin
     */
    ClarinLicense create(Context context, ClarinLicense clarinLicense) throws SQLException, AuthorizeException;

    /**
     * Find the clarin license object by id
     *
     * @param context DSpace context object
     * @param valueId id of the searching clarin license object
     * @return found clarin license object or null
     * @throws SQLException if database error
     */
    ClarinLicense find(Context context, int valueId) throws SQLException;

    ClarinLicense findByName(Context context, String name) throws SQLException;

    void addLicenseMetadataToItem(Context context, ClarinLicense clarinLicense, Item item) throws SQLException;

    void clearLicenseMetadataFromItem(Context context, Item item) throws SQLException;

    void addClarinLicenseToBitstream(Context context, Item item, Bundle bundle, Bitstream bitstream);

    /**
     * Find all clarin license objects
     *
     * @param context DSpace context object
     * @return list of all clarin license objects
     * @throws SQLException       if database error
     * @throws AuthorizeException the user in not admin
     */
    List<ClarinLicense> findAll(Context context) throws SQLException, AuthorizeException;

    /**
     * Delete the clarin license by id. The id is retrieved from the passed clarin license object.
     *
     * @param context       DSpace context object
     * @param clarinLicense object to delete
     * @throws SQLException       if database error
     * @throws AuthorizeException the user in not admin
     */
    void delete(Context context, ClarinLicense clarinLicense) throws SQLException, AuthorizeException;

    /**
     * Update the clarin license object by id. The id is retrieved from passed clarin license object.
     *
     * @param context          DSpace context object
     * @param newClarinLicense with new clarin license object values
     * @throws SQLException       if database error
     * @throws AuthorizeException the user is not admin
     */
    void update(Context context, ClarinLicense newClarinLicense) throws SQLException, AuthorizeException;
}
