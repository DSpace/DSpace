/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.bulkedit;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.Logger;
import org.dspace.app.bulkedit.service.BulkEditImportService;
import org.dspace.app.bulkedit.service.CSVBulkEditCacheService;
import org.dspace.app.bulkedit.service.BulkEditRegisterService;
import org.dspace.app.bulkedit.service.BulkEditServiceFactory;
import org.dspace.authority.factory.AuthorityServiceFactory;
import org.dspace.authority.service.AuthorityValueService;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.EntityService;
import org.dspace.content.service.EntityTypeService;
import org.dspace.content.service.InstallItemService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.RelationshipService;
import org.dspace.content.service.RelationshipTypeService;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.core.Context;
import org.dspace.core.LogHelper;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.handle.factory.HandleServiceFactory;
import org.dspace.handle.service.HandleService;
import org.dspace.scripts.DSpaceRunnable;
import org.dspace.scripts.handler.DSpaceRunnableHandler;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.utils.DSpace;

/**
 * Metadata importer to allow the batch import of metadata from a file
 *
 * @author Stuart Lewis
 */
public class MetadataImport extends DSpaceRunnable<MetadataImportScriptConfiguration> {

    /**
     * The DSpaceCSV object we're processing
     */
    DSpaceCSV csv;

    private boolean useTemplate = false;
    private String filename = null;
    private boolean useWorkflow = false;
    private boolean workflowNotify = false;
    private boolean change = false;
    private boolean help = false;
    protected boolean validateOnly;

    /**
     * Logger
     */
    protected static final Logger log = org.apache.logging.log4j.LogManager.getLogger(MetadataImport.class);

    protected ItemService itemService = ContentServiceFactory.getInstance().getItemService();
    protected InstallItemService installItemService = ContentServiceFactory.getInstance().getInstallItemService();
    protected CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();
    protected HandleService handleService = HandleServiceFactory.getInstance().getHandleService();
    protected WorkspaceItemService workspaceItemService = ContentServiceFactory.getInstance().getWorkspaceItemService();
    protected RelationshipTypeService relationshipTypeService = ContentServiceFactory.getInstance()
                                                                                     .getRelationshipTypeService();
    protected RelationshipService relationshipService = ContentServiceFactory.getInstance().getRelationshipService();
    protected EntityTypeService entityTypeService = ContentServiceFactory.getInstance().getEntityTypeService();
    protected EntityService entityService = ContentServiceFactory.getInstance().getEntityService();
    protected AuthorityValueService authorityValueService = AuthorityServiceFactory.getInstance()
                                                                                   .getAuthorityValueService();
    protected ConfigurationService configurationService
            = DSpaceServicesFactory.getInstance().getConfigurationService();

    protected BulkEditRegisterService<DSpaceCSV> bulkEditRegisterService =
        BulkEditServiceFactory.getInstance().getCSVBulkEditRegisterService();
    protected CSVBulkEditCacheService bulkEditCacheService =
        BulkEditServiceFactory.getInstance().getCSVBulkEditCacheService();
    protected BulkEditImportService bulkEditImportService =
        BulkEditServiceFactory.getInstance().getBulkEditImportService();

