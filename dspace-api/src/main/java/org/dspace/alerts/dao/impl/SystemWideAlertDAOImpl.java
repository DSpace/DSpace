/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.alerts.dao.impl;

import java.sql.SQLException;
import java.util.List;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.dspace.alerts.SystemWideAlert;
import org.dspace.alerts.SystemWideAlert_;
import org.dspace.alerts.dao.SystemWideAlertDAO;
import org.dspace.core.AbstractHibernateDAO;
import org.dspace.core.Context;

/**
 * Implementation class for the {@link SystemWideAlertDAO}
 */
public class SystemWideAlertDAOImpl extends AbstractHibernateDAO<SystemWideAlert> implements SystemWideAlertDAO {

    public List<SystemWideAlert> findAll(final Context context, final int limit, final int offset) throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, SystemWideAlert.class);
        Root<SystemWideAlert> alertRoot = criteriaQuery.from(SystemWideAlert.class);
        criteriaQuery.select(alertRoot);

        return list(context, criteriaQuery, false, SystemWideAlert.class, limit, offset);
    }

    public List<SystemWideAlert> findAllActive(final Context context, final int limit, final int offset)
            throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, SystemWideAlert.class);
        Root<SystemWideAlert> alertRoot = criteriaQuery.from(SystemWideAlert.class);
        criteriaQuery.select(alertRoot);
        criteriaQuery.where(criteriaBuilder.equal(alertRoot.get(SystemWideAlert_.active), true));

        return list(context, criteriaQuery, false, SystemWideAlert.class, limit, offset);
    }


}
