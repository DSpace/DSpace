/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.dao.impl;

import java.sql.SQLException;
import java.util.List;
import javax.persistence.Query;

import org.apache.commons.lang3.StringUtils;
import org.dspace.content.Collection;
import org.dspace.content.EtdUnit;
import org.dspace.content.dao.EtdUnitDAO;
import org.dspace.core.AbstractHibernateDSODAO;
import org.dspace.core.Context;

/**
 * Hibernate implementation of the Database Access Object interface class for
 * the EtdUnit object.
 * This class is responsible for all database calls for the EtdUnit object and
 * is autowired by spring
 * This class should never be accessed directly.
 *
 * @author kevinvandevelde at atmire.com
 */
public class EtdUnitDAOImpl extends AbstractHibernateDSODAO<EtdUnit> implements EtdUnitDAO {
    protected EtdUnitDAOImpl() {
        super();
    }

    @Override
    public List<EtdUnit> findByCollection(Context context, Collection collection) throws SQLException {
        Query query = createQuery(context,
            "from EtdUnit where (from Collection c where c.uuid = :collection_uuid) in elements(collection)");
        query.setParameter("collection_uuid", collection.getID());
        query.setHint("org.hibernate.cacheable", Boolean.TRUE);

        return list(query);
    }

    @Override
    public EtdUnit findByName(Context context, String name) throws SQLException {
        Query query = createQuery(context,
            "SELECT eu from EtdUnit eu " +
                "where eu.name = :name ");

        query.setParameter("name", name);
        query.setHint("org.hibernate.cacheable", Boolean.TRUE);

        return singleResult(query);
    }

    @Override
    public List<EtdUnit> findByNameLike(final Context context, final String etdunitName, final int offset,
        final int limit) throws SQLException {
        Query query = createQuery(context,
            "SELECT eu FROM EtdUnit eu WHERE lower(eu.name) LIKE lower(:name)");
        query.setParameter("name", "%" + StringUtils.trimToEmpty(etdunitName) + "%");

        if (0 <= offset) {
            query.setFirstResult(offset);
        }
        if (0 <= limit) {
            query.setMaxResults(limit);
        }

        return list(query);
    }

    @Override
    public int countByNameLike(final Context context, final String etdunitName) throws SQLException {
        Query query = createQuery(context,
            "SELECT count(*) FROM EtdUnit eu WHERE lower(eu.name) LIKE lower(:name)");
        query.setParameter("name", "%" + etdunitName + "%");

        return count(query);
    }

    @Override
    public List<EtdUnit> findAll(Context context, int pageSize, int offset) throws SQLException {
        Query query = createQuery(context,
            "SELECT eu FROM EtdUnit eu ORDER BY eu.name ASC");
        query.setHint("org.hibernate.cacheable", Boolean.TRUE);

        return list(query);
    }

    @Override
    public int countRows(Context context) throws SQLException {
        return count(createQuery(context, "SELECT count(*) FROM EtdUnit"));
    }
}
