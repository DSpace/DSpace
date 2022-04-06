/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.storage.rdbms;
import org.apache.logging.log4j.Logger;
import org.dspace.content.service.EntityTypeService;
import org.dspace.core.Context;
import org.flywaydb.core.api.callback.Callback;
import org.flywaydb.core.api.callback.Event;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Callback method to ensure that the default EntityTypes are created in the database
 * AFTER the database migration completes.
 * 
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
public class EntityTypeServiceInitializer implements Callback {

    private final Logger log = org.apache.logging.log4j.LogManager.getLogger(EntityTypeServiceInitializer.class);

    @Autowired(required = true)
    private EntityTypeService entityTypeService;

    private void initEntityTypes() {
        // After every migrate, ensure default EntityTypes are setup correctly.
        Context context = null;
        try {
            context = new Context();
            context.turnOffAuthorisationSystem();
            // While it's not really a formal "registry", we need to ensure the
            // default, required EntityTypes exist in the DSpace database
            entityTypeService.initDefaultEntityTypeNames(context);
            context.restoreAuthSystemState();
            // Commit changes and close context
            context.complete();
        } catch (Exception e) {
            log.error("Error attempting to add/update default DSpace EntityTypes", e);
            throw new RuntimeException(e);
        } finally {
            // Clean up our context, if it still exists & it was never completed
            if (context != null && context.isValid()) {
                context.abort();
            }
        }
    }

    /**
     * The callback name, Flyway will use this to sort the callbacks alphabetically before executing them
     * @return The callback name
     */
    @Override
    public String getCallbackName() {
        // Return class name only (not prepended by package)
        return EntityTypeServiceInitializer.class.getSimpleName();
    }

    @Override
    public boolean supports(Event event, org.flywaydb.core.api.callback.Context context) {
        // Must run AFTER all migrations complete, since it is dependent on Hibernate
        return event.equals(Event.AFTER_MIGRATE);
    }

    @Override
    public boolean canHandleInTransaction(Event event, org.flywaydb.core.api.callback.Context context) {
        return true;
    }

    @Override
    public void handle(Event event, org.flywaydb.core.api.callback.Context context) {
        initEntityTypes();
    }

}