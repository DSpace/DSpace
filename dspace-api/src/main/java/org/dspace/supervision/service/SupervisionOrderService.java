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

    SupervisionOrder create(Context context, Item item, Group group) throws SQLException;
    List<SupervisionOrder> findAll(Context context) throws SQLException;
    List<SupervisionOrder> findByItem(Context context, Item item) throws SQLException;
    SupervisionOrder findByItemAndGroup(Context context, Item item, Group group) throws SQLException;
    boolean isSupervisor(Context context, EPerson ePerson, Item item) throws SQLException;
}
