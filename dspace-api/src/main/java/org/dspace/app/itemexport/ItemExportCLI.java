/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.itemexport;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.dspace.app.itemexport.service.ItemExportService;
import org.dspace.content.Item;
import org.dspace.core.Context;

/**
 * CLI variant for the {@link ItemExport} class.
 * This was done to specify the specific behaviors for the CLI.
 *
 * @author Francesco Pio Scognamiglio (francescopio.scognamiglio at 4science.com)
 */
public class ItemExportCLI extends ItemExport {

    @Override
    protected void validate() {
        super.validate();

        setDestDirName();

        if (destDirName == null) {
            handler.logError("The destination directory must be set (run with -h flag for details)");
            throw new UnsupportedOperationException("The destination directory must be set");
        }

        if (seqStart == -1) {
            handler.logError("The sequence start number must be set (run with -h flag for details)");
            throw new UnsupportedOperationException("The sequence start number must be set");
        }
    }

    @Override
    protected void process(Context context, ItemExportService itemExportService) throws Exception {
        setZip(context);

        if (zip) {
            Iterator<Item> items;
            if (item != null) {
                List<Item> myItems = new ArrayList<>();
                myItems.add(item);
                items = myItems.iterator();
            } else {
                handler.logInfo("Exporting from collection: " + idString);
                items = itemService.findByCollection(context, collection);
            }
            itemExportService.exportAsZip(context, items, destDirName, zipFileName,
                    seqStart, migrate, excludeBitstreams);
        } else {
            if (item != null) {
                // it's only a single item
                itemExportService
                    .exportItem(context, Collections.singletonList(item).iterator(), destDirName,
                            seqStart, migrate, excludeBitstreams);
            } else {
                handler.logInfo("Exporting from collection: " + idString);

                // it's a collection, so do a bunch of items
                Iterator<Item> i = itemService.findByCollection(context, collection);
                itemExportService.exportItem(context, i, destDirName, seqStart, migrate, excludeBitstreams);
            }
        }
    }

    protected void setDestDirName() {
        if (commandLine.hasOption('d')) { // dest
            destDirName = commandLine.getOptionValue('d');
        }
    }

    @Override
    protected void setZip(Context context) {
        if (commandLine.hasOption('z')) {
            zip = true;
            zipFileName = commandLine.getOptionValue('z');
        }
    }

    @Override
    protected void setNumber() {
        if (commandLine.hasOption('n')) { // number
            seqStart = Integer.parseInt(commandLine.getOptionValue('n'));
        }
    }
}
