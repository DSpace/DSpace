/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.mfa.dao.impl;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaDelete;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.dspace.core.AbstractHibernateDAO;
import org.dspace.core.Context;
import org.dspace.mfa.MfaRecoveryCode;
import org.dspace.mfa.dao.MfaRecoveryCodeDAO;

/**
 * Hibernate implementation of {@link MfaRecoveryCodeDAO}.
 *
 * <p>Uses JPA Criteria API for type-safe query construction against the
 * {@code mfa_recovery_code} table. Extends {@link AbstractHibernateDAO} to
 * leverage DSpace's standard Hibernate session management.</p>
 *
 * @author DSpace Contributors
 * @see MfaRecoveryCodeDAO
 */
public class MfaRecoveryCodeDAOImpl extends AbstractHibernateDAO<MfaRecoveryCode> implements MfaRecoveryCodeDAO {

    /**
     * Protected constructor for Spring bean instantiation.
     */
    protected MfaRecoveryCodeDAOImpl() {
        super();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Queries for recovery codes where {@code mfa.id} matches the given UUID
     * and {@code used} is {@code false}. Returns all matching records without pagination.</p>
     */
    @Override
    public List<MfaRecoveryCode> findUnusedByMfa(Context context, UUID mfaUuid) throws SQLException {
        CriteriaBuilder cb = getCriteriaBuilder(context);
        CriteriaQuery<MfaRecoveryCode> cq = getCriteriaQuery(cb, MfaRecoveryCode.class);
        Root<MfaRecoveryCode> root = cq.from(MfaRecoveryCode.class);
        cq.select(root);
        cq.where(
            cb.and(
                cb.equal(root.get("mfa").get("id"), mfaUuid),
                cb.equal(root.get("used"), false)
            )
        );
        return list(context, cq, false, MfaRecoveryCode.class, -1, -1);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Executes a bulk DELETE statement removing all {@link MfaRecoveryCode} records
     * associated with the given MFA UUID, regardless of their used/unused state.</p>
     */
    @Override
    public void deleteByMfa(Context context, UUID mfaUuid) throws SQLException {
        CriteriaBuilder cb = getCriteriaBuilder(context);
        CriteriaDelete<MfaRecoveryCode> cd = cb.createCriteriaDelete(MfaRecoveryCode.class);
        Root<MfaRecoveryCode> root = cd.from(MfaRecoveryCode.class);
        cd.where(cb.equal(root.get("mfa").get("id"), mfaUuid));
        getHibernateSession(context).createMutationQuery(cd).executeUpdate();
    }
}
