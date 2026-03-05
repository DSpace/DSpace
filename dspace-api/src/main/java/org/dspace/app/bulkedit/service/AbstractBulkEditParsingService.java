/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.bulkedit.service;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.dspace.app.bulkedit.BulkEditChange;
import org.dspace.app.bulkedit.BulkEditMetadataValue;
import org.dspace.app.bulkedit.MetadataImportException;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.scripts.handler.DSpaceRunnableHandler;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Abstract service
 * Provides common logic along with hooks for implementing concrete parsing.
 *
 * Warning: This service and its extensions are stateful, so a new instance must be created every time it is used.
 *          This is by design because the service may keep information about multiple related changes until
 *          it is done parsing them all and this ensures none of the information leaks between other calls/processes.
 *          This means the service should never be Autowired and should instead be requested through the
 *          {@link BulkEditServiceFactory} wherever the call is made to parse and/or apply the changes.
 *
 * @param <T> The type of source object containing information about a batch-edit to parse
 * @param <R> The type of record contained in the source object, each to be converted into a {@link BulkEditChange}
 */
public abstract class AbstractBulkEditParsingService<T, R> implements BulkEditParsingService {
    @Autowired
    ConfigurationService configurationService;

    @Autowired
    ItemService itemService;

    protected T source;

    protected int rowCount = 1;
    protected int rowTotal;

    protected DSpaceRunnableHandler handler;

    public List<BulkEditChange> parse(
        Context c, InputStream is
    ) throws IOException, SQLException, MetadataImportException {
        if (this.source != null) {
            throw new IllegalStateException("Attempted to reuse stateful AbstractBulkEditParsingService instance!");
        }
        this.source = read(c, is);
        Context.Mode originalMode = c.getCurrentMode();

        try {
            // Force a READ ONLY mode to make it clear no actual changes are meant to be made during a register
            c.setMode(Context.Mode.READ_ONLY);

            List<BulkEditChange> changes = new ArrayList<>();
            List<R> records = getRecords();

            rowTotal = records.size();

            for (R record : records) {
                record = preprocessRecord(c, record);
                BulkEditChange change = initChange(c, record);

                resolveCollections(c, record, change);
                List<BulkEditMetadataValue> pending = parseMetadata(c, record, change);

                resolveAction(c, record, change);
                if (change.isNewItem()) {
                    pending.forEach(change::registerAdd);
                } else {
                    compareMetadata(c, record, change, pending);
                }

                additionalParsingSteps(c, record, change);
                validate(c, record, change);

                if (change.hasChanges()) {
                    changes.add(change);
                    logParse(change);
                    rowCount++;
                }
            }

            cleanUp(c);

            return changes;
        } finally {
            c.setMode(originalMode);
        }
    }

    /**
     * Read the input stream into a source object
     */
    protected abstract T read(Context c, InputStream is) throws IOException, SQLException, MetadataImportException;

    /**
     * Retrieve a list of records from the source.
     * Each record corresponds to a new Item or an edit of an existing Item.
     */
    protected abstract List<R> getRecords() throws SQLException, MetadataImportException;

    /**
     * Adjust a single record before parsing it (optional).
     */
    protected R preprocessRecord(Context c, R record) throws SQLException, MetadataImportException {
        return record;
    }

    /**
     * Initialize a {@link BulkEditChange} for a given record.
     */
    protected abstract BulkEditChange initChange(Context c, R record) throws SQLException, MetadataImportException;

    /**
     * Parse the owning & mapped Collections from the record (required)
     */
    protected abstract void resolveCollections(
        Context c, R record, BulkEditChange change
    ) throws SQLException, MetadataImportException;

    /**
     * Parse actions from the record (optional).
     * Only relevant for parsers that support editing.
     */
    protected void resolveAction(
        Context c, R record, BulkEditChange change
    ) throws SQLException, MetadataImportException {
    }

    /**
     * Parse metadata from the record (required).
     * Returns a list of "pending" metadata:
     * <ul>
     *     <li>For new imports all of these values will be added directly</li>
     *     <li>For edits they will be compared with the state of the current Item first</li>
     * </ul>
     */
    protected abstract List<BulkEditMetadataValue> parseMetadata(
        Context c, R record, BulkEditChange change
    ) throws SQLException, MetadataImportException;

