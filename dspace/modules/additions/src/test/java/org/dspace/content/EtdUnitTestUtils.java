package org.dspace.content;

import java.io.IOException;
import java.sql.SQLException;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.EtdUnitService;
import org.dspace.core.Context;

/**
 * Convenience methods for handling EtdUnits in tests.
 */
public class EtdUnitTestUtils {
    /**
     * Disable constructor (for Checkstyle)
     */
    private EtdUnitTestUtils() {
    }

    /**
     * Creates a EtdUnit with the given parameters, storing it in the database.
     *
     * @param context     the DSpace context
     * @param name        the name of the EtdUnit
     * @param facultyOnly true if the etdunit is faculty-only, false otherwise.
     * @return the newly create EtdUnit
     * @throws SQLException       if a database error occurs
     * @throws AuthorizeException if an authorization error occurs
     */
    public static EtdUnit createEtdUnit(Context context, String name, boolean facultyOnly)
            throws SQLException, AuthorizeException {
        EtdUnitService etdunitService = ContentServiceFactory.getInstance().getEtdUnitService();

        context.turnOffAuthorisationSystem();
        EtdUnit etdunit = etdunitService.create(context);
        etdunit.setName(name);
        etdunitService.update(context, etdunit);
        context.restoreAuthSystemState();
        return etdunit;
    }

    /**
     * Retrieves the EtdUnit with the given name from the database, or returns null
     * if a EtdUnit with the given name cannot be found.
     *
     * @param context     the DSpace context
     * @param etdunitName the name of the EtdUnit to retrieve
     * @return the EtdUnit with the given name from the database, or null if a
     *         EtdUnit with the given name is not found.
     * @throws SQLException if a database error occurs.
     */
    public static EtdUnit getEtdUnitFromDatabase(Context context, String etdunitName) throws SQLException {
        EtdUnitService etdunitService = ContentServiceFactory.getInstance().getEtdUnitService();

        return etdunitService.findByName(context, etdunitName);
    }

    /**
     * Deletes the given EtdUnit from the database.
     *
     * @param context the DSpace context
     * @param etdunit the EtdUnit to delete
     * @throws SQLException       if a database error occurs
     * @throws AuthorizeException if an authorization error occurs
     * @throws IOException        if an I/O error occurs.
     */
    public static void deleteEtdUnit(Context context, EtdUnit etdunit)
            throws SQLException, AuthorizeException, IOException {
        if (etdunit != null) {
            context.turnOffAuthorisationSystem();
            EtdUnitService etdunitService = ContentServiceFactory.getInstance().getEtdUnitService();

            etdunitService.delete(context, etdunit);
            context.restoreAuthSystemState();
        }
    }
}
