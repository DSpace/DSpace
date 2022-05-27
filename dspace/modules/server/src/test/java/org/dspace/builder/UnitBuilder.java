package org.dspace.builder;

import java.io.IOException;
import java.sql.SQLException;
import java.util.UUID;

import org.dspace.authorize.AuthorizeException;
import static org.dspace.builder.AbstractBuilder.groupService;
import org.dspace.content.service.DSpaceObjectService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.Unit;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.UnitService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Builder to construct Unit objects
 */
public class UnitBuilder extends AbstractDSpaceObjectBuilder<Unit> {

    private Unit unit;
    private static UnitService unitService = EPersonServiceFactory.getInstance().getUnitService();
    protected UnitBuilder(Context context) {
        super(context);

    }

    @Override
    public void cleanup() throws Exception {
        try (Context c = new Context()) {
            c.turnOffAuthorisationSystem();
            // Ensure object and any related objects are reloaded before checking to see what needs cleanup
            unit = c.reloadEntity(unit);
            if (unit != null) {
                delete(c, unit);
                c.complete();
            }
        }
    }

    public static UnitBuilder createUnit(final Context context) {
        UnitBuilder builder = new UnitBuilder(context);
        return builder.create(context);
    }

    private UnitBuilder create(final Context context) {
        this.context = context;
        try {
            unit = unitService.create(context);
        } catch (Exception e) {
            return handleException(e);
        }
        return this;
    }

    @Override
    protected DSpaceObjectService<Unit> getService() {
        return unitService;
    }

    @Override
    public Unit build() {
        try {
            unitService.update(context, unit);
        } catch (Exception e) {
            return handleException(e);
        }
        return unit;
    }

    public UnitBuilder withName(String unitName) {
        try {
            unit.setName(unitName);
        } catch (Exception e) {
            return handleException(e);
        }
        return this;
    }

    public UnitBuilder addGroup(Group group) {
        try {
            unitService.addGroup(context, unit, group);
        } catch (Exception e) {
            return handleException(e);
        }
        return this;
    }

    public static void deleteUnit(UUID uuid) throws SQLException, IOException {
        try (Context c = new Context()) {
            c.turnOffAuthorisationSystem();
            Unit unit = unitService.find(c, uuid);
            if (unit != null) {
                try {
                    unitService.delete(c, unit);
                } catch (AuthorizeException e) {
                    // cannot occur, just wrap it to make the compiler happy
                    throw new RuntimeException(e.getMessage(), e);
                }
            }
            c.complete();
        }
    }

}

