/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.eperson.dao;

import org.dspace.core.Context;
import org.dspace.core.GenericDAO;
import org.dspace.eperson.RegistrationData;

import java.sql.SQLException;

/**
 * Database Access Object interface class for the RegistrationData object.
 * The implementation of this class is responsible for all database calls for the RegistrationData object and is autowired by spring
 * This class should only be accessed from a single service and should never be exposed outside of the API
 *
 * @author kevinvandevelde at atmire.com
 */
public interface RegistrationDataDAO extends GenericDAO<RegistrationData> {

    public RegistrationData findByEmail(Context context, String email) throws SQLException;

    public RegistrationData findByToken(Context context, String token) throws SQLException;

    public void deleteByToken(Context context, String token) throws SQLException;
}
