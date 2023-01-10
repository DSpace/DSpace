package org.dspace.builder;

import java.io.IOException;
import java.sql.SQLException;
import java.util.UUID;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.EtdUnit;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.DSpaceObjectService;
import org.dspace.content.service.EtdUnitService;
import org.dspace.core.Context;

/**
 * Builder to construct EtdUnit objects
 */
public class EtdUnitBuilder extends AbstractDSpaceObjectBuilder<EtdUnit> {
    /**
     * The etdunit being built
     */
    private EtdUnit etdunit;

    /**
     * The EtdUnitService instance
     */
    private static final EtdUnitService etdunitService = ContentServiceFactory.getInstance().getEtdUnitService();

    /**
     * Constructs a EtdUnitBuilder with the given Context
     *
     * @param context the DSpace context
     */
    protected EtdUnitBuilder(Context context) {
        super(context);
    }

    @Override
    public void cleanup() throws Exception {
        try (Context c = new Context()) {
            c.turnOffAuthorisationSystem();
            // Ensure object and any related objects are reloaded before checking to see
            // what needs cleanup
            etdunit = c.reloadEntity(etdunit);
            if (etdunit != null) {
                delete(c, etdunit);
                c.complete();
            }
        }
    }

    /**
     * Returns a EtdUnitBuilder initialized with the given Context
     *
     * @param context the DSpace context
     * @return a EtdUnitBuilder with the given context
     */
    public static EtdUnitBuilder createEtdUnit(final Context context) {
        EtdUnitBuilder builder = new EtdUnitBuilder(context);
        return builder.create(context);
    }

    /**
     * Initializes a new EtdUnitBuilder instance with the given context
     *
     * @param context the DSpace context
     * @return a EtdUnitBuilder with the given context
     */
    private EtdUnitBuilder create(final Context context) {
        this.context = context;
        try {
            etdunit = etdunitService.create(context);
        } catch (SQLException | AuthorizeException e) {
            return handleException(e);
        }
        return this;
    }

    @Override
    protected DSpaceObjectService<EtdUnit> getService() {
        return etdunitService;
    }

    @Override
    public EtdUnit build() {
        try {
            etdunitService.update(context, etdunit);
        } catch (SQLException | AuthorizeException e) {
            return handleException(e);
        }
        return etdunit;
    }

    /**
     * Sets the name of the EtdUnit being built
     *
     * @param etdunitName the name of the etdunit
     * @return the EtdUnitBuilder instance
     */
    public EtdUnitBuilder withName(String etdunitName) {
        try {
            etdunit.setName(etdunitName);
        } catch (Exception e) {
            return handleException(e);
        }
        return this;
    }

    /**
     * Sets the given Collection to the EtdUnit being built
     *
     * @param collection the Collection to add to the etdunit
     * @return the EtdUnitBuilder instance
     */
    public EtdUnitBuilder addCollection(Collection collection) {
        try {
            etdunitService.addCollection(context, etdunit, collection);
        } catch (SQLException | AuthorizeException e) {
            return handleException(e);
        }
        return this;
    }

    /**
     * Deletes the EtdUnit with the given UUID from the database.
     *
     * @param uuid the UUID of the etdunit to delete.
     */
    public static void deleteEtdUnit(UUID uuid) throws SQLException, IOException {
        try (Context c = new Context()) {
            c.turnOffAuthorisationSystem();
            EtdUnit etdunit = etdunitService.find(c, uuid);
            if (etdunit != null) {
                try {
                    etdunitService.delete(c, etdunit);
                } catch (AuthorizeException e) {
                    // cannot occur, just wrap it to make the compiler happy
                    throw new RuntimeException(e.getMessage(), e);
                }
            }
            c.complete();
        }
    }
}
