/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.versioning.dao.impl;

import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Root;

import org.dspace.content.Item;
import org.dspace.core.AbstractHibernateDAO;
import org.dspace.core.Context;
import org.dspace.versioning.Version;
import org.dspace.versioning.VersionHistory;
import org.dspace.versioning.Version_;
import org.dspace.versioning.dao.VersionHistoryDAO;

/**
 * Hibernate implementation of the Database Access Object interface class for the VersionHistory object.
 * This class is responsible for all database calls for the VersionHistory object and is autowired by spring
 * This class should never be accessed directly.
 *
 * @author Fabio Bolognesi (fabio at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author kevinvandevelde at atmire.com
 */
public class VersionHistoryDAOImpl extends AbstractHibernateDAO<VersionHistory> implements VersionHistoryDAO {
    protected VersionHistoryDAOImpl() {
        super();
    }

    @Override
    public VersionHistory findByItem(Context context, Item item) throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, VersionHistory.class);
        Root<VersionHistory> versionHistoryRoot = criteriaQuery.from(VersionHistory.class);
        Join<VersionHistory, Version> join = versionHistoryRoot.join("versions");
        criteriaQuery.select(versionHistoryRoot);
        criteriaQuery.where(criteriaBuilder.equal(join.get(Version_.item), item));

        List<javax.persistence.criteria.Order> orderList = new LinkedList<>();
        orderList.add(criteriaBuilder.desc(join.get(Version_.versionNumber)));
        criteriaQuery.orderBy(orderList);

        return singleResult(context, criteriaQuery);
    }
}
