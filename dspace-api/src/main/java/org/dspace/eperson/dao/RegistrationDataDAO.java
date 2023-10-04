/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.eperson.dao;

import java.sql.SQLException;
import java.util.Date;

import org.dspace.core.Context;
import org.dspace.core.GenericDAO;
import org.dspace.eperson.RegistrationData;
import org.dspace.eperson.RegistrationTypeEnum;

/**
 * Database Access Object interface class for the RegistrationData object.
 * The implementation of this class is responsible for all database calls for the RegistrationData object and is
 * autowired by spring
 * This class should only be accessed from a single service and should never be exposed outside of the API
 *
 * @author kevinvandevelde at atmire.com
 */
public interface RegistrationDataDAO extends GenericDAO<RegistrationData> {

    /**
     * Finds {@link RegistrationData} by email.
     *
     * @param context Context for the current request
     * @param email The email
     * @return
     * @throws SQLException
     */
    public RegistrationData findByEmail(Context context, String email) throws SQLException;

    /**
     * Finds {@link RegistrationData} by email and type.
     *
     * @param context Context for the current request
     * @param email The email
     * @param type The type of the {@link RegistrationData}
     * @return
     * @throws SQLException
     */
    public RegistrationData findBy(Context context, String email, RegistrationTypeEnum type) throws SQLException;

    /**
     * Finds {@link RegistrationData} by token.
     *
     * @param context the context
     * @param token The token related to the {@link RegistrationData}.
     * @return
     * @throws SQLException
     */
    public RegistrationData findByToken(Context context, String token) throws SQLException;

    /**
     * Deletes {@link RegistrationData} by token.
     *
     * @param context Context for the current request
     * @param token The token to delete registrations for
     * @throws SQLException
     */
    public void deleteByToken(Context context, String token) throws SQLException;

    /**
     * Deletes expired {@link RegistrationData}.
     *
     * @param context Context for the current request
     * @param date The date to delete expired registrations for
     * @throws SQLException
     */
    void deleteExpiredBy(Context context, Date date) throws SQLException;
}
