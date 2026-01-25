/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.eperson.dao.impl;

import java.sql.SQLException;
import java.time.Instant;

import jakarta.persistence.Query;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaDelete;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.dspace.core.AbstractHibernateDAO;
import org.dspace.core.Context;
import org.dspace.eperson.RegistrationData;
import org.dspace.eperson.RegistrationData_;
import org.dspace.eperson.RegistrationTypeEnum;
import org.dspace.eperson.dao.RegistrationDataDAO;

/**
 * Hibernate implementation of the Database Access Object interface class for the RegistrationData object.
 * This class is responsible for all database calls for the RegistrationData object and is autowired by Spring.
 * This class should never be accessed directly.
 *
 * @author kevinvandevelde at atmire.com
 */
public class RegistrationDataDAOImpl extends AbstractHibernateDAO<RegistrationData> implements RegistrationDataDAO {

    protected RegistrationDataDAOImpl() {
        super();
    }

    @Override
    public RegistrationData findByEmail(Context context, String email) throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, RegistrationData.class);
        Root<RegistrationData> registrationDataRoot = criteriaQuery.from(RegistrationData.class);
        criteriaQuery.select(registrationDataRoot);
        criteriaQuery.where(criteriaBuilder.equal(registrationDataRoot.get(RegistrationData_.email), email));
        return uniqueResult(context, criteriaQuery, false, RegistrationData.class);
    }

    @Override
    public RegistrationData findBy(Context context, String email, RegistrationTypeEnum type) throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, RegistrationData.class);
        Root<RegistrationData> registrationDataRoot = criteriaQuery.from(RegistrationData.class);
        criteriaQuery.select(registrationDataRoot);
        criteriaQuery.where(
            criteriaBuilder.and(
                criteriaBuilder.equal(registrationDataRoot.get(RegistrationData_.email), email),
                criteriaBuilder.equal(registrationDataRoot.get(RegistrationData_.registrationType), type)
            )
        );
        return uniqueResult(context, criteriaQuery, false, RegistrationData.class);
    }

    @Override
    public RegistrationData findByToken(Context context, String token) throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, RegistrationData.class);
        Root<RegistrationData> registrationDataRoot = criteriaQuery.from(RegistrationData.class);
        criteriaQuery.select(registrationDataRoot);
        criteriaQuery.where(criteriaBuilder.equal(registrationDataRoot.get(RegistrationData_.token), token));
        return uniqueResult(context, criteriaQuery, false, RegistrationData.class);
    }

    @Override
    public void deleteByToken(Context context, String token) throws SQLException {
        // For Hibernate 7 compatibility: Use entity-based deletion instead of bulk delete.
        // Bulk HQL deletes don't update the persistence context, causing TransientPropertyValueException.
        RegistrationData registrationData = findByToken(context, token);
        if (registrationData != null) {
            delete(context, registrationData);
        }
    }

    @Override
    public void deleteExpiredBy(Context context, Instant instant) throws SQLException {
        // For Hibernate 7 compatibility: Use entity-based deletion instead of bulk delete.
        // Bulk HQL deletes don't update the persistence context, causing TransientPropertyValueException.
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, RegistrationData.class);
        Root<RegistrationData> registrationDataRoot = criteriaQuery.from(RegistrationData.class);
        criteriaQuery.select(registrationDataRoot);
        criteriaQuery.where(
            criteriaBuilder.lessThanOrEqualTo(registrationDataRoot.get(RegistrationData_.expires), instant)
        );
        java.util.List<RegistrationData> expiredRecords = list(context, criteriaQuery, false, RegistrationData.class,
            -1, -1);
        for (RegistrationData record : expiredRecords) {
            delete(context, record);
        }
    }
}
