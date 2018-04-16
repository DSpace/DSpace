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

import org.dspace.content.Item;
import org.dspace.content.Relationship;
import org.dspace.content.dao.RelationshipDAO;
import org.dspace.core.AbstractHibernateDAO;
import org.dspace.core.Context;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

public class RelationshipDAOImpl extends AbstractHibernateDAO<Relationship> implements RelationshipDAO {

    public List<Relationship> findByItem(Context context,Item item) throws SQLException {
        Criteria criteria = createCriteria(context,Relationship.class);
        criteria.add(Restrictions.or(
            Restrictions.eq("leftItem", item),
            Restrictions.eq("rightItem", item)
        ));

        return list(criteria);
    }

    public int findLeftPlaceByLeftItem(Context context, Item item) throws SQLException {
        Criteria criteria = createCriteria(context, Relationship.class);
        criteria.add(Restrictions.and(
            Restrictions.eq("leftItem", item)
        ));

        List<Relationship> list = list(criteria);
        list.sort((o1, o2) -> o2.getLeftPlace() - o1.getLeftPlace());
        if (!list.isEmpty()) {
            return list.get(0).getLeftPlace();
        } else {
            return 1;
        }
    }

    public int findRightPlaceByRightItem(Context context, Item item) throws SQLException {
        Criteria criteria = createCriteria(context, Relationship.class);
        criteria.add(Restrictions.and(
            Restrictions.eq("rightItem", item)
        ));

        List<Relationship> list = list(criteria);
        list.sort((o1, o2) -> o2.getRightPlace() - o1.getRightPlace());
        if (!list.isEmpty()) {
            return list.get(0).getLeftPlace();
        } else {
            return 1;
        }
    }
}
