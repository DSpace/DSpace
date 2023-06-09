/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.supervision.dao.impl;

import java.sql.SQLException;
import java.util.List;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.dspace.content.Item;
import org.dspace.core.AbstractHibernateDAO;
import org.dspace.core.Context;
import org.dspace.eperson.Group;
import org.dspace.supervision.SupervisionOrder;
import org.dspace.supervision.SupervisionOrder_;
import org.dspace.supervision.dao.SupervisionOrderDao;

/**
 * Hibernate implementation of the Database Access Object interface class for the SupervisionOrder object.
 * This class is responsible for all database calls for the SupervisionOrder object
 * and is autowired by spring
 *
 * @author Mohamed Eskander (mohamed.eskander at 4science dot it)
 */
public class SupervisionOrderDaoImpl extends AbstractHibernateDAO<SupervisionOrder> implements SupervisionOrderDao {

    @Override
    public List<SupervisionOrder> findByItem(Context context, Item item) throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, SupervisionOrder.class);

        Root<SupervisionOrder> supervisionOrderRoot = criteriaQuery.from(SupervisionOrder.class);
        criteriaQuery.select(supervisionOrderRoot);
        criteriaQuery.where(criteriaBuilder.equal(supervisionOrderRoot.get(SupervisionOrder_.item), item));

        return list(context, criteriaQuery, false, SupervisionOrder.class, -1, -1);
    }

    @Override
    public SupervisionOrder findByItemAndGroup(Context context, Item item, Group group) throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, SupervisionOrder.class);

        Root<SupervisionOrder> supervisionOrderRoot = criteriaQuery.from(SupervisionOrder.class);
        criteriaQuery.select(supervisionOrderRoot);
        criteriaQuery.where(criteriaBuilder.and(
            criteriaBuilder.equal(supervisionOrderRoot.get(SupervisionOrder_.item), item),
            criteriaBuilder.equal(supervisionOrderRoot.get(SupervisionOrder_.group), group)
        ));

        return singleResult(context, criteriaQuery);
    }
}
