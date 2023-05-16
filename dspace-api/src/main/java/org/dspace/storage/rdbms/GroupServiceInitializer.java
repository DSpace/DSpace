/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.storage.rdbms;

import org.apache.logging.log4j.Logger;
import org.dspace.core.Context;
import org.dspace.eperson.service.GroupService;
import org.flywaydb.core.api.callback.Callback;
import org.flywaydb.core.api.callback.Event;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Callback method to ensure that the default groups are created in the database
 * AFTER the database migration completes.
 *
 * @author kevinvandevelde at atmire.com
 */
public class GroupServiceInitializer implements Callback {

    private final Logger log = org.apache.logging.log4j.LogManager.getLogger(GroupServiceInitializer.class);

    @Autowired(required = true)
    protected GroupService groupService;

    public void initGroups() {
        // After every migrate, ensure default Groups are setup correctly.
        Context context = null;
        try {
            context = new Context();
            context.turnOffAuthorisationSystem();
            // While it's not really a formal "registry", we need to ensure the
            // default, required Groups exist in the DSpace database
            groupService.initDefaultGroupNames(context);
            context.restoreAuthSystemState();
            // Commit changes and close context
            context.complete();
        } catch (Exception e) {
            log.error("Error attempting to add/update default DSpace Groups", e);
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
        return GroupServiceInitializer.class.getSimpleName();
    }

    /**
     * Events supported by this callback.
     * @param event Flyway event
     * @param context Flyway context
     * @return true if AFTER_MIGRATE event
     */
    @Override
    public boolean supports(Event event, org.flywaydb.core.api.callback.Context context) {
        // Must run AFTER all migrations complete, since it is dependent on Hibernate
        return event.equals(Event.AFTER_MIGRATE);
    }

    /**
     * Whether event can be handled in a transaction or whether it must be handle outside of transaction.
     * @param event Flyway event
     * @param context Flyway context
     * @return true
     */
    @Override
    public boolean canHandleInTransaction(Event event, org.flywaydb.core.api.callback.Context context) {
        return true;
    }

    /**
     * What to run when the callback is triggered.
     * @param event Flyway event
     * @param context Flyway context
     */
    @Override
    public void handle(Event event, org.flywaydb.core.api.callback.Context context) {
        initGroups();
    }
}
