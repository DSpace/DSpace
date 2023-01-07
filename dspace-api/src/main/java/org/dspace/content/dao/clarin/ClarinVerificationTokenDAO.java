/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.dao.clarin;

import java.sql.SQLException;

import org.dspace.content.clarin.ClarinVerificationToken;
import org.dspace.core.Context;
import org.dspace.core.GenericDAO;

/**
 * Database Access Object interface class for the ClarinVerificationToken object.
 * The implementation of this class is responsible for all database calls for the ClarinVerificationToken object
 * and is autowired by spring This class should only be accessed from a single service and should never be exposed
 * outside the API
 *
 * @author Milan Majchrak (milan.majchrak at dataquest.sk)
 */
public interface ClarinVerificationTokenDAO extends GenericDAO<ClarinVerificationToken> {

    ClarinVerificationToken findByToken(Context context, String token) throws SQLException;
    ClarinVerificationToken findByNetID(Context context, String netID) throws SQLException;
}