    @Override
    public void internalRun() throws Exception {
        if (help) {
            printHelp();
            return;
        }
        // Create a context
        Context c = null;
        c = new Context();
        c.turnOffAuthorisationSystem();

        // Find the EPerson, assign to context
        assignCurrentUserInContext(c);

        // Read commandLines from the CSV file
        try {

            Optional<InputStream> optionalFileStream = handler.getFileStream(c, filename);
            if (optionalFileStream.isPresent()) {
                csv = new DSpaceCSV(optionalFileStream.get(), c);
            } else {
                throw new IllegalArgumentException("Error reading file, the file couldn't be found for filename: " +
                                                       filename);
            }
        } catch (MetadataImportInvalidHeadingException miihe) {
            throw miihe;
        } catch (Exception e) {
            throw new Exception("Error reading file: " + e.getMessage(), e);
        }

        boolean testRun = !commandLine.hasOption('s') || validateOnly;

        // Register the changes - just highlight differences
        c.setMode(Context.Mode.READ_ONLY);
        bulkEditCacheService.resetCache();
        List<BulkEditChange> changes = bulkEditRegisterService.registerBulkEditChange(c, csv);

        // Display the changes
        int changeCounter = displayChanges(changes, false);

        // If there were changes, ask if we should execute them
        if (testRun && !validateOnly && changeCounter > 0) {
            try {
                // Ask the user if they want to make the changes
                handler.logInfo("\n" + changeCounter + " item(s) will be changed\n");
                change = determineChange(handler);

            } catch (IOException ioe) {
                throw new IOException("Error: " + ioe.getMessage() + ", No changes have been made", ioe);
            }
        } else {
            handler.logInfo("There were no changes detected");
        }

        try {
            // If required, make the change
            if (change && !validateOnly) {
                c.setMode(Context.Mode.BATCH_EDIT);
                int i = 1;
                int batchSize = configurationService.getIntProperty("bulkedit.change.commit.count", 100);
                for (BulkEditChange bechange : changes) {
                    bulkEditImportService.importBulkEditChange(c, bechange, useTemplate, useWorkflow, workflowNotify);

                    if (i % batchSize == 0) {
                        c.commit();
                        handler.logInfo(LogHelper.getHeader(c, "metadata_import_commit", "lineNumber=" + i));
                    }

                    i++;
                }
                c.commit();

                // Display the changes
                displayChanges(changes, true);
            }

            // Finsh off and tidy up
            c.restoreAuthSystemState();
            c.complete();
        } catch (Exception e) {
            c.abort();
            throw new Exception(
                "Error committing changes to database: " + e.getMessage() + ", aborting most recent changes", e);
        }

    }

    protected void assignCurrentUserInContext(Context context) throws ParseException {
        UUID uuid = getEpersonIdentifier();
        if (uuid != null) {
            try {
                EPerson ePerson = EPersonServiceFactory.getInstance().getEPersonService().find(context, uuid);
                context.setCurrentUser(ePerson);
            } catch (SQLException e) {
                log.error("Something went wrong trying to fetch the eperson for uuid: " + uuid, e);
            }
        }
    }

    /**
     * This method determines whether the changes should be applied or not. This is default set to true for the REST
     * script as we don't want to interact with the caller. This will be overwritten in the CLI script to ask for
     * confirmation
     * @param handler   Applicable DSpaceRunnableHandler
     * @return boolean indicating the value
     * @throws IOException  If something goes wrong
     */
    protected boolean determineChange(DSpaceRunnableHandler handler) throws IOException {
        return true;
    }

    @Override
    public MetadataImportScriptConfiguration getScriptConfiguration() {
        return new DSpace().getServiceManager().getServiceByName("metadata-import",
                                                                 MetadataImportScriptConfiguration.class);
    }


    public void setup() throws ParseException {
        useTemplate = false;
        filename = null;
        useWorkflow = false;
        workflowNotify = false;

        if (commandLine.hasOption('h')) {
            help = true;
            return;
        }

        // Check a filename is given
        if (!commandLine.hasOption('f')) {
            throw new ParseException("Required parameter -f missing!");
        }
        filename = commandLine.getOptionValue('f');

        // Option to apply template to new items
        if (commandLine.hasOption('t')) {
            useTemplate = true;
        }

        // Options for workflows, and workflow notifications for new items
        if (commandLine.hasOption('w')) {
            useWorkflow = true;
            if (commandLine.hasOption('n')) {
                workflowNotify = true;
            }
        } else if (commandLine.hasOption('n')) {
            throw new ParseException(
                "Invalid option 'n': (notify) can only be specified with the 'w' (workflow) option.");
        }
        validateOnly = commandLine.hasOption('v');

        // Is this a silent run?
        change = false;
    }

