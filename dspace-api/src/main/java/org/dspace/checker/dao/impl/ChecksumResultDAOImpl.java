/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.checker.dao.impl;

import java.sql.SQLException;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.dspace.checker.ChecksumResult;
import org.dspace.checker.ChecksumResultCode;
import org.dspace.checker.ChecksumResult_;
import org.dspace.checker.dao.ChecksumResultDAO;
import org.dspace.core.AbstractHibernateDAO;
import org.dspace.core.Context;

/**
 * Hibernate implementation of the Database Access Object interface class for the ChecksumResult object.
 * This class is responsible for all database calls for the ChecksumResult object and is autowired by spring
 * This class should never be accessed directly.
 *
 * @author kevinvandevelde at atmire.com
 */
public class ChecksumResultDAOImpl extends AbstractHibernateDAO<ChecksumResult> implements ChecksumResultDAO {

    protected ChecksumResultDAOImpl() {
        super();
    }

    @Override
    public ChecksumResult findByCode(Context context, ChecksumResultCode code) throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, ChecksumResult.class);
        Root<ChecksumResult> checksumResultRoot = criteriaQuery.from(ChecksumResult.class);
        criteriaQuery.select(checksumResultRoot);
        criteriaQuery.where(criteriaBuilder.equal(checksumResultRoot.get(ChecksumResult_.resultCode), code));
        return uniqueResult(context, criteriaQuery, false, ChecksumResult.class, -1, -1);
    }
}
