/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.eperson.dao.impl;

import java.sql.SQLException;
import java.util.List;
import javax.persistence.Query;

import org.apache.commons.lang3.StringUtils;
import org.dspace.core.AbstractHibernateDSODAO;
import org.dspace.core.Context;
import org.dspace.eperson.Group;
import org.dspace.eperson.Unit;
import org.dspace.eperson.dao.UnitDAO;

/**
 * Hibernate implementation of the Database Access Object interface class for the Unit object.
 * This class is responsible for all database calls for the Unit object and is autowired by spring
 * This class should never be accessed directly.
 *
 * @author mohideen at umd.edu
 */
public class UnitDAOImpl extends AbstractHibernateDSODAO<Unit> implements UnitDAO {
    protected UnitDAOImpl() {
        super();
    }

    @Override
    public Unit findByName(Context context, String name) throws SQLException {
        Query query = createQuery(context,
                                  "SELECT u from Unit u " +
                                      "where u.name = :name ");

        query.setParameter("name", name);
        query.setHint("org.hibernate.cacheable", Boolean.TRUE);

        return singleResult(query);
    }

    @Override
    public List<Unit> findByNameLike(final Context context, final String unitName, final int offset, final int limit)
        throws SQLException {
        Query query = createQuery(context,
                                  "SELECT u FROM Unit u WHERE lower(u.name) LIKE lower(:name)");
        query.setParameter("name", "%" + StringUtils.trimToEmpty(unitName) + "%");

        if (0 <= offset) {
            query.setFirstResult(offset);
        }
        if (0 <= limit) {
            query.setMaxResults(limit);
        }

        return list(query);
    }

    @Override
    public int countByNameLike(final Context context, final String unitName) throws SQLException {
        Query query = createQuery(context,
                                  "SELECT count(*) FROM Unit u WHERE lower(u.name) LIKE lower(:name)");
        query.setParameter("name", "%" + unitName + "%");

        return count(query);
    }

    @Override
    public List<Unit> findAll(Context context, int pageSize, int offset) throws SQLException {
        Query query = createQuery(context,
                                  "SELECT u FROM Unit u ORDER BY u.name ASC");
        if (pageSize > 0) {
            query.setMaxResults(pageSize);
        }
        if (offset > 0) {
            query.setFirstResult(offset);
        }
        query.setHint("org.hibernate.cacheable", Boolean.TRUE);

        return list(query);
    }

    @Override
    public List<Unit> findByGroup(Context context, Group group) throws SQLException {
        Query query = createQuery(context,
                                  "from Unit where (from Group g where g.id = :group_id) in elements(group)");
        query.setParameter("group_id", group.getID());
        query.setHint("org.hibernate.cacheable", Boolean.TRUE);

        return list(query);
    }

    @Override
    public int countRows(Context context) throws SQLException {
        return count(createQuery(context, "SELECT count(*) FROM Unit"));
    }
}
