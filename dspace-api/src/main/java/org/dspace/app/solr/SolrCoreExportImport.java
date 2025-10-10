/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.solr;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.cli.ParseException;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;
import org.dspace.scripts.DSpaceRunnable;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.utils.DSpace;

/**
 * Script for complete export and import of SOLR cores with multithreading support.
 * Supports both CSV and JSON formats for data exchange.
 *
 * REST version requires admin privileges, CLI version can be executed freely.
 *
 * @author Stefano Maffei (stefano.maffei at 4science.com)
 */
public class SolrCoreExportImport extends DSpaceRunnable<SolrCoreExportImportScriptConfiguration> {

    private static final Logger log = LogManager.getLogger(SolrCoreExportImport.class);

    private String mode;
    private String coreName;
    private String directory;
    private String format = "csv";
    private int threadCount = 1;
    private int batchSize = 250_000;
    private boolean help = false;
    protected EPersonService epersonService;

    private ConfigurationService configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
    private ObjectMapper jsonMapper = new ObjectMapper();

    // Cache for field types per core to avoid repeated SOLR schema queries
    private java.util.Map<String, java.util.Set<String>> dateFieldsCache = new java.util.HashMap<>();

    /**
     * Determines if this script execution requires authentication.
     * Override in subclasses to change behavior (CLI vs REST).
     *
     * @return true if authentication is required, false otherwise
     */
    protected boolean requiresAuthentication() {
        // REST version requires authentication by default
        return true;
    }

    @Override
    public void setup() throws ParseException {
        if (commandLine.hasOption('h')) {
            help = true;
            return;
        }

        this.epersonService = EPersonServiceFactory.getInstance().getEPersonService();

        mode = commandLine.getOptionValue('m');
        if (StringUtils.isBlank(mode) || (!mode.equals("export") && !mode.equals("import"))) {
            throw new ParseException("Mode parameter is required and must be 'export' or 'import'");
        }

        coreName = commandLine.getOptionValue('c');
        if (StringUtils.isBlank(coreName)) {
            throw new ParseException("Core name parameter is required");
        }

        directory = commandLine.getOptionValue('d');
        if (StringUtils.isBlank(directory)) {
            throw new ParseException("Directory parameter is required");
        }

        if (commandLine.hasOption('f')) {
            format = commandLine.getOptionValue('f').toLowerCase();
            if (!format.equals("csv") && !format.equals("json")) {
                throw new ParseException("Format must be 'csv' or 'json'");
            }
        }

        if (commandLine.hasOption('t')) {
            try {
                threadCount = Integer.parseInt(commandLine.getOptionValue('t'));
                if (threadCount < 1) {
                    throw new ParseException("Thread count must be at least 1");
                }
            } catch (NumberFormatException e) {
                throw new ParseException("Invalid thread count: " + commandLine.getOptionValue('t'));
            }
        }

        if (commandLine.hasOption('b')) {
            try {
                batchSize = Integer.parseInt(commandLine.getOptionValue('b'));
                if (batchSize < 1) {
                    throw new ParseException("Batch size must be at least 1");
                }
            } catch (NumberFormatException e) {
                throw new ParseException("Invalid batch size: " + commandLine.getOptionValue('b'));
            }
        }
    }

    @Override
    public void internalRun() throws Exception {
        if (help) {
            printHelp();
            return;
        }

        Context context = new Context();
        try {
            context.turnOffAuthorisationSystem();

            // Only check authentication for REST execution, not CLI
            if (requiresAuthentication()) {
                // Set EPerson from parameters for REST execution
                if (getEpersonIdentifier() != null) {
                    try {
                        context.setCurrentUser(epersonService.find(context, getEpersonIdentifier()));
                    } catch (SQLException e) {
                        throw new RuntimeException("Failed to find EPerson", e);
                    }
                }

                // Check if user is authorized (admin required for REST)
                if (!getScriptConfiguration().isAllowedToExecute(context, null)) {
                    handler.logError("Current user is not eligible to execute SOLR core export/import script");
                    throw new AuthorizeException(
                            "Current user is not eligible to execute SOLR core export/import script");
                }
            } else {
                // CLI execution - log that we're running without authentication
                log.info("Running SOLR core export/import from CLI without authentication");
                handler.logInfo("Running from command line without authentication checks");
            }

            // Validate directory
            Path dirPath = Paths.get(directory);
            if (!Files.exists(dirPath)) {
                if (mode.equals("export")) {
                    Files.createDirectories(dirPath);
                    log.info("Created directory: {}", directory);
                } else {
                    throw new IllegalArgumentException("Import directory does not exist: " + directory);
                }
            }

            if (mode.equals("export")) {
                exportCore();
            } else {
                importCore();
            }

        } finally {
            context.restoreAuthSystemState();
            context.complete();
        }
    }

