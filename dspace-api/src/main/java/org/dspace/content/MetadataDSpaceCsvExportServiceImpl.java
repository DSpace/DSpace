/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

import org.dspace.app.bulkedit.DSpaceCSV;
import org.dspace.app.bulkedit.StreamingDSpaceCsvExporter;
import org.dspace.app.util.service.DSpaceObjectUtils;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.MetadataDSpaceCsvExportService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.handle.factory.HandleServiceFactory;
import org.dspace.scripts.handler.DSpaceRunnableHandler;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of {@link MetadataDSpaceCsvExportService}
 */
public class MetadataDSpaceCsvExportServiceImpl implements MetadataDSpaceCsvExportService {

    @Autowired
    private ItemService itemService;

    @Autowired
    private DSpaceObjectUtils dSpaceObjectUtils;

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private StreamingDSpaceCsvExporter streamingExporter;

    private int csxExportLimit = -1;

    @Override
    public DSpaceCSV handleExport(Context context, boolean exportAllItems, boolean exportAllMetadata, String identifier,
                                  DSpaceRunnableHandler handler) throws Exception {
        Iterator<Item> toExport = null;

        if (exportAllItems) {
            handler.logInfo("Exporting whole repository WARNING: May take some time!");
            toExport = itemService.findAll(context, getCsvExportLimit(), 0);
        } else {
            DSpaceObject dso = HandleServiceFactory.getInstance().getHandleService()
                .resolveToObject(context, identifier);
            if (dso == null) {
                dso = dSpaceObjectUtils.findDSpaceObject(context, UUID.fromString(identifier));
            }
            if (dso == null) {
                throw new IllegalArgumentException(
                    "DSO '" + identifier + "' does not resolve to a DSpace Object in your repository!");
            }

            if (dso.getType() == Constants.ITEM) {
                handler.logInfo("Exporting item '" + dso.getName() + "' (" + identifier + ")");
                List<Item> item = new ArrayList<>();
                item.add((Item) dso);
                toExport = item.iterator();
            } else if (dso.getType() == Constants.COLLECTION) {
                handler.logInfo("Exporting collection '" + dso.getName() + "' (" + identifier + ")");
                Collection collection = (Collection) dso;
                toExport = itemService.findByCollection(context, collection, getCsvExportLimit(), 0);
            } else if (dso.getType() == Constants.COMMUNITY) {
                handler.logInfo("Exporting community '" + dso.getName() + "' (" + identifier + ")");
                toExport = buildFromCommunity(context, (Community) dso);
            } else {
                throw new IllegalArgumentException(
                    String.format("DSO with id '%s' (type: %s) can't be exported. Supported types: %s", identifier,
                        Constants.typeText[dso.getType()], "Item | Collection | Community"));
            }
        }

        DSpaceCSV csv = this.export(context, toExport, exportAllMetadata, handler);
        return csv;
    }

    @Override
    public DSpaceCSV export(Context context, Iterator<Item> toExport,
                            boolean exportAll, DSpaceRunnableHandler handler) throws Exception {
        Context.Mode originalMode = context.getCurrentMode();
        context.setMode(Context.Mode.READ_ONLY);

        // Process each item until we reach the limit
        int itemExportLimit = getCsvExportLimit();
        DSpaceCSV csv = new DSpaceCSV(exportAll);

        for (int itemsAdded = 0; toExport.hasNext() && itemsAdded < itemExportLimit; itemsAdded++) {
            Item item = toExport.next();
            csv.addItem(item);
            context.uncacheEntity(item);
        }

        context.setMode(originalMode);
        // Return the results
        return csv;
    }

    @Override
    public DSpaceCSV export(Context context, Community community,
                            boolean exportAll, DSpaceRunnableHandler handler) throws Exception {
        return export(context, buildFromCommunity(context, community), exportAll, handler);
    }

    /**
     * Build a Java Collection of item IDs that are in a Community (including
     * its sub-Communities and Collections)
     *
     * @param context   DSpace context
     * @param community The community to build from
     * @return Iterator over the Collection of item ids
     * @throws SQLException if database error
     */
    private Iterator<Item> buildFromCommunity(Context context, Community community)
        throws SQLException {
        Set<Item> result = new HashSet<>();

        // Add all the collections
        List<Collection> collections = community.getCollections();
        for (Collection collection : collections) {
            // Never obtain more items than the configured limit
            Iterator<Item> items = itemService.findByCollection(context, collection, getCsvExportLimit(), 0);
            while (result.size() < getCsvExportLimit() && items.hasNext()) {
                result.add(items.next());
            }
        }

        // Add all the sub-communities
        List<Community> communities = community.getSubcommunities();
        for (Community subCommunity : communities) {
            Iterator<Item> items = buildFromCommunity(context, subCommunity);
            while (result.size() < getCsvExportLimit() && items.hasNext()) {
                result.add(items.next());
            }
        }

        return result.iterator();
    }

    @Override
    public InputStream handleExportStreaming(Context context, boolean exportAllItems, boolean exportAllMetadata,
                                            String identifier,
                                            DSpaceRunnableHandler handler) throws Exception {
        Supplier<Iterator<Item>> supplier;

        if (exportAllItems) {
            handler.logInfo("Exporting whole repository WARNING: May take some time!");
            supplier = () -> {
                try {
                    return itemService.findAll(context, getCsvExportLimit(), 0);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            };
        } else {
            DSpaceObject dso = HandleServiceFactory.getInstance().getHandleService()
                .resolveToObject(context, identifier);
            if (dso == null) {
                dso = dSpaceObjectUtils.findDSpaceObject(context, UUID.fromString(identifier));
            }
            if (dso == null) {
                throw new IllegalArgumentException(
                    "DSO '" + identifier + "' does not resolve to a DSpace Object in your repository!");
            }

            if (dso.getType() == Constants.ITEM) {
                handler.logInfo("Exporting item '" + dso.getName() + "' (" + identifier + ")");
                List<Item> items = new ArrayList<>();
                items.add((Item) dso);
                supplier = items::iterator;
            } else if (dso.getType() == Constants.COLLECTION) {
                handler.logInfo("Exporting collection '" + dso.getName() + "' (" + identifier + ")");
                Collection collection = (Collection) dso;
                supplier = () -> {
                    try {
                        return itemService.findByCollection(context, collection, getCsvExportLimit(), 0);
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                };
            } else if (dso.getType() == Constants.COMMUNITY) {
                handler.logInfo("Exporting community '" + dso.getName() + "' (" + identifier + ")");
                Community community = (Community) dso;
                supplier = () -> {
                    try {
                        return buildFromCommunity(context, community);
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                };
            } else {
                throw new IllegalArgumentException(
                    String.format("DSO with id '%s' (type: %s) can't be exported. Supported types: %s", identifier,
                        Constants.typeText[dso.getType()], "Item | Collection | Community"));
            }
        }

        return exportStreaming(context, supplier, exportAllMetadata);
    }

    @Override
    public InputStream exportStreaming(Context context, Supplier<Iterator<Item>> itemIteratorSupplier,
                                       boolean exportAll) throws Exception {
        Context.Mode originalMode = context.getCurrentMode();
        context.setMode(Context.Mode.READ_ONLY);
        try {
            return streamingExporter.export(context, itemIteratorSupplier, exportAll, getCsvExportLimit());
        } finally {
            context.setMode(originalMode);
        }
    }

    @Override
    public int getCsvExportLimit() {
        if (csxExportLimit == -1) {
            csxExportLimit = configurationService.getIntProperty("bulkedit.export.max.items", 500);
        }
        return csxExportLimit;
    }
}
