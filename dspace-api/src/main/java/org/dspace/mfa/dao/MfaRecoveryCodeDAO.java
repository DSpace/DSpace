/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.mfa.dao;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import org.dspace.core.Context;
import org.dspace.core.GenericDAO;
import org.dspace.mfa.MfaRecoveryCode;

/**
 * Data Access Object interface for the {@link MfaRecoveryCode} entity.
 *
 * <p>Provides persistence operations for MFA recovery codes beyond the
 * standard CRUD operations inherited from {@link GenericDAO}.</p>
 *
 * @author DSpace Contributors
 * @see MfaRecoveryCode
 * @see org.dspace.mfa.dao.impl.MfaRecoveryCodeDAOImpl
 */
public interface MfaRecoveryCodeDAO extends GenericDAO<MfaRecoveryCode> {

    /**
     * Retrieves all unused recovery codes associated with a specific MFA configuration.
     *
     * @param context the DSpace context providing the database session
     * @param mfaUuid the UUID of the parent {@link org.dspace.mfa.Mfa} entity
     * @return a list of unused {@link MfaRecoveryCode} entities; empty list if none found
     * @throws SQLException if a database access error occurs
     */
    List<MfaRecoveryCode> findUnusedByMfa(Context context, UUID mfaUuid) throws SQLException;

    /**
     * Deletes all recovery codes (used and unused) for a given MFA configuration.
     *
     * <p>This is typically called before generating a new set of recovery codes or
     * when disabling MFA entirely.</p>
     *
     * @param context the DSpace context providing the database session
     * @param mfaUuid the UUID of the parent {@link org.dspace.mfa.Mfa} entity
     * @throws SQLException if a database access error occurs
     */
    void deleteByMfa(Context context, UUID mfaUuid) throws SQLException;
}
