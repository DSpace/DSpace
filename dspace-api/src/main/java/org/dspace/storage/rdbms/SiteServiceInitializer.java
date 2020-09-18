/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.storage.rdbms;

import java.sql.Connection;

import org.apache.logging.log4j.Logger;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Site;
import org.dspace.content.service.SiteService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.Group;
import org.dspace.eperson.service.GroupService;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.callback.FlywayCallback;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Callback method to ensure that the Site object is created (if no site exists)
 * after the database migration completes.
 *
 * @author kevinvandevelde at atmire.com
 */
public class SiteServiceInitializer implements FlywayCallback {

    private Logger log = org.apache.logging.log4j.LogManager.getLogger(SiteServiceInitializer.class);

    @Autowired(required = true)
    protected SiteService siteService;

    @Autowired
    private AuthorizeService authorizeService;

    @Autowired
    private GroupService groupService;

    public void initializeSiteObject() {
        // After every migrate, ensure default Site is setup correctly.
        Context context = null;
        try {
            context = new Context();
            context.turnOffAuthorisationSystem();
            // While it's not really a formal "registry", we need to ensure the
            // default, required Groups exist in the DSpace database
            Site site = null;
            if (siteService.findSite(context) == null) {
                site = siteService.createSite(context);
            }
            context.restoreAuthSystemState();
            if (!authorizeService.authorizeActionBoolean(context, site, Constants.READ)) {
                context.turnOffAuthorisationSystem();
                Group anonGroup = groupService.findByName(context, Group.ANONYMOUS);
                if (anonGroup != null) {
                    authorizeService.addPolicy(context, site, Constants.READ, anonGroup);
                }
                context.restoreAuthSystemState();
            }
            // Commit changes and close context
            context.complete();
        } catch (Exception e) {
            log.error("Error attempting to add/update default DSpace Groups", e);
        } finally {
            // Clean up our context, if it still exists & it was never completed
            if (context != null && context.isValid()) {
                context.abort();
            }
        }


    }

    @Override
    public void beforeClean(Connection connection) {

    }

    @Override
    public void afterClean(Connection connection) {

    }

    @Override
    public void beforeMigrate(Connection connection) {

    }

    @Override
    public void afterMigrate(Connection connection) {
        initializeSiteObject();
    }

    @Override
    public void beforeEachMigrate(Connection connection, MigrationInfo migrationInfo) {

    }

    @Override
    public void afterEachMigrate(Connection connection, MigrationInfo migrationInfo) {

    }

    @Override
    public void beforeValidate(Connection connection) {

    }

    @Override
    public void afterValidate(Connection connection) {

    }

    @Override
    public void beforeBaseline(Connection connection) {

    }

    @Override
    public void afterBaseline(Connection connection) {

    }

    @Override
    public void beforeRepair(Connection connection) {

    }

    @Override
    public void afterRepair(Connection connection) {

    }

    @Override
    public void beforeInfo(Connection connection) {

    }

    @Override
    public void afterInfo(Connection connection) {

    }
}
