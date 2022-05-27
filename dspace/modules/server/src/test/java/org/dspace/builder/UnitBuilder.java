package org.dspace.builder;

import java.io.IOException;
import java.sql.SQLException;
import java.util.UUID;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.service.DSpaceObjectService;
import org.dspace.core.Context;
import org.dspace.eperson.Group;
import org.dspace.eperson.Unit;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.UnitService;

/**
 * Builder to construct Unit objects
 */
public class UnitBuilder extends AbstractDSpaceObjectBuilder<Unit> {
    /**
     * The unit being built
     */
    private Unit unit;

    /**
     * The UnitService instance
     */
    private static final UnitService unitService = EPersonServiceFactory.getInstance().getUnitService();

    /**
     * Constructs a UnitBuilder with the given Context
     * @param context the DSpace context
     */
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

    /**
     * Returns a UnitBuilder initialized with the given Context
     * @param context the DSpace context
     * @return a UnitBuilder with the given context
     */
    public static UnitBuilder createUnit(final Context context) {
        UnitBuilder builder = new UnitBuilder(context);
        return builder.create(context);
    }

    /**
     * Initializes a new UnitBuilder instance with the given context
     *
     * @param context the DSpace context
     * @return a UnitBuilder with the given context
     */
    private UnitBuilder create(final Context context) {
        this.context = context;
        try {
            unit = unitService.create(context);
        } catch (SQLException | AuthorizeException e) {
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
        } catch (SQLException | AuthorizeException e) {
            return handleException(e);
        }
        return unit;
    }

    /**
     * Sets the name of the Unit being built
     *
     * @param unitName the name of the unit
     * @return the UnitBuilder instance
     */
    public UnitBuilder withName(String unitName) {
        try {
            unit.setName(unitName);
        } catch (Exception e) {
            return handleException(e);
        }
        return this;
    }

    /**
     * Sets the given Group to the Unit being built
     *
     * @param group the Group to add to the unit
     * @return the UnitBuilder instance
     */
    public UnitBuilder addGroup(Group group) {
        try {
            unitService.addGroup(context, unit, group);
        } catch (SQLException | AuthorizeException e) {
            return handleException(e);
        }
        return this;
    }

    /**
     * Deletes the Unit with the given UUID from the database.
     *
     * @param uuid the UUID of the unit to delete.
     */
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
