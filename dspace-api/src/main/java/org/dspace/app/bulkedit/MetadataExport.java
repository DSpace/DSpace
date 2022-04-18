/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.bulkedit;

import java.sql.SQLException;
import java.util.UUID;

import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.StringUtils;
import org.dspace.app.util.factory.UtilServiceFactory;
import org.dspace.app.util.service.DSpaceObjectUtils;
import org.dspace.content.DSpaceObject;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.MetadataDSpaceCsvExportService;
import org.dspace.core.Context;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;
import org.dspace.handle.factory.HandleServiceFactory;
import org.dspace.scripts.DSpaceRunnable;
import org.dspace.utils.DSpace;

/**
 * Metadata exporter to allow the batch export of metadata into a file
 *
 * @author Stuart Lewis
 */
public class MetadataExport extends DSpaceRunnable<MetadataExportScriptConfiguration> {

    private boolean help = false;
    private String filename = null;
    private String identifier = null;
    private boolean exportAllMetadata = false;
    private boolean exportAllItems = false;

<<<<<<< HEAD
    protected Context context;

    /** Whether to export all metadata, or just normally edited metadata */
    protected boolean exportAll;
=======
    private static final String EXPORT_CSV = "exportCSV";
>>>>>>> dspace-7.2.1

    private MetadataDSpaceCsvExportService metadataDSpaceCsvExportService = new DSpace().getServiceManager()
                .getServicesByType(MetadataDSpaceCsvExportService.class).get(0);

    private EPersonService ePersonService = EPersonServiceFactory.getInstance().getEPersonService();

<<<<<<< HEAD
        // Store the export settings
        this.toExport = toExport;
        this.exportAll = exportAll;
        this.context = c;
    }
=======
    private DSpaceObjectUtils dSpaceObjectUtils = UtilServiceFactory.getInstance().getDSpaceObjectUtils();
>>>>>>> dspace-7.2.1

    @Override
    public void internalRun() throws Exception {

<<<<<<< HEAD
        try
        {
            // Try to export the community
            this.toExport = buildFromCommunity(c, toExport, 0);
            this.exportAll = exportAll;
            this.context = c;
=======
        if (help) {
            logHelpInfo();
            printHelp();
            return;
>>>>>>> dspace-7.2.1
        }
        Context context = new Context();
        context.turnOffAuthorisationSystem();
        try {
            context.setCurrentUser(ePersonService.find(context, this.getEpersonIdentifier()));
        } catch (SQLException e) {
            handler.handleException(e);
        }
        DSpaceCSV dSpaceCSV = metadataDSpaceCsvExportService
            .handleExport(context, exportAllItems, exportAllMetadata, identifier,
                          handler);
        handler.writeFilestream(context, filename, dSpaceCSV.getInputStream(), EXPORT_CSV);
        context.restoreAuthSystemState();
        context.complete();
    }

<<<<<<< HEAD
    /**
     * Build an array list of item ids that are in a community (include sub-communities and collections)
     *
     * @param context DSpace context
     * @param community The community to build from
     * @param indent How many spaces to use when writing out the names of items added
     * @return The list of item ids
     * @throws SQLException if database error
     */
    protected Iterator<Item> buildFromCommunity(Context context, Community community, int indent)
                                                                               throws SQLException
    {
        // Add all the collections
        List<Collection> collections = community.getCollections();
        Iterator<Item> result = null;
        for (Collection collection : collections)
        {
            for (int i = 0; i < indent; i++)
            {
                System.out.print(" ");
            }

            Iterator<Item> items = itemService.findByCollection(context, collection);
            result = addItemsToResult(result,items);

        }
        // Add all the sub-communities
        List<Community> communities = community.getSubcommunities();
        for (Community subCommunity : communities)
        {
            for (int i = 0; i < indent; i++)
            {
                System.out.print(" ");
            }
            Iterator<Item> items = buildFromCommunity(context, subCommunity, indent + 1);
            result = addItemsToResult(result,items);
        }

        return result;
    }

    private Iterator<Item> addItemsToResult(Iterator<Item> result, Iterator<Item> items) {
        if(result == null)
        {
            result = items;
        }else{
            result = Iterators.concat(result, items);
        }

        return result;
    }

    /**
     * Run the export
     *
     * @return the exported CSV lines
     */
    public DSpaceCSV export()
    {
        try
        {
            Context.Mode originalMode = context.getCurrentMode();
            context.setMode(Context.Mode.READ_ONLY);

            // Process each item
            DSpaceCSV csv = new DSpaceCSV(exportAll);
            while (toExport.hasNext())
            {
                Item item = toExport.next();
                csv.addItem(item);
                context.uncacheEntity(item);
            }

            context.setMode(originalMode);
            // Return the results
            return csv;
        }
        catch (Exception e)
        {
            // Something went wrong...
            System.err.println("Error exporting to CSV:");
            e.printStackTrace();
            return null;
        }
=======
    protected void logHelpInfo() {
        handler.logInfo("\nfull export: metadata-export");
        handler.logInfo("partial export: metadata-export -i handle/UUID");
>>>>>>> dspace-7.2.1
    }

    @Override
    public MetadataExportScriptConfiguration getScriptConfiguration() {
        return new DSpace().getServiceManager().getServiceByName("metadata-export",
                                                                 MetadataExportScriptConfiguration.class);
    }

    @Override
    public void setup() throws ParseException {

        if (commandLine.hasOption('h')) {
            help = true;
            return;
        }

        if (!commandLine.hasOption('i')) {
            exportAllItems = true;
        }
        identifier = commandLine.getOptionValue('i');
        filename = getFileNameForExportFile();

<<<<<<< HEAD
        // Check a filename is given
        if (!line.hasOption('f'))
        {
            System.err.println("Required parameter -f missing!");
            printHelp(options, 1);
        }
        String filename = line.getOptionValue('f');

        // Create a context
        Context c = new Context(Context.Mode.READ_ONLY);
        c.turnOffAuthorisationSystem();

        // The things we'll export
        Iterator<Item> toExport = null;
        MetadataExport exporter = null;
=======
        exportAllMetadata = commandLine.hasOption('a');
>>>>>>> dspace-7.2.1

    }

    protected String getFileNameForExportFile() throws ParseException {
        Context context = new Context();
        try {
            DSpaceObject dso = null;
            if (StringUtils.isNotBlank(identifier)) {
                dso = HandleServiceFactory.getInstance().getHandleService().resolveToObject(context, identifier);
                if (dso == null) {
                    dso = dSpaceObjectUtils.findDSpaceObject(context, UUID.fromString(identifier));
                }
            } else {
                dso = ContentServiceFactory.getInstance().getSiteService().findSite(context);
            }
            if (dso == null) {
                throw new ParseException("An identifier was given that wasn't able to be parsed to a DSpaceObject");
            }
            return dso.getID().toString() + ".csv";
        } catch (SQLException e) {
            handler.handleException("Something went wrong trying to retrieve DSO for identifier: " + identifier, e);
        }
        return null;
    }
}
