/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.storage.rdbms;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;

import org.dspace.administer.MetadataImporter;
import org.dspace.administer.RegistryImportException;
import org.dspace.administer.RegistryLoader;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.NonUniqueMetadataException;
import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.flywaydb.core.api.callback.Callback;
import org.flywaydb.core.api.callback.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

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
public class RegistryUpdater implements Callback {
    /**
     * logging category
     */
    private static final Logger log = LoggerFactory.getLogger(RegistryUpdater.class);

    /**
     * Method to actually update our registries from latest configuration files.
     */
    private void updateRegistries() {
        ConfigurationService config = DSpaceServicesFactory.getInstance().getConfigurationService();
        Context context = null;
        try {
            context = new Context();
            context.turnOffAuthorisationSystem();

            String base = config.getProperty("dspace.dir")
                + File.separator + "config" + File.separator
                + "registries" + File.separator;

            // Load updates to Bitstream format registry (if any)
            log.info("Updating Bitstream Format Registry based on {}bitstream-formats.xml", base);
            RegistryLoader.loadBitstreamFormats(context, base + "bitstream-formats.xml");

            // Load updates to Metadata schema registries (if any)
            log.info("Updating Metadata Registries based on metadata type configs in {}", base);
            for (String namespaceFile: config.getArrayProperty("registry.metadata.load")) {
                log.info("Reading {}", namespaceFile);
                MetadataImporter.loadRegistry(base + namespaceFile, true);
            }

            String workflowTypes = "workflow-types.xml";
            log.info("Reading {}", workflowTypes);
            MetadataImporter.loadRegistry(base + workflowTypes, true);

            context.restoreAuthSystemState();
            // Commit changes and close context
            context.complete();
            log.info("All Bitstream Format Regitry and Metadata Registry updates were completed.");
        } catch (IOException | SQLException | ParserConfigurationException
                | TransformerException | RegistryImportException
                | AuthorizeException | NonUniqueMetadataException
                | SAXException | XPathExpressionException e) {
            log.error("Error attempting to update Bitstream Format and/or Metadata Registries", e);
            throw new RuntimeException("Error attempting to update Bitstream Format and/or Metadata Registries", e);
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
        return RegistryUpdater.class.getSimpleName();
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
        // Always return true, as our handle() method is updating the database.
        return true;
    }

    /**
     * What to run when the callback is triggered.
     * @param event Flyway event
     * @param context Flyway context
     */
    @Override
    public void handle(Event event, org.flywaydb.core.api.callback.Context context) {
        updateRegistries();
    }
}
