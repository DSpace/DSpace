/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.bulkedit;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataSchema;
import org.dspace.content.MetadataValue;
import org.dspace.content.authority.Choices;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Streaming CSV exporter that writes item metadata directly to a temporary file
 * instead of accumulating all data in memory. This avoids OutOfMemoryError for
 * large exports by extracting only lightweight string data from each Item and
 * releasing the Hibernate entity immediately.
 *
 * <p>Uses a single-pass approach: iterates items once, extracting metadata into
 * lightweight row data objects. Item entities are uncached immediately after
 * extraction. The CSV is then written to a temp file from the extracted data.
 *
 * <p>This class is export-only. For CSV import, use {@link DSpaceCSV}.
 *
 * @author DSpace contributors
 */
public class StreamingDSpaceCsvExporter {

    @Autowired
    private ItemService itemService;

    @Autowired
    private ConfigurationService configurationService;

    /**
     * Export items to CSV via streaming, returning an InputStream over a temp file.
     *
     * @param context               the DSpace context
     * @param itemIteratorSupplier  supplier that provides a fresh item iterator (called once)
     * @param exportAll             whether to export all metadata including ignored fields
     * @param limit                 maximum number of items to export
     * @return an InputStream over the CSV content; caller must close it
     * @throws Exception if an error occurs during export
     */
    public InputStream export(Context context,
                              Supplier<Iterator<Item>> itemIteratorSupplier,
                              boolean exportAll,
                              int limit) throws Exception {
        String valueSeparator = getConfigProperty("bulkedit.valueseparator", "||");
        String fieldSeparator = resolveFieldSeparator(
            getConfigProperty("bulkedit.fieldseparator", ","));
        String authoritySeparator = getConfigProperty("bulkedit.authorityseparator", "::");
        Map<String, String> ignoreFields = buildIgnoreMap();

        // Single pass: iterate items once, extracting lightweight row data.
        // Item entities are uncached immediately after extraction to avoid OOM.
        Set<String> headerSet = new LinkedHashSet<>();
        List<RowData> rows = new ArrayList<>();

        Iterator<Item> items = itemIteratorSupplier.get();
        int itemCount = 0;
        while (items.hasNext() && itemCount < limit) {
            Item item = items.next();
            itemCount++;
            if (item.getOwningCollection() == null) {
                context.uncacheEntity(item);
                continue;
            }

            RowData row = extractRowData(item, exportAll, ignoreFields, authoritySeparator);
            headerSet.addAll(row.metadata.keySet());
            rows.add(row);
            context.uncacheEntity(item);
        }

        // Sort headers alphabetically (matches DSpaceCSV behavior)
        List<String> sortedHeaders = new ArrayList<>(headerSet);
        Collections.sort(sortedHeaders);

        // Write CSV to temp file
        File tempFile = File.createTempFile("dspace-csv-export-", ".csv");
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(tempFile), StandardCharsets.UTF_8))) {

            // Write header row
            writer.write("id");
            writer.write(fieldSeparator);
            writer.write("collection");
            for (String header : sortedHeaders) {
                writer.write(fieldSeparator);
                writer.write(header);
            }
            writer.newLine();

            // Write data rows
            for (RowData row : rows) {
                writeRow(writer, row, sortedHeaders, fieldSeparator, valueSeparator);
                writer.newLine();
            }
        } catch (Exception e) {
            tempFile.delete();
            throw e;
        }

        return new DeletingFileInputStream(tempFile);
    }

    /**
     * Extract lightweight row data from an Item, containing only strings.
     * The caller should uncache the Item entity after calling this method.
     */
    private RowData extractRowData(Item item, boolean exportAll,
                                   Map<String, String> ignoreFields,
                                   String authoritySeparator) {
        RowData row = new RowData();
        row.id = item.getID().toString();

        // Collection values: owning first, then mapped
        String owningHandle = item.getOwningCollection().getHandle();
        row.collections.add(owningHandle);
        for (Collection c : item.getCollections()) {
            if (!Objects.equals(c.getHandle(), owningHandle)) {
                row.collections.add(c.getHandle());
            }
        }

        // Metadata
        List<MetadataValue> md = itemService.getMetadata(item, Item.ANY, Item.ANY, Item.ANY, Item.ANY);
        for (MetadataValue value : md) {
            MetadataField metadataField = value.getMetadataField();
            if (exportAll || okToExport(metadataField, ignoreFields)) {
                String key = buildMetadataKey(value);
                String mdValue = value.getValue();
                if (value.getAuthority() != null && !"".equals(value.getAuthority())) {
                    mdValue += authoritySeparator + value.getAuthority()
                        + authoritySeparator
                        + (value.getConfidence() != -1 ? value.getConfidence() : Choices.CF_ACCEPTED);
                }
                row.metadata.computeIfAbsent(key, k -> new ArrayList<>()).add(mdValue);
            }
        }

        return row;
    }

    /**
     * Write a row to the CSV writer.
     */
    private void writeRow(BufferedWriter writer, RowData row,
                          List<String> sortedHeaders,
                          String fieldSeparator, String valueSeparator) throws IOException {
        // Write id
        writer.write("\"");
        writer.write(row.id);
        writer.write("\"");
        writer.write(fieldSeparator);

        // Write collection
        writer.write(valueToCSV(row.collections, valueSeparator));

        // Write each metadata column
        for (String header : sortedHeaders) {
            writer.write(fieldSeparator);
            List<String> values = row.metadata.get(header);
            if (values != null && !"collection".equals(header)) {
                writer.write(valueToCSV(values, valueSeparator));
            }
        }
    }

    /**
     * Build the metadata key string (schema.element.qualifier[language]).
     */
    private String buildMetadataKey(MetadataValue value) {
        MetadataField metadataField = value.getMetadataField();
        MetadataSchema metadataSchema = metadataField.getMetadataSchema();
        String key = metadataSchema.getName() + "." + metadataField.getElement();
        if (metadataField.getQualifier() != null) {
            key = key + "." + metadataField.getQualifier();
        }
        if (value.getLanguage() != null) {
            key = key + "[" + value.getLanguage() + "]";
        }
        return key;
    }

    /**
     * Format a list of values as a CSV field, matching {@link DSpaceCSVLine#valueToCSV}.
     */
    private String valueToCSV(List<String> values, String valueSeparator) {
        if (values == null) {
            return "";
        }

        String s;
        if (values.size() == 1) {
            s = values.get(0);
        } else {
            StringBuilder str = new StringBuilder();
            for (String value : values) {
                if (str.length() > 0) {
                    str.append(valueSeparator);
                }
                str.append(value);
            }
            s = str.toString();
        }

        // Replace internal quotes with two sets of quotes
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }

    /**
     * Check if a metadata field should be exported.
     */
    private boolean okToExport(MetadataField md, Map<String, String> ignoreFields) {
        String key = md.getMetadataSchema().getName() + "." + md.getElement();
        if (md.getQualifier() != null) {
            key += "." + md.getQualifier();
        }
        return ignoreFields.get(key) == null;
    }

    /**
     * Build the map of metadata fields to ignore on export.
     */
    private Map<String, String> buildIgnoreMap() {
        Map<String, String> ignore = new HashMap<>();
        String[] defaultValues = new String[] {
            "dc.date.accessioned", "dc.date.available", "dc.date.updated", "dc.description.provenance"
        };
        String[] toIgnoreArray = configurationService.getArrayProperty(
            "bulkedit.ignore-on-export", defaultValues);
        for (String toIgnoreString : toIgnoreArray) {
            if (!"".equals(toIgnoreString.trim())) {
                ignore.put(toIgnoreString.trim(), toIgnoreString.trim());
            }
        }
        return ignore;
    }

    /**
     * Get a config property with a default value.
     */
    private String getConfigProperty(String key, String defaultValue) {
        String value = configurationService.getProperty(key);
        if (value != null && !value.trim().isEmpty()) {
            return value.trim();
        }
        return defaultValue;
    }

    /**
     * Resolve special field separator names (tab, semicolon, hash) to their characters.
     */
    private String resolveFieldSeparator(String separator) {
        if ("tab".equals(separator)) {
            return "\t";
        } else if ("semicolon".equals(separator)) {
            return ";";
        } else if ("hash".equals(separator)) {
            return "#";
        }
        return separator;
    }

    /**
     * Lightweight data extracted from an Item for CSV export.
     * Contains only strings, allowing the Item entity to be uncached immediately.
     */
    private static class RowData {
        String id;
        List<String> collections = new ArrayList<>();
        Map<String, List<String>> metadata = new HashMap<>();
    }

    /**
     * A FileInputStream that deletes the underlying file when closed.
     * Ensures temp files are cleaned up after the export stream is consumed.
     */
    static class DeletingFileInputStream extends FileInputStream {
        private final File file;

        /**
         * Create a DeletingFileInputStream.
         *
         * @param file the file to read and delete on close
         * @throws IOException if the file cannot be opened
         */
        DeletingFileInputStream(File file) throws IOException {
            super(file);
            this.file = file;
        }

        @Override
        public void close() throws IOException {
            try {
                super.close();
            } finally {
                file.delete();
            }
        }
    }
}