    /**
     * Compare the parsed metadata with the current state of the Item.
     * Only used for parsers that support editing.
     *
     * @return whether any changes were made to this Item
     */
    protected void compareMetadata(
        Context c, R record, BulkEditChange change, List<BulkEditMetadataValue> pending
    ) throws SQLException {
        if (change.isNewItem()) {
            return;
        }

        // Compare with current Metadata (and Relationships ~ virtual Metadata)
        Map<String, List<BulkEditMetadataValue>> currentValues = BulkEditChange.getMetadataByField(
            itemService.getMetadata(change.getItem(), Item.ANY, Item.ANY, Item.ANY, Item.ANY, true)
                       .stream()
                       .map(mdv -> this.represent(c, change.getItem(), mdv))
                       .collect(Collectors.toList())
        );

        Map<String, List<BulkEditMetadataValue>> importValues = BulkEditChange.getMetadataByField(pending);
        change.getClear().forEach(bemf -> importValues.putIfAbsent(bemf.getKey(), List.of()));

        importValues.keySet().stream().sorted().forEach(field -> {
            Iterator<BulkEditMetadataValue> currentIterator = currentValues.getOrDefault(field, List.of()).iterator();
            Iterator<BulkEditMetadataValue> importIterator = importValues.getOrDefault(field, List.of()).iterator();

            while (currentIterator.hasNext() || importIterator.hasNext()) {
                if (currentIterator.hasNext() && importIterator.hasNext()) {
                    BulkEditMetadataValue currentValue = currentIterator.next();
                    BulkEditMetadataValue importValue = importIterator.next();

                    if (isValueChanged(currentValue, importValue)) {
                        change.registerConstant(importValue);
                    } else {
                        change.registerRemove(currentValue);
                        change.registerAdd(importValue);
                    }
                } else if (importIterator.hasNext()) {
                    BulkEditMetadataValue importValue = importIterator.next();
                    change.registerAdd(importValue);
                } else {
                    BulkEditMetadataValue currentValue = currentIterator.next();
                    change.registerRemove(currentValue);
                }
            }
        });
    }

    protected BulkEditMetadataValue represent(Context c, Item item, MetadataValue mdv) {
        BulkEditMetadataValue bemv = new BulkEditMetadataValue();

        bemv.setSchema(mdv.getMetadataField().getMetadataSchema().getName());
        bemv.setElement(mdv.getMetadataField().getElement());
        bemv.setQualifier(mdv.getMetadataField().getQualifier());
        bemv.setLanguage(mdv.getLanguage());
        bemv.setValue(mdv.getValue());
        bemv.setAuthority(mdv.getAuthority());
        bemv.setConfidence(mdv.getConfidence());
        return bemv;
    }

    /**
     * Check whether a metadata/Relationship value to be imported is changed, where the field, language and place are
     * already known to be equal.
     */
    protected boolean isValueChanged(BulkEditMetadataValue original, BulkEditMetadataValue imported) {
        return Strings.CI.equals(original.getValue(), imported.getValue())
            && Strings.CI.equals(original.getAuthority(), imported.getAuthority())
            && original.getConfidence() == imported.getConfidence();
    }


    /**
     * Define additional parsing steps here.
     */
    protected void additionalParsingSteps(
        Context c, R record, BulkEditChange change
    ) throws SQLException, MetadataImportException {
    }

    protected void validate(Context c, R record, BulkEditChange change) throws MetadataImportException {
        if (change.isNewItem()) {
            // Actions are only supported during editing
            if (change.isReinstated() || change.isWithdrawn() || change.isDeleted()) {
                throw new MetadataImportException("'action' not allowed for new items!");
            }

            // New imports must have an owning Collection
            if (change.getNewOwningCollection() == null) {
                throw new MetadataImportException("New items must have a 'collection' assigned");
            }
        }
    }

    /**
     * Define additional post-processing steps here.
     */
    protected void cleanUp(Context c) throws SQLException, MetadataImportException {
    }

    /**
     * Log once a change has been parsed
     */
    protected void logParse(BulkEditChange change) {
        if (handler == null) {
            return;
        }

        boolean isAdd = change.isNewItem();
        List<String> info = new ArrayList<>();

        if (change.getItem() != null) {
            info.add("uuid=" + change.getItem().getID());
        }

        handler.logInfo(String.format(
            "Row %d/%d: Parsed Item %s %s",
            rowCount, rowTotal,
            isAdd ? "import" : "update",
            info.isEmpty() ? "" : "(" + StringUtils.join(info, ", ") + ")"
        ));
    }

    /**
     * Optionally set a {@link DSpaceRunnableHandler} to log the parsing process
     * @param handler   {@link DSpaceRunnableHandler}
     */
    public void setHandler(DSpaceRunnableHandler handler) {
        this.handler = handler;
    }

}
