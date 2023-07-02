/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.eperson.dao.impl;

import java.sql.SQLException;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.dspace.core.AbstractHibernateDAO;
import org.dspace.eperson.RegistrationData;
import org.dspace.eperson.RegistrationData_;
import org.dspace.eperson.dao.RegistrationDataDAO;
import org.hibernate.Session;

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
    public RegistrationData findByEmail(Session session, String email) throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(session);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, RegistrationData.class);
        Root<RegistrationData> registrationDataRoot = criteriaQuery.from(RegistrationData.class);
        criteriaQuery.select(registrationDataRoot);
        criteriaQuery.where(criteriaBuilder.equal(registrationDataRoot.get(RegistrationData_.email), email));
        return uniqueResult(session, criteriaQuery, false, RegistrationData.class);
    }

    @Override
    public RegistrationData findByToken(Session session, String token) throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(session);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, RegistrationData.class);
        Root<RegistrationData> registrationDataRoot = criteriaQuery.from(RegistrationData.class);
        criteriaQuery.select(registrationDataRoot);
        criteriaQuery.where(criteriaBuilder.equal(registrationDataRoot.get(RegistrationData_.token), token));
        return uniqueResult(session, criteriaQuery, false, RegistrationData.class);
    }

    @Override
    public void deleteByToken(Session session, String token) throws SQLException {
        String hql = "delete from RegistrationData where token=:token";
        Query query = createQuery(session, hql);
        query.setParameter("token", token);
        query.executeUpdate();
    }
}
