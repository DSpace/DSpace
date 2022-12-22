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
import org.dspace.core.Context;
import org.dspace.core.GenericDAO;
import org.dspace.eperson.Group;
import org.dspace.supervision.SupervisionOrder;

/**
 * Database Access Object interface class for the SupervisionOrder object.
 *
 * The implementation of this class is responsible for all database calls for the SupervisionOrder object
 * and is autowired by spring
 *
 * @author Mohamed Eskander (mohamed.eskander at 4science dot it)
 */
public interface SupervisionOrderDao extends GenericDAO<SupervisionOrder> {

    List<SupervisionOrder> findByItem(Context context, Item item) throws SQLException;
    SupervisionOrder findByItemAndGroup(Context context, Item item, Group group) throws SQLException;

}
