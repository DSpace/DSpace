/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.mfa.dao;

import java.sql.SQLException;

import org.dspace.core.Context;
import org.dspace.core.GenericDAO;
import org.dspace.eperson.EPerson;
import org.dspace.mfa.Mfa;
import org.dspace.mfa.MfaType;

/**
 * Data Access Object interface for the {@link Mfa} entity.
 *
 * <p>Provides persistence operations specific to MFA configurations beyond
 * the standard CRUD operations inherited from {@link GenericDAO}.</p>
 *
 * @author DSpace Contributors
 * @see Mfa
 * @see org.dspace.mfa.dao.impl.MfaDAOImpl
 */
public interface MfaDAO extends GenericDAO<Mfa> {

    /**
     * Finds the MFA configuration for a given user and MFA type.
     *
     * @param context the DSpace context providing the database session
     * @param eperson the user whose MFA configuration to look up
     * @param type    the MFA mechanism type to search for
     * @return the matching {@link Mfa} entity, or {@code null} if none exists
     * @throws SQLException if a database access error occurs
     */
    Mfa findByEPersonAndType(Context context, EPerson eperson, MfaType type) throws SQLException;

    /**
     * Deletes all MFA configurations for a given user.
     *
     * <p>This is a bulk delete operation intended for use when disabling all MFA
     * mechanisms for a user. Associated recovery codes should be deleted separately
     * before calling this method to avoid foreign key violations.</p>
     *
     * @param context the DSpace context providing the database session
     * @param eperson the user whose MFA configurations should be removed
     * @throws SQLException if a database access error occurs
     */
    void deleteByEPerson(Context context, EPerson eperson) throws SQLException;
}
