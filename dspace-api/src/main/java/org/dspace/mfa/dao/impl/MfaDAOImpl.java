/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.mfa.dao.impl;

import java.sql.SQLException;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaDelete;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.dspace.core.AbstractHibernateDAO;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.mfa.Mfa;
import org.dspace.mfa.MfaType;
import org.dspace.mfa.dao.MfaDAO;

/**
 * Hibernate implementation of {@link MfaDAO}.
 *
 * <p>Uses JPA Criteria API for type-safe query construction against the
 * {@code mfa} table. Extends {@link AbstractHibernateDAO} to leverage
 * DSpace's standard Hibernate session management.</p>
 *
 * @author DSpace Contributors
 * @see MfaDAO
 */
public class MfaDAOImpl extends AbstractHibernateDAO<Mfa> implements MfaDAO {

    /**
     * Protected constructor for Spring bean instantiation.
     */
    protected MfaDAOImpl() {
        super();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Queries the {@code mfa} table for a record matching both the given EPerson
     * and MFA type. Returns at most one result since the combination is expected
     * to be unique.</p>
     */
    @Override
    public Mfa findByEPersonAndType(Context context, EPerson eperson, MfaType type) throws SQLException {
        CriteriaBuilder cb = getCriteriaBuilder(context);
        CriteriaQuery<Mfa> cq = getCriteriaQuery(cb, Mfa.class);
        Root<Mfa> root = cq.from(Mfa.class);
        cq.select(root);
        cq.where(
            cb.and(
                cb.equal(root.get("eperson"), eperson),
                cb.equal(root.get("mfaType"), type)
            )
        );
        return uniqueResult(context, cq, false, Mfa.class);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Executes a bulk DELETE statement removing all {@link Mfa} records
     * associated with the given EPerson. Uses a criteria delete for efficiency.</p>
     */
    @Override
    public void deleteByEPerson(Context context, EPerson eperson) throws SQLException {
        CriteriaBuilder cb = getCriteriaBuilder(context);
        CriteriaDelete<Mfa> cd = cb.createCriteriaDelete(Mfa.class);
        Root<Mfa> root = cd.from(Mfa.class);
        cd.where(cb.equal(root.get("eperson"), eperson));
        getHibernateSession(context).createMutationQuery(cd).executeUpdate();
    }
}
