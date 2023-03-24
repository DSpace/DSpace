/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.dao.impl;

import org.dspace.content.Collection;
import org.dspace.core.Context;
import org.dspace.core.AbstractHibernateDAO;
import org.dspace.content.Umrestricted;
import org.dspace.content.dao.UmrestrictedDAO;
import org.hibernate.Criteria;
import javax.persistence.Query;
//import org.hibernate.Query;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import java.sql.SQLException;
import java.util.List;
import java.util.Iterator;


/**
 * Hibernate implementation of the Database Access Object interface class for the Subscription object.
 * This class is responsible for all database calls for the Subscription object and is autowired by spring
 * This class should never be accessed directly.
 *
 * @author kevinvandevelde at atmire.com
 */
public class UmrestrictedDAOImpl extends AbstractHibernateDAO<Umrestricted> implements UmrestrictedDAO
{
    protected UmrestrictedDAOImpl()
    {
        super();
    }

    @Override
    public void createUmrestricted(Context context, String item_id, String release_date) throws SQLException {
        //String myQuery = "INSERT into Umrestricted values (items_id, date)";
        //createQuery(context, myQuery);

        Query query = getHibernateSession(context).createSQLQuery("INSERT INTO umrestricted (item_id, release_date) VALUES (:item_id, :release_date)");
        query.setParameter("item_id", item_id);
        query.setParameter("release_date", release_date);
        query.executeUpdate();

        return;
    }

    @Override
    public void deleteUmrestricted(Context context, String item_id) throws SQLException {
        //String myQuery = "DELETE from Umrestricted where item_id = '" + item_id + "'";
        //createQuery(context, myQuery);

        Query query = getHibernateSession(context)
            .createSQLQuery("DELETE FROM umrestricted WHERE item_id=:item_id");
        query.setParameter("item_id", item_id);
        query.executeUpdate();

        return;
    }


    @Override
    public Iterator<Umrestricted> findAllUmrestricted(Context context ) throws SQLException {
        Query query = createQuery(context, "SELECT u from Umrestricted u");
        return iterate(query);
    }

    @Override
    public Iterator<Umrestricted> findAllByItemIdUmrestricted(Context context, String item_id ) throws SQLException {
        Query query = createQuery(context, "SELECT u from Umrestricted u where u.item_id='" + item_id + "'");
        return iterate(query);
    }

    @Override
    public Iterator<Umrestricted> findAllByDateUmrestricted(Context context, String date ) throws SQLException {
        Query query = createQuery(context, "SELECT u from Umrestricted u where u.release_date<'" + date + "'");
        return iterate(query);
    }


}