/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.storage.rdbms;

import java.io.File;
import java.sql.Connection;

import org.dspace.administer.MetadataImporter;
import org.dspace.administer.RegistryLoader;
import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.workflow.factory.WorkflowServiceFactory;
import org.dspace.xmlworkflow.service.XmlWorkflowService;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.callback.FlywayCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a FlywayCallback class which automatically updates the
 * Metadata Schema Registry and Bitstream Formats Registries AFTER
 * all Database migrations occur.
 * <P>
 * The reason this runs AFTER all migrations is that the RegistryLoader
 * and MetadataImporter now depend on Hibernate and Hibernate cannot be
 * initialized until the Database is fully migrated.
 * <P>
 * If a migration needs to use on one or more registry values, there are
 * two options:
 * <UL>
 * <LI>Create/insert those registry values in the migration itself (via SQL or similar).</LI>
 * <LI>Alternatively, first check for the existence of the MetadataSchemaRegistry (or similar)
 * before running the migration logic. If the table or fields do not yet exist, you might be
 * able to skip the migration logic entirely. See "DatabaseUtils.tableExists()" and similar methods.</LI>
 * </UL>
 *
 * @author Tim Donohue
 */
public class DatabaseRegistryUpdater implements FlywayCallback
{
     /** logging category */
    private static final Logger log = LoggerFactory.getLogger(DatabaseRegistryUpdater.class);

    /**
     * Method to actually update our registries from latest configs
     */
    private void updateRegistries()
    {
        ConfigurationService config = DSpaceServicesFactory.getInstance().getConfigurationService();
        Context context = null;
        try
        {
            context = new Context();
            context.turnOffAuthorisationSystem();

            String base = config.getProperty("dspace.dir")
                            + File.separator + "config" + File.separator
                            + "registries" + File.separator;

            // Load updates to Bitstream format registry (if any)
            log.info("Updating Bitstream Format Registry based on " + base + "bitstream-formats.xml");
            RegistryLoader.loadBitstreamFormats(context, base + "bitstream-formats.xml");

            // Load updates to Metadata schema registries (if any)
            log.info("Updating Metadata Registries based on metadata type configs in " + base);
            MetadataImporter.loadRegistry(base + "dublin-core-types.xml", true);
            MetadataImporter.loadRegistry(base + "dcterms-types.xml", true);
            MetadataImporter.loadRegistry(base + "local-types.xml", true);
            MetadataImporter.loadRegistry(base + "eperson-types.xml", true);
            MetadataImporter.loadRegistry(base + "sword-metadata.xml", true);

            // Check if XML Workflow is enabled in workflow.cfg
            if (WorkflowServiceFactory.getInstance().getWorkflowService() instanceof XmlWorkflowService)
            {
                // If so, load in the workflow metadata types as well
                MetadataImporter.loadRegistry(base + "workflow-types.xml", true);
            }

            context.restoreAuthSystemState();
            // Commit changes and close context
            context.complete();
            log.info("All Bitstream Format Regitry and Metadata Registry updates were completed.");
        }
        catch(Exception e)
        {
            log.error("Error attempting to update Bitstream Format and/or Metadata Registries", e);
            throw new RuntimeException("Error attempting to update Bitstream Format and/or Metadata Registries", e);
        }
        finally
        {
            // Clean up our context, if it still exists & it was never completed
            if(context!=null && context.isValid())
                context.abort();
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
        // Must run AFTER all migrations complete, since it is dependent on Hibernate
        updateRegistries();
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
