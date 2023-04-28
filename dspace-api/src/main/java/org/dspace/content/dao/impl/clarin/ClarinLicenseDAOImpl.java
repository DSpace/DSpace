/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.dao.impl.clarin;

import java.sql.SQLException;
import java.util.List;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.dspace.content.clarin.ClarinLicense;
import org.dspace.content.clarin.ClarinLicense_;
import org.dspace.content.dao.clarin.ClarinLicenseDAO;
import org.dspace.core.AbstractHibernateDAO;
import org.dspace.core.Context;

/**
 * Hibernate implementation of the Database Access Object interface class for the Clarin License object.
 * This class is responsible for all database calls for the Clarin License object and is autowired by spring
 * This class should never be accessed directly.
 *
 * @author Milan Majchrak (milan.majchrak at dataquest.sk)
 */
public class ClarinLicenseDAOImpl extends AbstractHibernateDAO<ClarinLicense> implements ClarinLicenseDAO {
    protected ClarinLicenseDAOImpl() {
        super();
    }

    @Override
    public ClarinLicense findByName(Context context, String name) throws SQLException {
        Query query = createQuery(context, "SELECT cl " +
                "FROM ClarinLicense cl " +
                "WHERE cl.name = :name");

        query.setParameter("name", name);
        query.setHint("org.hibernate.cacheable", Boolean.TRUE);

        return singleResult(query);
    }

    @Override
    public List<ClarinLicense> findByNameLike(Context context, String name) throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, ClarinLicense.class);
        Root<ClarinLicense> clarinLicenseRoot = criteriaQuery.from(ClarinLicense.class);
        criteriaQuery.select(clarinLicenseRoot);
        criteriaQuery.where(criteriaBuilder.like(clarinLicenseRoot.get(ClarinLicense_.name), "%" + name + "%"));
        criteriaQuery.orderBy(criteriaBuilder.asc(clarinLicenseRoot.get(ClarinLicense_.name)));
        return list(context, criteriaQuery, false, ClarinLicense.class, -1, -1);
    }
}
