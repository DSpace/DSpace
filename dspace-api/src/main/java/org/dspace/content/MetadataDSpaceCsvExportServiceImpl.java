/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.dspace.app.bulkedit.DSpaceCSV;
import org.dspace.app.util.service.DSpaceObjectUtils;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.MetadataDSpaceCsvExportService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.handle.factory.HandleServiceFactory;
import org.dspace.scripts.handler.DSpaceRunnableHandler;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of {@link MetadataDSpaceCsvExportService}
 */
public class MetadataDSpaceCsvExportServiceImpl implements MetadataDSpaceCsvExportService {

    @Autowired
    private ItemService itemService;

    @Autowired
    private DSpaceObjectUtils dSpaceObjectUtils;

    @Override
    public DSpaceCSV handleExport(Context context, boolean exportAllItems, boolean exportAllMetadata, String identifier,
                                  DSpaceRunnableHandler handler) throws Exception {
        Iterator<Item> toExport = null;

        if (exportAllItems) {
            handler.logInfo("Exporting whole repository WARNING: May take some time!");
            toExport = itemService.findAll(context);
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
                toExport = itemService.findByCollection(context, collection);
            } else if (dso.getType() == Constants.COMMUNITY) {
                handler.logInfo("Exporting community '" + dso.getName() + "' (" + identifier + ")");
                toExport = buildFromCommunity(context, (Community) dso);
            } else {
                throw new IllegalArgumentException(
                    String.format("DSO with id '%s' (type: %s) can't be exported. Supported types: %s", identifier,
                        Constants.typeText[dso.getType()], "Item | Collection | Community"));
            }
        }

        DSpaceCSV csv = this.export(context, toExport, exportAllMetadata);
        return csv;
    }

    @Override
    public DSpaceCSV export(Context context, Iterator<Item> toExport, boolean exportAll) throws Exception {
        Context.Mode originalMode = context.getCurrentMode();
        context.setMode(Context.Mode.READ_ONLY);

        // Process each item
        DSpaceCSV csv = new DSpaceCSV(exportAll);
        while (toExport.hasNext()) {
            Item item = toExport.next();
            csv.addItem(item);
            context.uncacheEntity(item);
        }

        context.setMode(originalMode);
        // Return the results
        return csv;
    }

    @Override
    public DSpaceCSV export(Context context, Community community, boolean exportAll) throws Exception {
        return export(context, buildFromCommunity(context, community), exportAll);
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
            Iterator<Item> items = itemService.findByCollection(context, collection);
            while (items.hasNext()) {
                result.add(items.next());
            }
        }

    // Add all the sub-communities
        List<Community> communities = community.getSubcommunities();
        for (Community subCommunity : communities) {
            Iterator<Item> items = buildFromCommunity(context, subCommunity);
            while (items.hasNext()) {
                result.add(items.next());
            }
        }

        return result.iterator();
    }
}
