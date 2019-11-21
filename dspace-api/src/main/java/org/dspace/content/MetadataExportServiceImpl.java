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
import java.util.Iterator;
import java.util.List;

import com.google.common.collect.Iterators;
import org.apache.logging.log4j.Logger;
import org.dspace.app.bulkedit.DSpaceCSV;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.MetadataExportService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.handle.factory.HandleServiceFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class MetadataExportServiceImpl implements MetadataExportService {

    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(MetadataExportServiceImpl.class);

    @Autowired
    private ItemService itemService;

    public DSpaceCSV handleExport(Context context, boolean exportAllItems, boolean exportAllMetadata, String handle)
        throws Exception {
        Iterator<Item> toExport = null;

        if (!exportAllItems) {
            log.info("Exporting whole repository WARNING: May take some time!");
            toExport = itemService.findAll(context);
        } else {
            DSpaceObject dso = HandleServiceFactory.getInstance().getHandleService().resolveToObject(context, handle);
            if (dso == null) {
                throw new IllegalArgumentException(
                    "Item '" + handle + "' does not resolve to an item in your repository!");
            }

            if (dso.getType() == Constants.ITEM) {
                log.info("Exporting item '" + dso.getName() + "' (" + handle + ")");
                List<Item> item = new ArrayList<>();
                item.add((Item) dso);
                toExport = item.iterator();
            } else if (dso.getType() == Constants.COLLECTION) {
                log.info("Exporting collection '" + dso.getName() + "' (" + handle + ")");
                Collection collection = (Collection) dso;
                toExport = itemService.findByCollection(context, collection);
            } else if (dso.getType() == Constants.COMMUNITY) {
                log.info("Exporting community '" + dso.getName() + "' (" + handle + ")");
                toExport = buildFromCommunity(context, (Community) dso);
            } else {
                throw new IllegalArgumentException("Error identifying '" + handle + "'");
            }
        }

        DSpaceCSV csv = this.export(context, toExport, exportAllMetadata);
        return csv;
    }

    /**
     * Run the export
     *
     * @return the exported CSV lines
     */
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
        Iterator<Item> result = null;
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
