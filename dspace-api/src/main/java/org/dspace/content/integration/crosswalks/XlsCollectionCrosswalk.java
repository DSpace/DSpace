/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.integration.crosswalks;

import static org.apache.commons.collections4.IteratorUtils.chainedIterator;
import static org.apache.commons.collections4.IteratorUtils.singletonListIterator;

import java.io.IOException;
import java.io.OutputStream;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.logging.Level;

import org.apache.commons.collections4.IteratorUtils;
import org.apache.poi.ss.usermodel.Workbook;
import org.dspace.app.bulkedit.BulkImport;
import org.dspace.app.bulkimport.service.BulkImportWorkbookBuilder;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.crosswalk.CrosswalkException;
import org.dspace.content.crosswalk.CrosswalkMode;
import org.dspace.content.crosswalk.CrosswalkObjectNotSupported;
import org.dspace.content.crosswalk.StreamDisseminationCrosswalk;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.scripts.handler.DSpaceRunnableHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of {@link StreamDisseminationCrosswalk} to export all the item
 * of the given collection in the xls format. This format is the same expected
 * by the import performed with {@link BulkImport}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class XlsCollectionCrosswalk implements ItemExportCrosswalk {

    @Autowired
    private ItemService itemService;

    private static final Logger LOGGER = LoggerFactory.getLogger(XlsCollectionCrosswalk.class);

    @Autowired
    private CollectionService collectionService;

    @Autowired
    private BulkImportWorkbookBuilder bulkImportWorkbookBuilder;

    private DSpaceRunnableHandler handler;

    @Override
    public boolean canDisseminate(Context context, DSpaceObject dso) {
        return dso.getType() == Constants.COLLECTION;
    }

    @Override
    public String getMIMEType() {
        return "application/vnd.ms-excel";
    }

    @Override
    public String getFileName() {
        return "items.xls";
    }

    public CrosswalkMode getCrosswalkMode() {
        return CrosswalkMode.MULTIPLE;
    }

    @Override
    public void disseminate(Context context, DSpaceObject dso, OutputStream out)
            throws CrosswalkException, IOException, SQLException, AuthorizeException {

        if (!canDisseminate(context, dso)) {
            throw new CrosswalkObjectNotSupported("Can only crosswalk a Collection");
        }

        Collection collection = (Collection) dso;

        Iterator<Item> itemIterator = itemService.findByCollection(context, collection);

        writeWorkbook(context, collection, itemIterator, out);

    }

    @Override
    public void disseminate(Context context, Iterator<? extends DSpaceObject> dsoIterator, OutputStream out)
            throws CrosswalkException, IOException, SQLException, AuthorizeException {

        if (!dsoIterator.hasNext()) {
            throw new IllegalArgumentException("At least one object must be provided to perform xsl export");
        }

        Item firstItem = convertToItem(dsoIterator.next());
        Collection collection = findCollection(context, firstItem);
        Iterator<Item> itemIterator = convertToItemIterator(dsoIterator);

        Iterator<Item> newItemIterator = chainedIterator(singletonListIterator(firstItem), itemIterator);
        writeWorkbook(context, collection, newItemIterator, out);

    }

    private void writeWorkbook(Context context, Collection collection, Iterator<Item> itemIterator, OutputStream out)
            throws IOException {
        this.setHandler(this.handler);
        try (Workbook workbook = bulkImportWorkbookBuilder.buildForItems(
                context, collection, itemIterator, this::logMessage)) {
            workbook.write(out);
        }

    }

    private Iterator<Item> convertToItemIterator(Iterator<? extends DSpaceObject> dsoIterator) {
        return IteratorUtils.transformedIterator(dsoIterator, this::convertToItem);
    }

    private Item convertToItem(DSpaceObject dso) {
        if (dso.getType() != Constants.ITEM) {
            throw new IllegalArgumentException("The xsl export supports only items. "
                    + "Found object with type " + dso.getType() + " and id " + dso.getID());
        }
        return (Item) dso;
    }

    private Collection findCollection(Context context, Item item) throws SQLException {
        Collection collection = collectionService.findByItem(context, item);
        if (collection == null) {
            throw new IllegalArgumentException("No collection found for item with id: " + item.getID());
        }
        return collection;
    }


    protected void logMessage(Level level, String message) {

        if (handler != null) {
            switch (level.getName()) {
                case "OFF":
                    LOGGER.info(message);
                    break;
                case "SEVERE":
                    handler.logError(message);
                    break;
                case "WARNING":
                    handler.logWarning(message);
                    break;
                case "INFO":
                case "CONFIG":
                case "FINE":
                    handler.logInfo(message);
                    break;
                case "FINER":
                case "FINEST":
                case "ALL":
                default:
                    handler.logDebug(message);
                    break;
            }
        }
    }
    public void setHandler(DSpaceRunnableHandler handler) {
        this.handler = handler;
    }
}
