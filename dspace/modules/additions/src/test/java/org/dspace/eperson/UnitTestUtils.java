package org.dspace.eperson;

import java.io.IOException;
import java.sql.SQLException;

import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.UnitService;

/**
 * Convenience methods for handling Units in tests.
 */
public class UnitTestUtils {
    /**
     * Disable constructor (for Checkstyle)
     */
    private UnitTestUtils() {}

    /**
     * Creates a Unit with the given parameters, storing it in the database.
     *
     * @param context the DSpace context
     * @param name the name of the Unit
     * @param facultyOnly true if the unit is faculty-only, false otherwise.
     * @return the newly create Unit
     * @throws SQLException if a database error occurs
     * @throws AuthorizeException if an authorization error occurs
     */
    public static Unit createUnit(Context context, String name, boolean facultyOnly)
            throws SQLException, AuthorizeException {
        UnitService unitService = EPersonServiceFactory.getInstance().getUnitService();

        context.turnOffAuthorisationSystem();
        Unit unit = unitService.create(context);
        unit.setName(name);
        unit.setFacultyOnly(facultyOnly);
        unitService.update(context, unit);
        context.restoreAuthSystemState();
        return unit;
    }

    /**
     * Retrieves the Unit with the given name from the database, or returns null
     * if a Unit with the given name cannot be found.
     * @param context the DSpace context
     * @param unitName the name of the Unit to retrieve
     * @return the Unit with the given name from the database, or null if a
     * Unit with the given name is not found.
     * @throws SQLException if a database error occurs.
     */
    public static Unit getUnitFromDatabase(Context context, String unitName) throws SQLException {
        UnitService unitService = EPersonServiceFactory.getInstance().getUnitService();

        return unitService.findByName(context, unitName);
    }

    /**
     * Deletes the given Unit from the database.
     *
     * @param context the DSpace context
     * @param unit the Unit to delete
     * @throws SQLException if a database error occurs
     * @throws AuthorizeException if an authorization error occurs
     * @throws IOException if an I/O error occurs.
     */
    public static void deleteUnit(Context context, Unit unit)
            throws SQLException, AuthorizeException, IOException {
        if (unit != null) {
            context.turnOffAuthorisationSystem();
            UnitService unitService = EPersonServiceFactory.getInstance().getUnitService();

            unitService.delete(context, unit);
            context.restoreAuthSystemState();
        }
    }
}
