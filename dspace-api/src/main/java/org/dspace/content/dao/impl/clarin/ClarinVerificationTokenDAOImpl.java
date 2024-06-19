/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.dao.impl.clarin;

import java.sql.SQLException;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.dspace.content.clarin.ClarinVerificationToken;
import org.dspace.content.clarin.ClarinVerificationToken_;
import org.dspace.content.dao.clarin.ClarinVerificationTokenDAO;
import org.dspace.core.AbstractHibernateDAO;
import org.dspace.core.Context;

/**
 * Hibernate implementation of the Database Access Object interface class for the ClarinVerificationToken object.
 * This class is responsible for all database calls for the ClarinVerificationToken object and is autowired by spring
 * This class should never be accessed directly.
 *
 * @author Milan Majchrak (milan.majchrak at dataquest.sk)
 */
public class ClarinVerificationTokenDAOImpl extends AbstractHibernateDAO<ClarinVerificationToken>
        implements ClarinVerificationTokenDAO {

    @Override
    public ClarinVerificationToken findByToken(Context context, String token) throws SQLException {
        Query query = createQuery(context, "SELECT cvt " +
                "FROM ClarinVerificationToken cvt " +
                "WHERE cvt.token = :token");

        query.setParameter("token", token);
        query.setHint("org.hibernate.cacheable", Boolean.TRUE);

        return singleResult(query);
    }

    @Override
    public ClarinVerificationToken findByNetID(Context context, String netID) throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, ClarinVerificationToken.class);
        Root<ClarinVerificationToken> clarinVerificationTokenRoot = criteriaQuery.from(ClarinVerificationToken.class);
        criteriaQuery.select(clarinVerificationTokenRoot);
        criteriaQuery.where(criteriaBuilder.like(clarinVerificationTokenRoot.get(ClarinVerificationToken_.ePersonNetID),
                "%" + netID + "%"));
        criteriaQuery.orderBy(criteriaBuilder.asc(clarinVerificationTokenRoot.
                get(ClarinVerificationToken_.ePersonNetID)));
        return singleResult(context, criteriaQuery);
    }
}
