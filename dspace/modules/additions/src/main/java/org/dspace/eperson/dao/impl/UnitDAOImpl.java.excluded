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

import org.dspace.eperson.Group;
import org.dspace.eperson.Unit;
import org.dspace.eperson.dao.UnitDAO;
import org.dspace.core.AbstractHibernateDSODAO;
import org.dspace.core.Context;
import org.hibernate.Criteria;
import org.hibernate.FetchMode;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

/**
 * Hibernate implementation of the Database Access Object interface class for the Unit object.
 * This class is responsible for all database calls for the Unit object and is autowired by spring
 * This class should never be accessed directly.
 *
 * @author mohideen at umd.edu
 */
public class UnitDAOImpl extends AbstractHibernateDSODAO<Unit> implements UnitDAO
{
    protected UnitDAOImpl()
    {
        super();
    }

    @Override
    public List<Unit> findAllByGroup(Context context, Group group) throws SQLException {
      Criteria criteria = createCriteria(context, Unit.class);
      criteria.setFetchMode("Group", FetchMode.JOIN)
              .add(Restrictions.eq("id", group.getID()));
      return list(criteria);
    }

    @Override
    public Unit findByName(Context context, String name) throws SQLException {
      Criteria criteria = createCriteria(context, Unit.class);
      criteria.add(Restrictions.eq("name", name))
              .setFirstResult(0)
              .setMaxResults(1);
      List<Unit> units = list(criteria);
      if (units.isEmpty()) {
        return null;
      }
      return units.get(0);
    }

    @Override
    public List<Unit> searchByName(Context context, String query, int offset, int limit) throws SQLException {
      Criteria criteria = searchByNameCriteria(context, query);
      if (offset > 0) {
        criteria.setFirstResult(offset);
      }
      if (limit > 0) {
        criteria.setMaxResults(limit);
      }

      return list(criteria);
    }

    @Override
    public int searchByNameResultCount(Context context, String query) throws SQLException {
      return count(searchByNameCriteria(context, query));
    }

    @Override
    public List<Unit> findAllSortedByName(Context context) throws SQLException
    {
      Criteria criteria = createCriteria(context, Unit.class);
      criteria.addOrder(Order.asc("name.value"));
      return list(criteria);
    }

    @Override
    public int countRows(Context context) throws SQLException {
        return count(createQuery(context, "SELECT count(*) FROM Unit"));
    }

    private Criteria searchByNameCriteria(Context context, String query) throws SQLException {
      Criteria criteria = createCriteria(context, Unit.class);
      criteria.add(Restrictions.like("name", query, MatchMode.ANYWHERE));
      return criteria;
    }
}
