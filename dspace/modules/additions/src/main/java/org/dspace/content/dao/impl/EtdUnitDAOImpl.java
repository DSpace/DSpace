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

import org.dspace.content.Collection;
import org.dspace.content.EtdUnit;
import org.dspace.content.dao.EtdUnitDAO;
import org.dspace.core.AbstractHibernateDSODAO;
import org.dspace.core.Context;
import org.hibernate.Criteria;
import org.hibernate.FetchMode;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

/**
 * Hibernate implementation of the Database Access Object interface class for the EtdUnit object.
 * This class is responsible for all database calls for the EtdUnit object and is autowired by spring
 * This class should never be accessed directly.
 *
 * @author kevinvandevelde at atmire.com
 */
public class EtdUnitDAOImpl extends AbstractHibernateDSODAO<EtdUnit> implements EtdUnitDAO
{
    protected EtdUnitDAOImpl()
    {
        super();
    }

    @Override
    public List<EtdUnit> findAllByCollection(Context context, Collection collection) throws SQLException {
      Criteria criteria = createCriteria(context, EtdUnit.class);
      criteria.setFetchMode("Collection", FetchMode.JOIN)
              .add(Restrictions.eq("id", collection.getID()));
      return list(criteria);
    }

    @Override
    public EtdUnit findByName(Context context, String name) throws SQLException {
      Criteria criteria = createCriteria(context, EtdUnit.class);
      criteria.add(Restrictions.eq("name", name))
              .setFirstResult(0)
              .setMaxResults(1);
      List<EtdUnit> etdunits = list(criteria);
      if (etdunits.isEmpty()) {
        return null;
      }
      return etdunits.get(0);
    }

    @Override
    public List<EtdUnit> searchByName(Context context, String query, int offset, int limit) throws SQLException {
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
    public List<EtdUnit> findAllSortedByName(Context context) throws SQLException
    {
      Criteria criteria = createCriteria(context, EtdUnit.class);
      criteria.addOrder(Order.asc("name.value"));
      return list(criteria);
    }

    @Override
    public int countRows(Context context) throws SQLException {
        return count(createQuery(context, "SELECT count(*) FROM EtdUnit"));
    }

    private Criteria searchByNameCriteria(Context context, String query) throws SQLException {
      Criteria criteria = createCriteria(context, EtdUnit.class);
      criteria.add(Restrictions.like("name", query, MatchMode.ANYWHERE));
      return criteria;
    }
}
