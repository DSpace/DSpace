/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.storage.rdbms;

import java.io.File;
import org.dspace.administer.MetadataImporter;
import org.dspace.administer.RegistryLoader;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.eperson.service.GroupService;
import org.dspace.services.KernelStartupCallbackService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

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
public class DatabaseRegistryUpdater implements KernelStartupCallbackService
{
     /** logging category */
    private static final Logger log = LoggerFactory.getLogger(DatabaseRegistryUpdater.class);

    @Autowired(required = true)
    protected GroupService groupService;

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
    public void executeCallback()
    {
        updateRegistries();
    }
}
