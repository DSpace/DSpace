/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.supervision.service;

import java.sql.SQLException;
import java.util.List;

import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.service.DSpaceCRUDService;
import org.dspace.supervision.SupervisionOrder;

/**
 * Service interface class for the SupervisionOrder object.
 *
 * @author Mohamed Eskander (mohamed.eskander at 4science dot it)
 */
public interface SupervisionOrderService extends DSpaceCRUDService<SupervisionOrder> {

    /**
     * Creates a new SupervisionOrder
     *
     * @param context The DSpace context
     * @param item the item
     * @param group the group
     * @return the created Supervision Order on item and group
     * @throws SQLException If something goes wrong in the database
     */
    SupervisionOrder create(Context context, Item item, Group group) throws SQLException;

    /**
     * Find all supervision orders currently stored
     *
     * @param context The DSpace context
     * @return all Supervision Orders
     * @throws SQLException If something goes wrong in the database
     */
    List<SupervisionOrder> findAll(Context context) throws SQLException;

    /**
     * Find all supervision orders for a given Item
     *
     * @param context The DSpace context
     * @param item the item
     * @return all Supervision Orders related to the item
     * @throws SQLException If something goes wrong in the database
     */
    List<SupervisionOrder> findByItem(Context context, Item item) throws SQLException;

    /**
     *
     * Find a supervision order depending on given Item and Group
     *
     * @param context The DSpace context
     * @param item the item
     * @param group the group
     * @return the Supervision Order of the item and group
     * @throws SQLException If something goes wrong in the database
     */
    SupervisionOrder findByItemAndGroup(Context context, Item item, Group group) throws SQLException;

    /**
     *
     * Checks if an EPerson is supervisor of an Item
     *
     * @param context The DSpace context
     * @param ePerson the ePerson to be checked
     * @param item the item
     * @return true if the ePerson is a supervisor of the item
     * @throws SQLException If something goes wrong in the database
     */
    boolean isSupervisor(Context context, EPerson ePerson, Item item) throws SQLException;
}