    /**
     * Export complete SOLR core data to files
     */
    private void exportCore() throws Exception {
        String solrUrl = configurationService.getProperty("solr.server");
        String fullCoreName = getFullCoreName(coreName);

        try (SolrClient solrClient = new HttpSolrClient.Builder(solrUrl + "/" + fullCoreName).build()) {
            // Get total document count
            SolrQuery countQuery = new SolrQuery("*:*");
            countQuery.setRows(0);
            QueryResponse countResponse = solrClient.query(countQuery);
            long totalDocs = countResponse.getResults().getNumFound();

            log.info("Starting export of {} documents from core '{}' (full name: '{}') using {} threads",
                    totalDocs, coreName, fullCoreName, threadCount);
            handler.logInfo("Exporting " + totalDocs + " documents from SOLR core: " + fullCoreName);

            if (totalDocs == 0) {
                log.warn("No documents found in core '{}'", coreName);
                handler.logWarning("No documents found in core: " + coreName);
                return;
            }

            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            List<CompletableFuture<Void>> futures = new ArrayList<>();

            // Calculate batches for parallel processing
            long numBatches = (totalDocs + batchSize - 1) / batchSize;

            for (int i = 0; i < numBatches; i++) {
                final int batchIndex = i;
                final int start = i * batchSize;
                final int rows = (int) Math.min(batchSize, totalDocs - start);

                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    try {
                        exportBatch(solrClient, batchIndex, start, rows);
                    } catch (Exception e) {
                        log.error("Error exporting batch {}: {}", batchIndex, e.getMessage(), e);
                        throw new RuntimeException(e);
                    }
                }, executor);

                futures.add(future);
            }

            // Wait for all batches to complete
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            executor.shutdown();
            executor.awaitTermination(30, TimeUnit.SECONDS);

