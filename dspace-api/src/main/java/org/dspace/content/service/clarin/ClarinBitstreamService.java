/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.service.clarin;

import java.io.IOException;
import java.sql.SQLException;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.core.Context;

/**
 * Service interface class for the Bitstream object created for Clarin-Dspace import.
 * Contains methods needed to import bitstream when dspace5 migrating to dspace7.
 * The implementation of this class is autowired by spring.
 *
 * @author Michaela Paurikova (michaela.paurikova at dataquest.sk)
 */
public interface ClarinBitstreamService {

    /**
     * Create new empty bitstream without file and with bitstream format "unknown".
     * Add bitstream to bundle if the bundle is entered.
     * @param context context
     * @param bundle  The bundle in which our bitstream should be added.
     * @return  the newly created bitstream
     * @throws SQLException       if database error
     * @throws AuthorizeException if authorization error
     */
    public Bitstream create(Context context, Bundle bundle) throws SQLException, AuthorizeException;

    /**
     * Validation between expected values and calculated values based on existing file.
     * The file must be stored in assetstore under internal_id. Internal_id must be specified in input bitstream.
     * Method finds data in assetstore and calculates the bitstream
     * check fields (checksum, sizeBytes, checksum algorithm).
     * These calculated values are compared with expected values from input bitstream.
     * The bitstream is stored into database only if the error was not occur:
     * calculated and expected check fields values match.
     * @param context context
     * @param bitstream bitstream
     * @return validation was successfully
     * @throws IOException If a problem occurs while storing the bits
     * @throws SQLException if database error
     * @throws AuthorizeException if authorization error
     */
    public boolean validation(Context context, Bitstream bitstream)
            throws IOException, SQLException, AuthorizeException ;
}