    /**
     * Display the changes that have been detected, or that have been made
     *
     * @param changes The changes detected
     * @param changed Whether or not the changes have been made
     * @return The number of items that have changed
     */
    private int displayChanges(List<BulkEditChange> changes, boolean changed) {
        // Display the changes
        int changeCounter = 0;
        for (BulkEditChange change : changes) {
            // Get the changes
            List<BulkEditMetadataValue> adds = change.getAdds();
            List<BulkEditMetadataValue> removes = change.getRemoves();
            List<Collection> newCollections = change.getNewMappedCollections();
            List<Collection> oldCollections = change.getOldMappedCollections();
            if ((adds.size() > 0) || (removes.size() > 0) ||
                (newCollections.size() > 0) || (oldCollections.size() > 0) ||
                (change.getNewOwningCollection() != null) || (change.getOldOwningCollection() != null) ||
                (change.isDeleted()) || (change.isWithdrawn()) || (change.isReinstated())) {
                // Show the item
                Item i = change.getItem();
                handler.logInfo("-----------------------------------------------------------");
                if (!change.isNewItem()) {
                    handler.logInfo("Changes for item: " + i.getID() + " (" + i.getHandle() + ")");
                } else {
                    handler.logInfo("New item: ");
                    if (i != null) {
                        if (i.getHandle() != null) {
                            handler.logInfo(i.getID() + " (" + i.getHandle() + ")");
                        } else {
                            handler.logInfo(i.getID() + " (in workflow)");
                        }
                    }
                }
                changeCounter++;
            }

            // Show actions
            if (change.isDeleted()) {
                if (changed) {
                    handler.logInfo(" - EXPUNGED!");
                } else {
                    handler.logInfo(" - EXPUNGE!");
                }
            }
            if (change.isWithdrawn()) {
                if (changed) {
                    handler.logInfo(" - WITHDRAWN!");
                } else {
                    handler.logInfo(" - WITHDRAW!");
                }
            }
            if (change.isReinstated()) {
                if (changed) {
                    handler.logInfo(" - REINSTATED!");
                } else {
                    handler.logInfo(" - REINSTATE!");
                }
            }

            if (change.getNewOwningCollection() != null) {
                Collection c = change.getNewOwningCollection();
                if (c != null) {
                    String cHandle = c.getHandle();
                    String cName = c.getName();
                    if (!changed) {
                        handler.logInfo(" + New owning collection (" + cHandle + "): ");
                    } else {
                        handler.logInfo(" + New owning collection  (" + cHandle + "): ");
                    }
                    handler.logInfo(cName);
                }

                c = change.getOldOwningCollection();
                if (c != null) {
                    String cHandle = c.getHandle();
                    String cName = c.getName();
                    if (!changed) {
                        handler.logInfo(" + Old owning collection (" + cHandle + "): ");
                    } else {
                        handler.logInfo(" + Old owning collection  (" + cHandle + "): ");
                    }
                    handler.logInfo(cName);
                }
            }

            // Show new mapped collections
            for (Collection c : newCollections) {
                String cHandle = c.getHandle();
                String cName = c.getName();
                if (!changed) {
                    handler.logInfo(" + Map to collection (" + cHandle + "): ");
                } else {
                    handler.logInfo(" + Mapped to collection  (" + cHandle + "): ");
                }
                handler.logInfo(cName);
            }

            // Show old mapped collections
            for (Collection c : oldCollections) {
                String cHandle = c.getHandle();
                String cName = c.getName();
                if (!changed) {
                    handler.logInfo(" + Un-map from collection (" + cHandle + "): ");
                } else {
                    handler.logInfo(" + Un-mapped from collection  (" + cHandle + "): ");
                }
                handler.logInfo(cName);
            }

            // Show additions
            for (BulkEditMetadataValue metadataValue : adds) {
                String md = metadataValue.getSchema() + "." + metadataValue.getElement();
                if (metadataValue.getQualifier() != null) {
                    md += "." + metadataValue.getQualifier();
                }
                if (metadataValue.getLanguage() != null) {
                    md += "[" + metadataValue.getLanguage() + "]";
                }
                if (!changed) {
                    handler.logInfo(" + Add    (" + md + "): ");
                } else {
                    handler.logInfo(" + Added   (" + md + "): ");
                }
                handler.logInfo(metadataValue.getValue());
                if (bulkEditCacheService.isAuthorityControlledField(md)) {
                    handler.logInfo(", authority = " + metadataValue.getAuthority());
                    handler.logInfo(", confidence = " + metadataValue.getConfidence());
                }
            }

            // Show removals
            for (BulkEditMetadataValue metadataValue : removes) {
                String md = metadataValue.getSchema() + "." + metadataValue.getElement();
                if (metadataValue.getQualifier() != null) {
                    md += "." + metadataValue.getQualifier();
                }
                if (metadataValue.getLanguage() != null) {
                    md += "[" + metadataValue.getLanguage() + "]";
                }
                if (!changed) {
                    handler.logInfo(" - Remove (" + md + "): ");
                } else {
                    handler.logInfo(" - Removed (" + md + "): ");
                }
                handler.logInfo(metadataValue.getValue());
                if (bulkEditCacheService.isAuthorityControlledField(md)) {
                    handler.logInfo(", authority = " + metadataValue.getAuthority());
                    handler.logInfo(", confidence = " + metadataValue.getConfidence());
                }
            }
        }
        return changeCounter;
    }

}