            log.info("Export completed successfully for core '{}'", coreName);
            handler.logInfo("Export completed successfully");
        }
    }

    /**
     * Export a single batch of documents
     */
    private void exportBatch(SolrClient solrClient, int batchIndex, int start, int rows)
            throws SolrServerException, IOException {

        SolrQuery query = new SolrQuery("*:*");
        query.setStart(start);
        query.setRows(rows);

        // Use appropriate sort field based on core name
        String sortField = getSortFieldForCore();
        query.addSort(sortField, SolrQuery.ORDER.asc); // Ensure consistent ordering

        QueryResponse response = solrClient.query(query);
        SolrDocumentList docs = response.getResults();

        String filename = String.format("solr_export_batch_%04d.%s", batchIndex, format);
        Path filePath = Paths.get(directory, filename);

        if (format.equals("csv")) {
            exportBatchToCSV(docs, filePath);
        } else {
            exportBatchToJSON(docs, filePath);
        }

        log.debug("Exported batch {} ({} documents) to {}", batchIndex, docs.size(), filename);
    }

    /**
     * Determine the appropriate sort field based on core name.
     *
     * @return the field name to use for sorting
     */
    private String getSortFieldForCore() {
        return switch (coreName) {
            case "search" -> "search.uniqueid";
            case "suggestion" -> "suggestion_id";
            case "qaevent" -> "event_id";
            default -> "uid"; // Default sort field for other cores, generally statistics and audit
        };
    }

    /**
     * Export batch to CSV format
     */
    private void exportBatchToCSV(SolrDocumentList docs, Path filePath) throws IOException {
        try (FileWriter writer = new FileWriter(filePath.toFile());
             CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.withFirstRecordAsHeader())) {

            if (!docs.isEmpty()) {
                // Filter out problematic fields from the first document to create header
                SolrDocument firstDoc = docs.get(0);
                List<String> allowedFields = firstDoc.getFieldNames().stream()
                    .filter(this::isFieldAllowedForExport)
                    .collect(java.util.stream.Collectors.toList());

                csvPrinter.printRecord(allowedFields);

                // Write document data using only allowed fields
                for (SolrDocument doc : docs) {
                    List<Object> values = new ArrayList<>();
                    for (String fieldName : allowedFields) {
                        Object value = doc.getFieldValue(fieldName);
                        values.add(formatValueForExport(fieldName, value));
                    }
                    csvPrinter.printRecord(values);
                }
            }
        }
    }

    /**
     * Export batch to JSON format
     */
    private void exportBatchToJSON(SolrDocumentList docs, Path filePath) throws IOException {
        // Filter out problematic fields from documents before serialization
        List<java.util.Map<String, Object>> filteredDocs = new ArrayList<>();

        for (SolrDocument doc : docs) {
            java.util.Map<String, Object> filteredDoc = new java.util.HashMap<>();
            for (String fieldName : doc.getFieldNames()) {
                if (isFieldAllowedForExport(fieldName)) {
                    Object value = doc.getFieldValue(fieldName);
                    filteredDoc.put(fieldName, formatValueForExport(fieldName, value));
                }
            }
            filteredDocs.add(filteredDoc);
        }

        jsonMapper.writeValue(filePath.toFile(), filteredDocs);
    }

    /**
     * Format a field value for proper export handling.
     * Ensures dates are in ISO format and arrays are properly serialized.
     *
     * @param fieldName the name of the field
     * @param value the original value from SOLR
     * @return the formatted value for export
     */
    private Object formatValueForExport(String fieldName, Object value) {
        if (value == null) {
            return "";
        }

        // Handle date fields - convert to ISO format for proper reimport
        if (isDateField(fieldName) && value instanceof java.util.Date) {
            java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ISO_INSTANT;
            return formatter.format(((java.util.Date) value).toInstant());
        }

        // Handle collection/array fields
        if (value instanceof java.util.Collection) {
            java.util.Collection<?> collection = (java.util.Collection<?>) value;
            if (collection.isEmpty()) {
                return "";
            }
            // Convert to JSON array string for CSV compatibility
            return collection.stream()
                    .map(Object::toString)
                    .collect(java.util.stream.Collectors.joining(",", "[", "]"));
        }

        return value.toString();
    }

    /**
     * Determines if a field is a date field by querying SOLR schema.
     * Results are cached per core to avoid repeated queries.
     *
     * @param fieldName the field name to check
     * @return true if it's actually a date field according to SOLR schema
     */
    private boolean isDateField(String fieldName) {
        String fullCoreName = getFullCoreName(coreName);

        // Check cache first
        java.util.Set<String> dateFields = dateFieldsCache.get(fullCoreName);
        if (dateFields != null) {
            return dateFields.contains(fieldName);
        }

        // Query SOLR schema and populate cache
        dateFields = fetchDateFieldsFromSchema(fullCoreName);
        dateFieldsCache.put(fullCoreName, dateFields);

        return dateFields.contains(fieldName);
    }

    /**
     * Fetch actual date fields from SOLR schema using Luke Request Handler.
     * This gives us the real field types as defined in the schema.
     *
     * @param fullCoreName the complete core name with prefix
     * @return set of field names that are date types
     */
    private java.util.Set<String> fetchDateFieldsFromSchema(String fullCoreName) {
        java.util.Set<String> dateFields = new java.util.HashSet<>();
        String solrUrl = configurationService.getProperty("solr.server");

        try (SolrClient solrClient = new HttpSolrClient.Builder(solrUrl + "/" + fullCoreName).build()) {

            // Use Luke Request Handler to get field information
            org.apache.solr.client.solrj.request.LukeRequest lukeRequest =
                new org.apache.solr.client.solrj.request.LukeRequest();
            lukeRequest.setShowSchema(true);

            org.apache.solr.client.solrj.response.LukeResponse lukeResponse =
                lukeRequest.process(solrClient);

            // Get field information from Luke response
            java.util.Map<String, org.apache.solr.client.solrj.response.LukeResponse.FieldInfo> fields =
                lukeResponse.getFieldInfo();

            for (java.util.Map.Entry<String, org.apache.solr.client.solrj.response.LukeResponse.FieldInfo> entry :
                 fields.entrySet()) {

                String fieldName = entry.getKey();
                org.apache.solr.client.solrj.response.LukeResponse.FieldInfo fieldInfo = entry.getValue();
                String fieldType = fieldInfo.getType();

                // Check if field type indicates it's a date field
                if (isDateType(fieldType)) {
                    dateFields.add(fieldName);
                    log.debug("Identified date field '{}' with type '{}'", fieldName, fieldType);
                }
            }

            log.info("Found {} date fields in core '{}': {}", dateFields.size(), fullCoreName, dateFields);

        } catch (Exception e) {
            log.warn("Could not fetch schema for core '{}', falling back to pattern matching: {}",
                    fullCoreName, e.getMessage());

            // Fallback to the old pattern-based approach if schema query fails
            return getFallbackDateFields();
        }

        return dateFields;
    }

    /**
     * Determines if a SOLR field type represents a date/time field.
     *
     * @param fieldType the SOLR field type string
     * @return true if it's a date/time type
     */
    private boolean isDateType(String fieldType) {
        if (StringUtils.isBlank(fieldType)) {
            return false;
        }

        String type = fieldType.toLowerCase();
        return type.contains("date") ||
               type.contains("time") ||
               type.equals("pdate") ||
               type.equals("tdate") ||
               type.startsWith("date") ||
               type.endsWith("_dt");
    }

    /**
     * Fallback method using pattern matching when schema query fails.
     * Uses the old logic as a backup.
     *
     * @return set of likely date field names based on patterns
     */
    private java.util.Set<String> getFallbackDateFields() {
        java.util.Set<String> fallbackFields = new java.util.HashSet<>();

        // Add common known date field names as fallback
        fallbackFields.add("time");
        fallbackFields.add("timestamp");
        // Add more known patterns specific to your DSpace instance if needed

        log.info("Using fallback date field detection with {} known fields", fallbackFields.size());
        return fallbackFields;
    }

    /**
     * Determines if a field should be included in the export.
     * Excludes internal SOLR fields that cause problems during import.
     *
     * @param fieldName the name of the field to check
     * @return true if the field should be exported, false otherwise
     */
    private boolean isFieldAllowedForExport(String fieldName) {
        // Exclude internal SOLR fields that cause import issues
        return !fieldName.equals("_version_") &&
               !fieldName.equals("_root_") &&
               !fieldName.startsWith("_nest_");
    }

    /**
     * Import SOLR core data from files
     */
    private void importCore() throws Exception {
        String solrUrl = configurationService.getProperty("solr.server");
        String fullCoreName = getFullCoreName(coreName);

        try (SolrClient solrClient = new HttpSolrClient.Builder(solrUrl + "/" + fullCoreName).build()) {
            File[] files = new File(directory).listFiles((dir, name) ->
                name.startsWith("solr_export_batch_") && name.endsWith("." + format));

            if (files == null || files.length == 0) {
                throw new IllegalArgumentException("No export files found in directory: " + directory);
            }

            log.info("Starting import of {} files to core '{}' (full name: '{}') using {} threads",
                    files.length, coreName, fullCoreName, threadCount);
            handler.logInfo("Importing " + files.length + " files to SOLR core: " + fullCoreName);

            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            List<CompletableFuture<Void>> futures = new ArrayList<>();

            for (File file : files) {
                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    try {
                        importFile(solrClient, file);
                    } catch (Exception e) {
                        log.error("Error importing file {}: {}", file.getName(), e.getMessage(), e);
                        throw new RuntimeException(e);
                    }
                }, executor);

                futures.add(future);
            }

            // Wait for all imports to complete
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            executor.shutdown();
            executor.awaitTermination(30, TimeUnit.SECONDS);

            // Final commit
            solrClient.commit();

            log.info("Import completed successfully for core '{}'", coreName);
            handler.logInfo("Import completed successfully");
        }
    }

    /**
     * Import a single file
     */
    private void importFile(SolrClient solrClient, File file) throws Exception {
        if (format.equals("csv")) {
            importFromCSV(solrClient, file);
        } else {
            importFromJSON(solrClient, file);
        }

        log.debug("Imported file: {}", file.getName());
    }

    /**
     * Import from CSV format
     */
    private void importFromCSV(SolrClient solrClient, File file) throws Exception {
        try (CSVParser parser = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(
                Files.newBufferedReader(file.toPath()))) {

            List<SolrInputDocument> docs = new ArrayList<>();

            for (CSVRecord record : parser) {
                SolrInputDocument doc = new SolrInputDocument();
                for (String header : parser.getHeaderNames()) {
                    String value = record.get(header);
                    if (StringUtils.isNotBlank(value)) {
                        Object parsedValue = parseValueForImport(header, value);
                        if (parsedValue != null) {
                            doc.addField(header, parsedValue);
                        }
                    }
                }
                docs.add(doc);

                // Batch commit for memory efficiency
                if (docs.size() >= 10000) { // Reduced batch size for safer import
                    solrClient.add(docs);
                    docs.clear();
                }
            }

            // Add remaining documents
            if (!docs.isEmpty()) {
                solrClient.add(docs);
            }
        }
    }

    /**
     * Import from JSON format
     */
    private void importFromJSON(SolrClient solrClient, File file) throws Exception {
        @SuppressWarnings("unchecked")
        List<java.util.Map<String, Object>> docs = jsonMapper.readValue(file, List.class);
        List<SolrInputDocument> inputDocs = new ArrayList<>();

        for (java.util.Map<String, Object> docMap : docs) {
            SolrInputDocument inputDoc = new SolrInputDocument();
            for (java.util.Map.Entry<String, Object> entry : docMap.entrySet()) {
                String fieldName = entry.getKey();
                Object value = entry.getValue();

                if (value != null) {
                    Object parsedValue = parseValueForImport(fieldName, value.toString());
                    if (parsedValue != null) {
                        inputDoc.addField(fieldName, parsedValue);
                    }
                }
            }
            inputDocs.add(inputDoc);

            // Batch commit for memory efficiency
            if (inputDocs.size() >= 10000) { // Reduced batch size for safer import
                solrClient.add(inputDocs);
                inputDocs.clear();
            }
        }

        // Add remaining documents
        if (!inputDocs.isEmpty()) {
            solrClient.add(inputDocs);
        }
    }

    /**
     * Parse a value from import data to proper SOLR format.
     * Handles date conversion and array deserialization.
     *
     * @param fieldName the field name
     * @param value the string value from export
     * @return the parsed value for SOLR import
     */
    private Object parseValueForImport(String fieldName, String value) {
        if (StringUtils.isBlank(value) || value.equals("null")) {
            return null;
        }

        // Handle array fields (detect JSON array format)
        if (value.startsWith("[") && value.endsWith("]")) {
            String arrayContent = value.substring(1, value.length() - 1);
            if (StringUtils.isNotBlank(arrayContent)) {
                return java.util.Arrays.asList(arrayContent.split(",\\s*"));
            }
            return new ArrayList<>();
        }

        // Handle date fields - parse ISO format back to proper format
        if (isDateField(fieldName)) {
            try {
                // Try parsing ISO instant format first
                java.time.Instant instant = java.time.Instant.parse(value);
                return java.util.Date.from(instant);
            } catch (Exception e) {
                // If that fails, try other common formats
                try {
                    java.time.format.DateTimeFormatter formatter =
                        java.time.format.DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss zzz yyyy",
                                                                     java.util.Locale.ENGLISH);
                    java.time.ZonedDateTime zdt = java.time.ZonedDateTime.parse(value, formatter);
                    return java.util.Date.from(zdt.toInstant());
                } catch (Exception e2) {
                    log.warn("Could not parse date field '{}' with value '{}', using as string", fieldName, value);
                    return value; // Fallback to string
                }
            }
        }

        return value;
    }

    /**
     * Get the full core name including the multicore prefix if configured.
     *
     * @param baseName the base core name (e.g., "search", "statistics")
     * @return the full core name with prefix (e.g., "dspace-search", "dspace-statistics")
     */
    private String getFullCoreName(String baseName) {
        String multicorePrefix = configurationService.getProperty("solr.multicorePrefix");

        if (StringUtils.isNotBlank(multicorePrefix)) {
            return multicorePrefix + baseName;
        }

        return baseName;
    }

    @Override
    public SolrCoreExportImportScriptConfiguration getScriptConfiguration() {
        return new DSpace().getServiceManager().getServiceByName("solr-core-management",
                SolrCoreExportImportScriptConfiguration.class);
    }
}
