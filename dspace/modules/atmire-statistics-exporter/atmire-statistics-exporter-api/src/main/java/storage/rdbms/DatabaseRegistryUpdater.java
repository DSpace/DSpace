/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package storage.rdbms;

import org.dspace.administer.MetadataImporter;
import org.dspace.administer.RegistryLoader;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.eperson.Group;
import org.dspace.storage.rdbms.DatabaseUtils;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.callback.FlywayCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.sql.Connection;

/**
 * This is a FlywayCallback class which automatically updates the
 * Metadata Schema Registry and Bitstream Formats Registries BEFORE
 * any Database migration occurs.
 * <P>
 * The reason this runs BEFORE a migration is to ensure that any new
 * metadata fields are FIRST added to our registries, so that the
 * migrations can make use of those new metadata fields, etc.
 * <P>
 * However, there is one exception. If this is a "fresh install" of DSpace,
 * we'll need to wait until the necessary database tables are created. In
 * that scenario we will load registries AFTER the initial migration.
 *
 * @author Tim Donohue
 */
public class DatabaseRegistryUpdater implements FlywayCallback
{
     /** logging category */
    private static final Logger log = LoggerFactory.getLogger(DatabaseRegistryUpdater.class);

    // Whether or not this is a fresh install of DSpace
    // This determines whether to update registries PRE or POST migration
    private boolean freshInstall = false;

    /**
     * Method to actually update our registries from latest configs
     */
    private void updateRegistries()
    {
        Context context = null;
        try
        {
            context = new Context();
            context.turnOffAuthorisationSystem();

            String base = ConfigurationManager.getProperty("dspace.dir")
                            + File.separator + "config" + File.separator
                            + "registries" + File.separator;

            // Load updates to Bitstream format registry (if any)
            log.info("Updating Bitstream Format Registry based on " + base + "bitstream-formats.xml");
            RegistryLoader.loadBitstreamFormats(context, base + "bitstream-formats.xml");

            // Load updates to Metadata schema registries (if any)
            log.info("Updating Metadata Registries based on metadata type configs in " + base);
            MetadataImporter.loadRegistry(base + "dublin-core-types.xml", true);
            MetadataImporter.loadRegistry(base + "dcterms-types.xml", true);
            MetadataImporter.loadRegistry(base + "eperson-types.xml", true);
            MetadataImporter.loadRegistry(base + "sword-metadata.xml", true);

            // Check if XML Workflow is enabled in workflow.cfg
            if (ConfigurationManager.getProperty("workflow", "workflow.framework").equals("xmlworkflow"))
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
        }
        finally
        {
            // Clean up our context, if it still exists & it was never completed
            if(context!=null && context.isValid())
                context.abort();
        }
    }


    @Override
    public void afterClean(Connection connection)
    {
        // do nothing
    }

    @Override
    public void afterEachMigrate(Connection connection, MigrationInfo info)
    {
        // do nothing
    }

    @Override
    public void afterInfo(Connection connection)
    {
        if (!freshInstall) {
            updateRegistries();
        }
    }

    @Override
    public void afterInit(Connection connection)
    {
        // do nothing
    }

    @Override
    public void afterMigrate(Connection connection)
    {
        // If this is a fresh install, we must update registries AFTER the
        // initial migrations (since the registry tables won't exist until the
        // initial migrations are performed)
        if(freshInstall)
        {
            updateRegistries();
            freshInstall = false;
        }

        // After every migrate, ensure default Groups are setup correctly.
        Context context = null;
        try
        {
            context = new Context();
            context.turnOffAuthorisationSystem();
            // While it's not really a formal "registry", we need to ensure the
            // default, required Groups exist in the DSpace database
            Group.initDefaultGroupNames(context);
            context.restoreAuthSystemState();
            // Commit changes and close context
            context.complete();
        }
        catch(Exception e)
        {
            log.error("Error attempting to add/update default DSpace Groups", e);
        }
        finally
        {
            // Clean up our context, if it still exists & it was never completed
            if(context!=null && context.isValid())
                context.abort();
        }
    }

    @Override
    public void afterRepair(Connection connection)
    {
        // do nothing
    }

    @Override
    public void afterValidate(Connection connection)
    {
        // do nothing
    }

    @Override
    public void beforeClean(Connection connection)
    {
        // do nothing
    }

    @Override
    public void beforeEachMigrate(Connection connection, MigrationInfo info)
    {
        // do nothing
    }

    @Override
    public void beforeInfo(Connection connection)
    {
        // do nothing
    }

    @Override
    public void beforeInit(Connection connection)
    {
        // do nothing
    }

    @Override
    public void beforeMigrate(Connection connection)
    {
        // Check if our MetadataSchemaRegistry table exists yet.
        // If it does NOT, then this is a fresh install & we'll need to
        // updateRegistries() AFTER migration
        if(DatabaseUtils.tableExists(connection, "MetadataSchemaRegistry"))
        {
            // Ensure registries are updated BEFORE a database migration (upgrade)
            // We need to ensure any new metadata fields are added before running
            // migrations, just in case the migrations need to utilize those new fields
            updateRegistries();
        }
        else
        {
            // this is a fresh install, need to migrate first in order to create
            // the registry tables.
            freshInstall = true;
        }
    }

    @Override
    public void beforeRepair(Connection connection)
    {
        // do nothing
    }

    @Override
    public void beforeValidate(Connection connection)
    {
        // do nothing
    }
}
