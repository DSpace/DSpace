/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.license;

import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.opencsv.CSVReader;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Loads the entire CSV into this bean that way we don't have to keep calling it and can utilize helper functions
 */
public class CCLicenseCSVRepository implements InitializingBean {

    @Autowired
    private ConfigurationService configurationService;

    private List<CCLicenseCSVRow> rows;

    private Map<String, List<CCLicenseCSVRow>> byVersion = new HashMap<>();
    private Map<String, CCLicenseCSVRow> byIdentifier = new HashMap<>();
    private Map<String, CCLicenseCSVRow> byUri = new HashMap<>();
    private List<CCLicenseCSVRow> entryPoints = new ArrayList<>();
    private Map<String, String> licenseUriIndex = new HashMap<>();

    @Override
    public void afterPropertiesSet() throws Exception {
        loadCsv();
        buildIndexes();
    }

    /**
     * Loads the Creative Commons license CSV file from the configured path and
     * converts each row into a {@link CCLicenseCSVRow}.
     *
     * <p>The CSV header is used to dynamically map column names to indices.
     * All parsed rows are stored in memory for later indexing and lookup.</p>
     *
     * @throws Exception if the CSV file cannot be read or parsed
     */
    private void loadCsv() throws Exception {
        String csvPath = configurationService.getProperty("dspace.cc-license.csv");

        List<CCLicenseCSVRow> result = new ArrayList<>();

        try (CSVReader reader = new CSVReader(new FileReader(csvPath))) {
            String[] header = reader.readNext();
            Map<String, Integer> col = mapHeader(header);

            int i = 1;
            String[] row;
            while ((row = reader.readNext()) != null) {
                CCLicenseCSVRow r = new CCLicenseCSVRow();
                r.setId("license" + i);
                r.setCategory(row[col.get("CATEGORY")]);
                r.setVersion(row[col.get("VERSION")]);
                r.setUnit(row[col.get("UNIT")]);
                r.setJurisdiction(row[col.get("JURISDICTION")]);
                r.setUrl(row[col.get("CANONICAL_URL")]);
                r.setIdentifier(row[col.get("IDENTIFIER")]);
                r.setTitle(row[col.get("TITLE")]);
                r.setEntryPoint(row[col.get("ENTRY_POINT")]);
                i++;
                result.add(r);
            }
        }

        this.rows = List.copyOf(result);
    }

    /**
     * Builds in-memory lookup indexes from the loaded CSV rows.
     *
     * <p>This method populates:
     * <ul>
     *     <li>{@code byVersion} for grouping licenses by version</li>
     *     <li>{@code byIdentifier} for direct lookup of special licenses</li>
     *     <li>{@code entryPoints} for UI entry-point licenses</li>
     *     <li>{@code licenseUriIndex} for fast version/unit/jurisdiction lookup</li>
     * </ul>
     * </p>
     */
    private void buildIndexes() {
        for (CCLicenseCSVRow row : rows) {

            // version grouping (for CC)
            byVersion.computeIfAbsent(row.getVersion(), k -> new ArrayList<>()).add(row);

            // identifier lookup (for PD)
            byIdentifier.put(row.getId(), row);

            byUri.put(row.getUrl(), row);

            // entry points
            if (row.getEntryPoint() != null && !row.getEntryPoint().isBlank()) {
                entryPoints.add(row);
            }

            String key = buildKey(row.getVersion(), row.getUnit(), row.getJurisdiction());
            licenseUriIndex.put(key, row.getUrl());
        }
    }

    /**
     * Builds a composite lookup key for license URI indexing.
     *
     * <p>The key format is:</p>
     * <pre>
     * version|unit|jurisdiction
     * </pre>
     *
     * <p>Null values are normalized to empty strings and all parts are trimmed.</p>
     *
     * @param version the Creative Commons version (e.g., "4.0", "3.0")
     * @param unit the license unit (e.g., "by-nc-sa")
     * @param jurisdiction the jurisdiction code (e.g., "us", "de"), may be empty
     * @return a normalized composite key used for internal map lookups
     */
    private String buildKey(String version, String unit, String jurisdiction) {
        return (version == null ? "" : version.trim()) + "|" +
                (unit == null ? "" : unit.trim()) + "|" +
                (jurisdiction == null ? "" : jurisdiction.trim());
    }

    public List<CCLicenseCSVRow> getEntryPoints() {
        return entryPoints;
    }

    public CCLicenseCSVRow getByIdentifier(String id) {
        return byIdentifier.get(id);
    }

    public String findUri(String version, String unit, String jurisdiction) {
        String key = buildKey(version, unit, jurisdiction);
        return licenseUriIndex.get(key);
    }

    public List<String> getJurisdictionsByVersion(String version) {
        return rows.stream()
                .filter(r -> version.equals(r.getVersion()))
                .map(CCLicenseCSVRow::getJurisdiction)
                .filter(j -> j != null && !j.isBlank())
                .distinct()
                .toList();
    }

    public CCLicenseCSVRow getByURI(String uri) {
        return byUri.get(uri);
    }

    /**
     * Maps CSV header column names to their corresponding index positions.
     *
     * <p>This allows the CSV parser to reference columns by name rather than
     * relying on fixed ordering.</p>
     *
     * <p>Header values are normalized by removing surrounding quotation marks.</p>
     *
     * @param header the first row of the CSV file containing column names
     * @return a map of column name to zero-based column index
     */
    private Map<String, Integer> mapHeader(String[] header) {
        Map<String, Integer> map = new HashMap<>();
        for (int i = 0; i < header.length; i++) {
            map.put(header[i].replace("\"", ""), i);
        }
        return map;
    }
}
