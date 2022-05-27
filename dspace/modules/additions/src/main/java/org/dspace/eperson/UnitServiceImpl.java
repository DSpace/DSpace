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
import java.util.ArrayList;
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
import org.dspace.eperson.service.UnitService;
import org.dspace.event.Event;
import org.dspace.util.UUIDUtils;
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
    private static final Logger log = Logger.getLogger(UnitServiceImpl.class);

    @Autowired(required = true)
    protected UnitDAO unitDAO;
    @Autowired(required = true)
    protected AuthorizeService authorizeService;

    protected UnitServiceImpl() {
        super();
    }

    @Override
    public Unit create(Context context) throws SQLException, AuthorizeException {
        // Authorize
        canEdit(context);

        Unit newUnit = unitDAO.create(context, new Unit());

        log.info(LogHelper.getHeader(context, "create_unit", "unit_id=" + newUnit.getID()));

        context.addEvent(new Event(Event.CREATE, Constants.UNIT, newUnit.getID(), newUnit.getName()));

        return newUnit;
    }

    @Override
    public void setName(Unit unit, String name) throws SQLException {
        unit.setName(name);
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
    public List<Unit> findAll(Context context, int pageSize, int offset) throws SQLException {
        return unitDAO.findAll(context, pageSize, offset);
    }

    @Override
    public List<Unit> findAllByGroup(Context context, Group group) throws SQLException {
        return unitDAO.findByGroup(context, group);
    }

    @Override
    public List<Unit> search(Context context, String query) throws SQLException {
        return search(context, query, -1, -1);
    }

    @Override
    public List<Unit> search(Context context, String query, int offset, int limit) throws SQLException {
        List<Unit> units = new ArrayList<>();
        UUID uuid = UUIDUtils.fromString(query);
        if (uuid == null) {
            // Search by unit name
            units = unitDAO.findByNameLike(context, query, offset, limit);
        } else {
            // Search by unit id
            Unit unit = find(context, uuid);
            if (unit != null) {
                units.add(unit);
            }
        }
        return units;
    }

    @Override
    public int searchResultCount(Context context, String query) throws SQLException {
        UUID uuid = UUIDUtils.fromString(query);
        if (uuid == null) {
            // Search by unit name
            return unitDAO.countByNameLike(context, query);
        } else {
            // Search by unit id
            Unit unit = find(context, uuid);
            if (unit != null) {
                return 1;
            }
        }
        return 0;
    }

    @Override
    public void update(Context context, Unit unit) throws SQLException, AuthorizeException {
        // Authorize
        canEdit(context);

        super.update(context, unit);

        unitDAO.save(context, unit);

        log.info(LogHelper.getHeader(context, "update_unit", "unit_id=" + unit.getID()));

        if (unit.isModified()) {
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
        try {
            canEdit(context);

            return true;
        } catch (AuthorizeException e) {
            return false;
        }
    }

    @Override
    public void canEdit(Context context) throws AuthorizeException, SQLException {
        // Check authorisation
        if (!authorizeService.isAdmin(context)) {
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
        if (StringUtils.isNumeric(id)) {
            return findByLegacyId(context, Integer.parseInt(id));
        } else {
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
