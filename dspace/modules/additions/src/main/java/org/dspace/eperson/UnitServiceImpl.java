/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.eperson;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.DSpaceObjectServiceImpl;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogHelper;
import org.dspace.eperson.dao.UnitDAO;
import org.dspace.eperson.service.GroupService;
import org.dspace.eperson.service.UnitService;
import org.dspace.event.Event;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Service implementation for the Unit object.
 * This class is responsible for all business logic calls for the Unit object and is autowired by spring.
 * This class should never be accessed directly.
 *
 * @author mohideen at umd.edu
 */
public class UnitServiceImpl extends DSpaceObjectServiceImpl<Unit> implements UnitService {

    /** log4j category */
    private static Logger log = Logger.getLogger(UnitServiceImpl.class);

    @Autowired(required = true)
    protected UnitDAO unitDAO;
    @Autowired(required = true)
    protected GroupService groupService;
    @Autowired(required = true)
    protected AuthorizeService authorizeService;

    protected UnitServiceImpl()
    {
        super();

    }

    @Override
    public Unit create(Context context) throws SQLException, AuthorizeException {
        // Authorize
        canEdit(context);

        Unit newUnit = unitDAO.create(context, new Unit());

        unitDAO.save(context, newUnit);

        context.addEvent(new Event(Event.CREATE, Constants.UNIT, newUnit.getID(), newUnit.getName()));

        log.info(LogHelper.getHeader(context, "create_unit",
                "unit_id=" + newUnit.getID()));

        return newUnit;
    }

    @Override
    public Unit find(Context context, UUID id) throws SQLException {
        return unitDAO.findByID(context, Unit.class, id);
    }

    @Override
    public Unit findByName(Context context, String name) throws SQLException {
      return unitDAO.findByName(context, name);
    }

    @Override
    public List<Unit> findAll(Context context) throws SQLException {
        return unitDAO.findAllSortedByName(context);
    }

    @Override
    public List<Unit> findAllByGroup(Context context, Group group) throws SQLException {
      return unitDAO.findAllByGroup(context, group);
    }

    @Override
    public List<Unit> search(Context context, String query) throws SQLException {
      return search(context, query, -1, -1);
    }

    @Override
    public List<Unit> search(Context context, String query, int offset, int limit) throws SQLException {
      return unitDAO.searchByName(context, query, offset, limit);
    }

    @Override
    public int searchResultCount(Context context, String query) throws SQLException {
      return unitDAO.searchByNameResultCount(context, query);
    }

    
    @Override
    public void update(Context context, Unit unit) throws SQLException, AuthorizeException {
        // Authorize
        canEdit(context);

        log.info(LogHelper.getHeader(context, "update_unit",
                "unit_id=" + unit.getID()));

        super.update(context, unit);

        unitDAO.save(context, unit);
        if (unit.isModified())
        {
            context.addEvent(new Event(Event.MODIFY, Constants.UNIT, unit.getID(), unit.getName()));
            unit.clearModified();
        }
        unit.clearDetails();
    }

   
    @Override
    public List<Group> getAllGroups(Context context, Unit unit) throws SQLException {
        return unit.getGroups();
    }
    
    @Override
    public void addGroup(Context context, Unit unit, Group group) throws SQLException, AuthorizeException {
         // Authorize
        canEdit(context);

        log.info(LogHelper.getHeader(context, "add_group",
                "unit_id=" + unit.getID() + ",group_id=" + group.getID()));

        unit.addGroup(group);
        
        context.addEvent(new Event(Event.ADD, Constants.UNIT, unit.getID(),
        Constants.COLLECTION, group.getID(), unit.getName()));
    }


    @Override
    public void removeGroup(Context context, Unit unit, Group group) throws SQLException, AuthorizeException {
         // Authorize
        canEdit(context);

        log.info(LogHelper.getHeader(context, "add_group",
                "unit_id=" + unit.getID() + ",group_id=" + group.getID()));

        unit.removeGroup(group);
        
        context.addEvent(new Event(Event.REMOVE, Constants.UNIT, unit.getID(),
        Constants.COLLECTION, group.getID(), unit.getName()));
    }

    @Override
    public boolean isMember(Unit unit, Group group) {
      return unit.isMember(group);
    }

   
    @Override
    public void delete(Context context, Unit unit) throws SQLException, AuthorizeException, IOException {
        // Authorize
        canEdit(context);

        log.info(LogHelper.getHeader(context, "delete_unit",
                "unit_id=" + unit.getID()));

        UUID removedId = unit.getID();

        unitDAO.delete(context, unit);

        context.addEvent(new Event(Event.REMOVE, Constants.UNIT, removedId, unit.getName()));
    }

    @Override
    public int getSupportsTypeConstant() {
        return Constants.UNIT;
    }



    @Override
    public boolean canEditBoolean(Context context) throws SQLException {
        try
        {
            canEdit(context);

            return true;
        }
        catch (AuthorizeException e)
        {
            return false;
        }
    }

    @Override
    public void canEdit(Context context) throws AuthorizeException, SQLException {
        // Check authorisation
        if (!(authorizeService.isAdmin(context)))
        {
            throw new AuthorizeException("Only administrators can create or modify units and its associations");
        }
    }

    @Override
    public void updateLastModified(Context context, Unit unit) {
        //Also fire a modified event since the unit HAS been modified
        context.addEvent(new Event(Event.MODIFY, Constants. UNIT,
                unit.getID(), unit.getName()));

    }

    @Override
    public Unit findByIdOrLegacyId(Context context, String id) throws SQLException {
        if(StringUtils.isNumeric(id))
        {
            return findByLegacyId(context, Integer.parseInt(id));
        }
        else
        {
            return find(context, UUID.fromString(id));
        }
    }

    @Override
    public Unit findByLegacyId(Context context, int id) throws SQLException {
        return unitDAO.findByLegacyId(context, id, Unit.class);
    }

    @Override
    public int countTotal(Context context) throws SQLException {
        return unitDAO.countRows(context);
    }
}
