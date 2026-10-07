/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.supervision;

import java.sql.SQLException;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.service.GroupService;
import org.dspace.event.Event;
import org.dspace.supervision.dao.SupervisionOrderDao;
import org.dspace.supervision.service.SupervisionOrderService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of {@link SupervisionOrderService}
 *
 * @author Mohamed Eskander (mohamed.eskander at 4science dot it)
 */
public class SupervisionOrderServiceImpl implements SupervisionOrderService {

    @Autowired(required = true)
    private SupervisionOrderDao supervisionDao;

    @Autowired(required = true)
    private GroupService groupService;

    @Autowired(required = true)
    private ItemService itemService;

    protected SupervisionOrderServiceImpl() {

    }

    @Override
    public SupervisionOrder create(Context context) throws SQLException, AuthorizeException {
        return supervisionDao.create(context, new SupervisionOrder());
    }

    @Override
    public SupervisionOrder find(Context context, int id) throws SQLException {
        return supervisionDao.findByID(context, SupervisionOrder.class, id);
    }

    @Override
    public void update(Context context, SupervisionOrder supervisionOrder)
        throws SQLException, AuthorizeException {
        supervisionDao.save(context, supervisionOrder);
    }

    @Override
    public void update(Context context, List<SupervisionOrder> supervisionOrders)
        throws SQLException, AuthorizeException {
        if (CollectionUtils.isNotEmpty(supervisionOrders)) {
            for (SupervisionOrder supervisionOrder : supervisionOrders) {
                supervisionDao.save(context, supervisionOrder);
            }
        }
    }

    @Override
    public void delete(Context context, SupervisionOrder supervisionOrder) throws SQLException, AuthorizeException {
        supervisionDao.delete(context, supervisionOrder);
    }

    @Override
    public SupervisionOrder create(Context context, Item item, Group group) throws SQLException {
        SupervisionOrder supervisionOrder = new SupervisionOrder();
        supervisionOrder.setItem(item);
        supervisionOrder.setGroup(group);
        SupervisionOrder supOrder = supervisionDao.create(context, supervisionOrder);
        context.addEvent(new Event(Event.MODIFY, Constants.ITEM, item.getID(), null,
            itemService.getIdentifiers(context, item)));
        return supOrder;
    }

    @Override
    public List<SupervisionOrder> findAll(Context context) throws SQLException {
        return supervisionDao.findAll(context, SupervisionOrder.class);
    }

    @Override
    public List<SupervisionOrder> findByItem(Context context, Item item) throws SQLException {
        return supervisionDao.findByItem(context, item);
    }

    @Override
    public SupervisionOrder findByItemAndGroup(Context context, Item item, Group group) throws SQLException {
        return supervisionDao.findByItemAndGroup(context, item, group);
    }

    @Override
    public boolean isSupervisor(Context context, EPerson ePerson, Item item) throws SQLException {
        List<SupervisionOrder> supervisionOrders = findByItem(context, item);

        if (CollectionUtils.isEmpty(supervisionOrders)) {
            return false;
        }

        return supervisionOrders
            .stream()
            .map(SupervisionOrder::getGroup)
            .anyMatch(group -> isMember(context, ePerson, group));
    }

    private boolean isMember(Context context, EPerson ePerson, Group group) {
        try {
            return groupService.isMember(context, ePerson, group);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

}
