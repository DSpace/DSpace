/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.solr;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
 * Uses direct HTTP calls to SOLR for maximum performance and simplicity.
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
    private HttpClient httpClient;

    // Cache for fields list to avoid multiple calls to SOLR
    private List<String> cachedFields = null;

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

        // Initialize HTTP client for SOLR calls
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();

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
     * Export complete SOLR core data to files using direct HTTP calls
     */
    private void exportCore() throws Exception {
        long startTime = System.currentTimeMillis();
        String solrUrl = configurationService.getProperty("solr.server");
        String fullCoreName = getFullCoreName(coreName);
        String baseUrl = solrUrl + "/" + fullCoreName;

        log.info("Starting export from SOLR core: {}", baseUrl);
        handler.logInfo("Exporting from SOLR core: " + baseUrl);

        // Get total document count first
        long totalDocs = getTotalDocumentCount(baseUrl);
        log.info("Total documents to export: {}", totalDocs);

        if (totalDocs == 0) {
            log.warn("No documents found in core '{}'", fullCoreName);
            handler.logWarning("No documents found in core: " + fullCoreName);
            return;
        }

        // Calculate batches for parallel processing
        long numBatches = (totalDocs + batchSize - 1) / batchSize;
        log.info("Processing {} documents in {} batches using {} threads", totalDocs, numBatches, threadCount);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        long processingStart = System.currentTimeMillis();
        for (int i = 0; i < numBatches; i++) {
            final int batchIndex = i;
            final int start = i * batchSize;
            final int rows = (int) Math.min(batchSize, totalDocs - start);

            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    exportBatch(baseUrl, batchIndex, start, rows);
                } catch (Exception e) {
                    log.error("Error exporting batch {}: {}", batchIndex, e.getMessage(), e);
                    throw new RuntimeException(e);
                }
            }, executor);

            futures.add(future);
        }

        // Wait for all batches to complete
        log.info("Waiting for {} export tasks to complete...", futures.size());
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        long processingTime = System.currentTimeMillis() - processingStart;

        executor.shutdown();
        if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
            log.warn("Executor did not terminate gracefully, forcing shutdown");
            executor.shutdownNow();
        }

        long totalTime = System.currentTimeMillis() - startTime;
        double docsPerSecond = totalDocs / (totalTime / 1000.0);
        log.info("Export completed in {} ms ({} docs/sec)", totalTime, String.format("%.2f", docsPerSecond));
        handler.logInfo("Export completed successfully");
    }

    /**
     * Get total document count using a simple HTTP call to SOLR
     */
    private long getTotalDocumentCount(String baseUrl) throws Exception {
        String url = baseUrl + "/select?q=*:*&rows=0&wt=json";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofMinutes(2))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("SOLR count query failed with status: " + response.statusCode());
        }

        JsonNode jsonResponse = jsonMapper.readTree(response.body());
        return jsonResponse.path("response").path("numFound").asLong();
    }

    /**
     * Export a single batch using direct HTTP call to SOLR
     */
    private void exportBatch(String baseUrl, int batchIndex, int start, int rows) throws Exception {
        long batchStart = System.currentTimeMillis();
        Thread currentThread = Thread.currentThread();

        log.debug("Thread '{}' exporting batch {} (start={}, rows={})",
                currentThread.getName(), batchIndex, start, rows);

        // Build SOLR URL - similar to your curl example
        String sortField = getSortFieldForCore();
        String url = String.format("%s/select?q=*:*&start=%d&rows=%d&sort=%s%%20asc&wt=%s",
                baseUrl, start, rows, sortField, format);

        // Add field list to exclude problematic fields
        List<String> fields = getAvailableFields(baseUrl);
        String fieldList = String.join(",", fields);
        url += "&fl=" + fieldList;

        log.debug("Thread '{}' calling SOLR URL: {}", currentThread.getName(), url);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofMinutes(5))
                .GET()
                .build();

        long queryStart = System.currentTimeMillis();
        HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
        long queryTime = System.currentTimeMillis() - queryStart;

        if (response.statusCode() != 200) {
            throw new RuntimeException("SOLR export failed with status: " + response.statusCode());
        }

        // Write response directly to file
        String filename = String.format("solr_export_batch_%04d.%s", batchIndex, format);
        Path filePath = Paths.get(directory, filename);

        log.debug("Thread '{}' writing to file: {}", currentThread.getName(), filename);
        long writeStart = System.currentTimeMillis();

        try (InputStream inputStream = response.body();
             FileOutputStream outputStream = new FileOutputStream(filePath.toFile())) {
            inputStream.transferTo(outputStream);
        }

        long writeTime = System.currentTimeMillis() - writeStart;
        long totalTime = System.currentTimeMillis() - batchStart;

        log.info("Thread '{}' completed batch {} in {} ms (query: {}ms, write: {}ms)",
                currentThread.getName(), batchIndex, totalTime, queryTime, writeTime);
    }

    /**
     * Determine the appropriate sort field based on core name
     */
    private String getSortFieldForCore() {
        return switch (coreName) {
            case "search" -> "search.uniqueid";
            case "suggestion" -> "suggestion_id";
            case "qaevent" -> "event_id";
            default -> "uid"; // Default for statistics, audit, etc.
        };
    }

    /**
     * Import SOLR core data from files using direct HTTP calls
     */
    private void importCore() throws Exception {
        long startTime = System.currentTimeMillis();
        String solrUrl = configurationService.getProperty("solr.server");
        String fullCoreName = getFullCoreName(coreName);
        String baseUrl = solrUrl + "/" + fullCoreName;

        log.info("Starting import to SOLR core: {}", baseUrl);
        handler.logInfo("Importing to SOLR core: " + baseUrl);

        File[] files = new File(directory).listFiles((dir, name) ->
                name.startsWith("solr_export_batch_") && name.endsWith("." + format));

        if (files == null || files.length == 0) {
            throw new IllegalArgumentException("No export files found in directory: " + directory);
        }

        // Sort files to ensure consistent processing order
        java.util.Arrays.sort(files, java.util.Comparator.comparing(File::getName));

        log.info("Found {} files to import using {} threads", files.length, threadCount);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        long processingStart = System.currentTimeMillis();
        for (File file : files) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    importFile(baseUrl, file);
                } catch (Exception e) {
                    log.error("Error importing file {}: {}", file.getName(), e.getMessage(), e);
                    throw new RuntimeException(e);
                }
            }, executor);

            futures.add(future);
        }

        // Wait for all imports to complete
        log.info("Waiting for {} import tasks to complete...", futures.size());
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        long processingTime = System.currentTimeMillis() - processingStart;

        executor.shutdown();
        if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
            log.warn("Executor did not terminate gracefully, forcing shutdown");
            executor.shutdownNow();
        }

        // Final commit
        log.info("Performing final SOLR commit...");
        long commitStart = System.currentTimeMillis();
        commitToSolr(baseUrl);
        long commitTime = System.currentTimeMillis() - commitStart;

        long totalTime = System.currentTimeMillis() - startTime;
        log.info("Import completed in {} ms (processing: {}ms, commit: {}ms)",
                totalTime, processingTime, commitTime);
        handler.logInfo("Import completed successfully");
    }

    /**
     * Import a single file using HTTP POST to SOLR
     */
    private void importFile(String baseUrl, File file) throws Exception {
        long fileStart = System.currentTimeMillis();
        Thread currentThread = Thread.currentThread();

        log.info("Thread '{}' importing file: {} ({}KB)",
                currentThread.getName(), file.getName(), file.length() / 1024);

        String url = baseUrl + "/update";
        String contentType = format.equals("csv") ? "application/csv" : "application/json";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofMinutes(10))
                .header("Content-Type", contentType)
                .POST(HttpRequest.BodyPublishers.ofFile(file.toPath()))
                .build();

        long uploadStart = System.currentTimeMillis();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        long uploadTime = System.currentTimeMillis() - uploadStart;

        if (response.statusCode() != 200) {
            throw new RuntimeException("SOLR import failed for file " + file.getName() +
                    " with status: " + response.statusCode() + " - " + response.body());
        }

        long totalTime = System.currentTimeMillis() - fileStart;
        log.info("Thread '{}' completed import of '{}' in {} ms (upload: {}ms)",
                currentThread.getName(), file.getName(), totalTime, uploadTime);
    }

    /**
     * Commit changes to SOLR
     */
    private void commitToSolr(String baseUrl) throws Exception {
        String url = baseUrl + "/update?commit=true";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofMinutes(5))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{}"))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("SOLR commit failed with status: " + response.statusCode());
        }
    }

    /**
     * Get the full core name including the multicore prefix if configured
     */
    private String getFullCoreName(String baseName) {
        String multicorePrefix = configurationService.getProperty("solr.multicoreprefix");

        if (StringUtils.isNotBlank(multicorePrefix)) {
            return multicorePrefix + baseName;
        }

        return baseName;
    }

    /**
     * Get available fields from SOLR core schema
     */
    private List<String> getAvailableFields(String baseUrl) throws Exception {
        // Return cached fields if available
        if (cachedFields != null) {
            return cachedFields;
        }

        String url = baseUrl + "/schema/fields?wt=json";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofMinutes(1))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            log.warn("Could not retrieve schema fields from {}, using fallback method", baseUrl);
            return getFieldsFromSampleQuery(baseUrl);
        }

        List<String> fields = new ArrayList<>();
        JsonNode jsonResponse = jsonMapper.readTree(response.body());
        JsonNode fieldsArray = jsonResponse.path("fields");

        if (fieldsArray.isArray()) {
            for (JsonNode field : fieldsArray) {
                String fieldName = field.path("name").asText();
                // Exclude problematic fields
                if (!fieldName.startsWith("_") && !fieldName.equals("_version_") && !fieldName.equals("_root_")) {
                    fields.add(fieldName);
                }
            }
        }

        log.info("Retrieved {} fields from schema for core '{}'", fields.size(), coreName);

        // Cache the retrieved fields
        cachedFields = fields;
        return fields;
    }

    /**
     * Fallback method to get fields from a sample query when schema endpoint is not available
     */
    private List<String> getFieldsFromSampleQuery(String baseUrl) throws Exception {
        String url = baseUrl + "/select?q=*:*&rows=1&wt=json";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofMinutes(1))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Could not retrieve sample document from SOLR core");
        }

        List<String> fields = new ArrayList<>();
        JsonNode jsonResponse = jsonMapper.readTree(response.body());
        JsonNode docs = jsonResponse.path("response").path("docs");

        if (docs.isArray() && docs.size() > 0) {
            JsonNode firstDoc = docs.get(0);
            firstDoc.fieldNames().forEachRemaining(fieldName -> {
                // Exclude problematic fields
                if (!fieldName.startsWith("_") && !fieldName.equals("_version_") && !fieldName.equals("_root_")) {
                    fields.add(fieldName);
                }
            });
        }

        log.info("Retrieved {} fields from sample document for core '{}'", fields.size(), coreName);
        return fields;
    }

    @Override
    public SolrCoreExportImportScriptConfiguration getScriptConfiguration() {
        return new DSpace().getServiceManager().getServiceByName("solr-core-management",
                SolrCoreExportImportScriptConfiguration.class);
    }
}
