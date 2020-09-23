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
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.google.common.collect.Iterators;
import org.dspace.app.bulkedit.DSpaceCSV;
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

    @Override
    public DSpaceCSV handleExport(Context context, boolean exportAllItems, boolean exportAllMetadata, String handle,
                                  DSpaceRunnableHandler handler) throws Exception {
        Iterator<Item> toExport = null;

        if (exportAllItems) {
            handler.logInfo("Exporting whole repository WARNING: May take some time!");
            toExport = itemService.findAll(context);
        } else {
            DSpaceObject dso = HandleServiceFactory.getInstance().getHandleService().resolveToObject(context, handle);
            if (dso == null) {
                throw new IllegalArgumentException(
                    "Item '" + handle + "' does not resolve to an item in your repository!");
            }

            if (dso.getType() == Constants.ITEM) {
                handler.logInfo("Exporting item '" + dso.getName() + "' (" + handle + ")");
                List<Item> item = new ArrayList<>();
                item.add((Item) dso);
                toExport = item.iterator();
            } else if (dso.getType() == Constants.COLLECTION) {
                handler.logInfo("Exporting collection '" + dso.getName() + "' (" + handle + ")");
                Collection collection = (Collection) dso;
                toExport = itemService.findByCollection(context, collection);
            } else if (dso.getType() == Constants.COMMUNITY) {
                handler.logInfo("Exporting community '" + dso.getName() + "' (" + handle + ")");
                toExport = buildFromCommunity(context, (Community) dso);
            } else {
                throw new IllegalArgumentException("Error identifying '" + handle + "'");
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
     * Build an array list of item ids that are in a community (include sub-communities and collections)
     *
     * @param context   DSpace context
     * @param community The community to build from
     * @return The list of item ids
     * @throws SQLException if database error
     */
    private Iterator<Item> buildFromCommunity(Context context, Community community)
        throws SQLException {
        // Add all the collections
        List<Collection> collections = community.getCollections();
        Iterator<Item> result = Collections.<Item>emptyIterator();
        for (Collection collection : collections) {
            Iterator<Item> items = itemService.findByCollection(context, collection);
            result = addItemsToResult(result, items);

        }
        // Add all the sub-communities
        List<Community> communities = community.getSubcommunities();
        for (Community subCommunity : communities) {
            Iterator<Item> items = buildFromCommunity(context, subCommunity);
            result = addItemsToResult(result, items);
        }

        return result;
    }

    private Iterator<Item> addItemsToResult(Iterator<Item> result, Iterator<Item> items) {
        if (result == null) {
            result = items;
        } else {
            result = Iterators.concat(result, items);
        }

        return result;
    }
}
