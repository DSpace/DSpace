/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.supervision.dao;

import java.sql.SQLException;
import java.util.List;

import org.dspace.content.Item;
import org.dspace.core.GenericDAO;
import org.dspace.eperson.Group;
import org.dspace.supervision.SupervisionOrder;
import org.hibernate.Session;

/**
 * Database Access Object interface class for the SupervisionOrder object.
 *
 * The implementation of this class is responsible for all database calls for the SupervisionOrder object
 * and is autowired by spring
 *
 * @author Mohamed Eskander (mohamed.eskander at 4science dot it)
 */
public interface SupervisionOrderDao extends GenericDAO<SupervisionOrder> {

    /**
     * find all Supervision Orders related to the item
     *
     * @param session The current request's database context.
     * @param item the item
     * @return the Supervision Orders related to the item
     * @throws SQLException If something goes wrong in the database
     */
    List<SupervisionOrder> findByItem(Session session, Item item) throws SQLException;

    /**
     * find the Supervision Order related to the item and group
     *
     * @param session The current request's database context.
     * @param item the item
     * @param group the group
     * @return the Supervision Order related to the item and group
     * @throws SQLException If something goes wrong in the database
     */
    SupervisionOrder findByItemAndGroup(Session session, Item item, Group group) throws SQLException;

}
