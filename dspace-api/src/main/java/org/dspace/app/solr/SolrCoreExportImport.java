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
    private String dateField;
    private String startDate;
    private String endDate;
    private String dateIncrement = "MONTH"; // WEEK, MONTH, YEAR
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

        if (commandLine.hasOption('s')) {
            startDate = commandLine.getOptionValue('s');
        }

        if (commandLine.hasOption('e')) {
            endDate = commandLine.getOptionValue('e');
        }

        if (commandLine.hasOption('i')) {
            dateIncrement = commandLine.getOptionValue('i').toUpperCase();
            if (!dateIncrement.equals("WEEK") && !dateIncrement.equals("MONTH") && !dateIncrement.equals("YEAR")) {
                throw new ParseException("Date increment must be WEEK, MONTH, or YEAR");
            }
        }
    }

    @Override
    public void internalRun() throws Exception {
        if (help) {
            printVerboseHelp();
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
                handler.logInfo("Running SOLR core export/import from CLI without authentication");
            }

            // Validate directory
            Path dirPath = Paths.get(directory);
            if (!Files.exists(dirPath)) {
                if (mode.equals("export")) {
                    Files.createDirectories(dirPath);
                    handler.logInfo("Created directory: " + directory);
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
     * Export complete SOLR core data to files using date ranges instead of pagination
     */
    private void exportCore() throws Exception {
        long startTime = System.currentTimeMillis();
        String solrUrl = configurationService.getProperty("solr.server");
        String fullCoreName = getFullCoreName(coreName);
        String baseUrl = solrUrl + "/" + fullCoreName;

        handler.logInfo("Starting export from SOLR core: " + baseUrl);

        // Determine date field based on core type
        dateField = getDateFieldForCore();
        handler.logInfo("Using date field '" + dateField + "' for range queries");

        // Get date range boundaries
        DateRange totalRange = getDateRange(baseUrl);
        if (totalRange == null) {
            log.warn("No date range found in core '{}'", fullCoreName);
            handler.logWarning("No date range found in core: " + fullCoreName);
            return;
        }

        handler.logInfo("Date range: " + totalRange.start + " to " + totalRange.end);

        // Generate date ranges based on increment
        List<DateRange> dateRanges = generateDateRanges(totalRange);
        handler.logInfo("Created " + dateRanges.size() + " date ranges using " + dateIncrement + " increment");

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        long processingStart = System.currentTimeMillis();
        for (int i = 0; i < dateRanges.size(); i++) {
            final int rangeIndex = i;
            final DateRange range = dateRanges.get(i);

            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    exportDateRange(baseUrl, rangeIndex, range);
                } catch (Exception e) {
                    log.error("Error exporting date range {} ({}): {}", rangeIndex, range, e.getMessage(), e);
                    throw new RuntimeException(e);
                }
            }, executor);

            futures.add(future);
        }

        // Wait for all ranges to complete
        handler.logInfo("Waiting for " + futures.size() + " export tasks to complete...");
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        long processingTime = System.currentTimeMillis() - processingStart;

        executor.shutdown();
        if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
            log.warn("Executor did not terminate gracefully, forcing shutdown");
            executor.shutdownNow();
        }

        long totalTime = System.currentTimeMillis() - startTime;
        handler.logInfo("Export completed in " + totalTime + " ms");
    }

    /**
     * Get the appropriate date field based on core name
     */
    private String getDateFieldForCore() {
        return switch (coreName) {
            case "statistics" -> "time";
            case "audit" -> "timeStamp";
            default -> "lastModified"; // Default fallback
        };
    }

    /**
     * Get min/max date range from SOLR or use provided parameters
     */
    private DateRange getDateRange(String baseUrl) throws Exception {
        String minDate = startDate;
        String maxDate = endDate;

        // If dates not provided, get them from SOLR
        if (StringUtils.isBlank(minDate) || StringUtils.isBlank(maxDate)) {
            String statsUrl = String.format("%s/select?q=*:*&rows=0&wt=json&stats=true&stats.field=%s",
                    baseUrl, dateField);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(statsUrl))
                    .timeout(Duration.ofMinutes(2))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new RuntimeException("SOLR stats query failed with status: " + response.statusCode() +
                        "Solr Core might be empty or not available");
            }

            JsonNode jsonResponse = jsonMapper.readTree(response.body());
            JsonNode stats = jsonResponse.path("stats").path("stats_fields").path(dateField);

            if (StringUtils.isBlank(minDate)) {
                String solrMinDate = stats.path("min").asText();
                // Normalize to date-only format
                minDate = solrMinDate.length() >= 10 ? solrMinDate.substring(0, 10) : solrMinDate;
            }
            if (StringUtils.isBlank(maxDate)) {
                String solrMaxDate = stats.path("max").asText();
                // Normalize to date-only format
                maxDate = solrMaxDate.length() >= 10 ? solrMaxDate.substring(0, 10) : solrMaxDate;
            }

            if (StringUtils.isBlank(minDate) || StringUtils.isBlank(maxDate)) {
                return null;
            }

            handler.logInfo("Retrieved date range from SOLR: " + minDate + " to " + maxDate);
        }

        return new DateRange(minDate, maxDate);
    }

    /**
     * Generate date ranges based on the increment setting
     */
    private List<DateRange> generateDateRanges(DateRange totalRange) {
        List<DateRange> ranges = new ArrayList<>();

        try {
            // Parse dates - handle both full ISO format and date-only format
            java.time.LocalDate startDate = parseInputDate(totalRange.start);
            java.time.LocalDate endDate = parseInputDate(totalRange.end);

            java.time.LocalDate current = startDate;

            while (current.isBefore(endDate) || current.isEqual(endDate)) {
                java.time.LocalDate next = switch (dateIncrement) {
                    case "WEEK" -> current.plusWeeks(1).minusDays(1);
                    case "MONTH" -> current.plusMonths(1).minusDays(1);
                    case "YEAR" -> current.plusYears(1).minusDays(1);
                    default -> current.plusMonths(1).minusDays(1);
                };

                // Don't exceed the end date
                if (next.isAfter(endDate)) {
                    next = endDate;
                }

                ranges.add(new DateRange(
                        current.atStartOfDay() + ":00.000Z",  // 00:00:00.000Z
                        next.atTime(23, 59, 59, 999_000_000).toString() + "Z"  // 23:59:59.999Z
                ));

                // Next range starts the day after this one ends
                current = next.plusDays(1);
            }
        } catch (Exception e) {
            log.warn("Failed to parse dates, creating single range: {}", e.getMessage());
            ranges.add(totalRange);
        }

        return ranges;
    }

    /**
     * Parse input date handling both ISO format and date-only format
     */
    private java.time.LocalDate parseInputDate(String dateStr) {
        if (dateStr.length() == 10) {
            // Date only format: 2023-01-01
            return java.time.LocalDate.parse(dateStr);
        } else {
            // Full ISO format: 2023-01-01T00:00:00Z
            return java.time.LocalDateTime.parse(dateStr.substring(0, 19)).toLocalDate();
        }
    }

    /**
     * Export data for a specific date range
     */
    private void exportDateRange(String baseUrl, int rangeIndex, DateRange range) throws Exception {
        long rangeStart = System.currentTimeMillis();
        Thread currentThread = Thread.currentThread();

        handler.logInfo("Thread '" + currentThread.getName() + "' exporting range " + rangeIndex +
                        " (" + range.start + " to " + range.end + ")");

        // Build SOLR query for date range using filter query
        String query = "*:*";
        String filterQuery = String.format("%s:[%s TO %s]", dateField, range.start, range.end);
        String url = String.format("%s/select?q=%s&fq=%s&rows=%d&wt=%s",
                baseUrl,
                java.net.URLEncoder.encode(query, "UTF-8"),
                java.net.URLEncoder.encode(filterQuery, "UTF-8"),
                Integer.MAX_VALUE,
                format);

        // Add field list
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
        String filename = String.format("solr_export_range_%04d.%s", rangeIndex, format);
        Path filePath = Paths.get(directory, filename);

        log.debug("Thread '{}' writing to file: {}", currentThread.getName(), filename);
        long writeStart = System.currentTimeMillis();

        try (InputStream inputStream = response.body();
             FileOutputStream outputStream = new FileOutputStream(filePath.toFile())) {
            inputStream.transferTo(outputStream);
        }

        long writeTime = System.currentTimeMillis() - writeStart;
        long totalTime = System.currentTimeMillis() - rangeStart;

        handler.logInfo("Thread '" + currentThread.getName() + "' completed range " + rangeIndex +
                        " in " + totalTime + " ms (query: " + queryTime + "ms, write: " + writeTime + "ms)");
    }

    /**
     * Simple data class to hold date range information
     */
    private static class DateRange {
        final String start;
        final String end;

        DateRange(String start, String end) {
            this.start = start;
            this.end = end;
        }

        @Override
        public String toString() {
            return start + " to " + end;
        }
    }

    /**
     * Import SOLR core data from files using direct HTTP calls
     */
    private void importCore() throws Exception {
        long startTime = System.currentTimeMillis();
        String solrUrl = configurationService.getProperty("solr.server");
        String fullCoreName = getFullCoreName(coreName);
        String baseUrl = solrUrl + "/" + fullCoreName;

        handler.logInfo("Starting import to SOLR core: " + baseUrl);

        // Look for both old batch files and new range files
        File[] files = new File(directory).listFiles((dir, name) ->
                name.startsWith("solr_export_range_")
                        && name.endsWith("." + format));

        if (files == null || files.length == 0) {
            throw new IllegalArgumentException("No export files found in directory: " + directory);
        }

        // Sort files to ensure consistent processing order
        java.util.Arrays.sort(files, java.util.Comparator.comparing(File::getName));

        handler.logInfo("Found " + files.length + " files to import using " + threadCount + " threads");

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
        handler.logInfo("Waiting for " + futures.size() + " import tasks to complete...");
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        long processingTime = System.currentTimeMillis() - processingStart;

        executor.shutdown();
        if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
            log.warn("Executor did not terminate gracefully, forcing shutdown");
            executor.shutdownNow();
        }

        // Final commit
        handler.logInfo("Performing final SOLR commit...");
        long commitStart = System.currentTimeMillis();
        commitToSolr(baseUrl);
        long commitTime = System.currentTimeMillis() - commitStart;

        long totalTime = System.currentTimeMillis() - startTime;
        handler.logInfo("Import completed in " + totalTime + " ms (processing: " + processingTime +
                "ms, commit: " + commitTime + "ms)");
    }

    /**
     * Import a single file using HTTP POST to SOLR
     */
    private void importFile(String baseUrl, File file) throws Exception {
        long fileStart = System.currentTimeMillis();
        Thread currentThread = Thread.currentThread();

        handler.logInfo("Thread '" + currentThread.getName() + "' importing file: " + file.getName() +
                        " (" + (file.length() / 1024) + "KB)");

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
        handler.logInfo("Thread '" + currentThread.getName() + "' completed import of '" + file.getName() +
                        "' in " + totalTime + " ms (upload: " + uploadTime + "ms)");
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
        String multicorePrefix = configurationService.getProperty("solr.multicorePrefix");

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

        handler.logInfo("Retrieved " + fields.size() + " fields from schema for core '" + coreName + "'");

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

        handler.logInfo("Retrieved " + fields.size() + " fields from sample document for core '" + coreName + "'");
        return fields;
    }

    @Override
    public SolrCoreExportImportScriptConfiguration getScriptConfiguration() {
        return new DSpace().getServiceManager().getServiceByName("solr-core-management",
                SolrCoreExportImportScriptConfiguration.class);
    }

    /**
     * Print help information for the script
     */
    private void printVerboseHelp() {
        handler.logInfo("SOLR Core Management Script");
        handler.logInfo("===========================");
        handler.logInfo("This script allows complete export and import of SOLR cores using date ranges for "
                + "optimal performance.");
        handler.logInfo("");
        handler.logInfo("Parameters:");
        handler.logInfo("  -m <mode>        : Required. Mode: 'export' or 'import'");
        handler.logInfo("  -c <core>        : Required. SOLR core name (statistics, audit, search, etc.)");
        handler.logInfo("  -d <directory>   : Required. Directory for export/import files");
        handler.logInfo("  -f <format>      : Optional. Format: 'csv' or 'json' (default: csv)");
        handler.logInfo("  -t <threads>     : Optional. Number of threads (default: 1)");
        handler.logInfo("  -s <startdate>   : Optional. Start date (format: 2023-01-01)");
        handler.logInfo("  -e <enddate>     : Optional. End date (format: 2023-12-31)");
        handler.logInfo("  -i <increment>   : Optional. Date increment: WEEK, MONTH, YEAR (default: MONTH)");
        handler.logInfo("  -h               : Show this help");
        handler.logInfo("");
        handler.logInfo("Examples:");
        handler.logInfo("  Export statistics core using date ranges:");
        handler.logInfo("    ./dspace solr-core-management -m export -c statistics -d /tmp/export -f csv -t 4");
        handler.logInfo("");
        handler.logInfo("  Export with specific date range:");
        handler.logInfo("    ./dspace solr-core-management -m export -c audit -d /tmp/export "
                + "-s 2023-01-01 -e 2023-06-30");
        handler.logInfo("");
        handler.logInfo("  Import from exported files:");
        handler.logInfo("    ./dspace solr-core-management -m import -c statistics -d /tmp/export -f csv -t 4");
        handler.logInfo("");
        handler.logInfo("Notes:");
        handler.logInfo("- Export uses date ranges instead of pagination for better performance on large datasets");
        handler.logInfo("- Dates should be in YYYY-MM-DD format, time is automatically set to start/end of day");
        handler.logInfo("- If start/end dates are not specified, they will be retrieved from SOLR");
        handler.logInfo("- Date increment determines how data is split across threads");
        handler.logInfo("- Import can process both old batch files and new range files");
    }
}
