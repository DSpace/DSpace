/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.requestitem.dao.impl;

import java.sql.SQLException;
import java.util.Iterator;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.dspace.app.requestitem.RequestItem;
import org.dspace.app.requestitem.RequestItem_;
import org.dspace.app.requestitem.dao.RequestItemDAO;
import org.dspace.content.Item;
import org.dspace.core.AbstractHibernateDAO;
import org.dspace.core.Context;

/**
 * Hibernate implementation of the Database Access Object interface class for the RequestItem object.
 * This class is responsible for all database calls for the RequestItem object and is autowired by Spring.
 * This class should never be accessed directly.
 *
 * @author kevinvandevelde at atmire.com
 */
public class RequestItemDAOImpl extends AbstractHibernateDAO<RequestItem> implements RequestItemDAO {
    protected RequestItemDAOImpl() {
        super();
    }

    @Override
    public RequestItem findByToken(Context context, String token) throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, RequestItem.class);
        Root<RequestItem> requestItemRoot = criteriaQuery.from(RequestItem.class);
        criteriaQuery.select(requestItemRoot);
        criteriaQuery.where(criteriaBuilder.equal(requestItemRoot.get(RequestItem_.token), token));
        return uniqueResult(context, criteriaQuery, false, RequestItem.class);
    }
    @Override
    public Iterator<RequestItem> findByItem(Context context, Item item) throws SQLException {
        Query query = createQuery(context, "FROM RequestItem WHERE item_id= :uuid");
        query.setParameter("uuid", item.getID());
        return iterate(query);
    }
}
