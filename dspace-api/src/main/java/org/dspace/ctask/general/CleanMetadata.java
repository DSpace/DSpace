/*
 * CleanMetadata.java
 *
 * DSpace 9 Curation Task
 * ----------------------
 * Standardizes metadata field values on every Item it processes by:
 *
 *   1. QUOTES     — converts curly/smart quote variants to plain straight quotes
 *                   (applies to ALL metadata fields)
 *
 *   2. DASHES     — converts double hyphens (--) to em dashes (—)
 *                   (applies only to fields listed in cleanmetadata.cfg)
 *                   Sequences of three or more hyphens (---) are preserved
 *                   because they are often legal-document placeholders.
 *
 *   3. WHITESPACE — strips leading/trailing spaces and collapses internal
 *                   runs of two or more spaces to a single space
 *                   (applies to ALL metadata fields)
 *
 * Configuration file: [dspace]/config/modules/cleanmetadata.cfg
 *
 * Registration (curate.cfg):
 *   plugin.named.org.dspace.curate.CurationTask = \
 *       org.dspace.ctask.general.CleanMetadata = cleanmetadata
 *
 * CLI usage:
 *   [dspace]/bin/dspace curate -t cleanmetadata -i all
 *   [dspace]/bin/dspace curate -t cleanmetadata -i hdl:123456789/1
 *
 * Return codes:
 *   Curator.CURATE_SUCCESS  — item processed; changes may or may not have been made
 *   Curator.CURATE_SKIP     — DSO is not an Item (Community / Collection); skipped
 *   Curator.CURATE_ERROR    — an unexpected exception occurred
 *
 * Author  : Generated for DSpace 9 metadata preparation
 * Licence : MIT — free to use and modify
 */
package org.dspace.ctask.general;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.handle.factory.HandleServiceFactory;
import org.dspace.handle.service.HandleService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.curate.AbstractCurationTask;
import org.dspace.curate.Curator;
import org.dspace.curate.Distributive;

/**
 * Curation task that standardizes quotation marks, dashes, and whitespace
 * in DSpace Item metadata fields to prepare for or maintain Solr-consistent
 * indexing in DSpace Discovery.
 *
 * Annotated with @Distributive so that when applied to a Collection or
 * Community the curation system automatically applies it to every Item
 * within that container.
 */
@Distributive
public class CleanMetadata extends AbstractCurationTask {

    private static final Logger log = LogManager.getLogger(CleanMetadata.class);

    // ── Services ─────────────────────────────────────────────────────────────

    private final ItemService itemService =
            ContentServiceFactory.getInstance().getItemService();

    private final HandleService handleService =
            HandleServiceFactory.getInstance().getHandleService();

    // ── Regex patterns ────────────────────────────────────────────────────────

    /**
     * All curly/smart quote Unicode characters that should become straight quotes.
     * U+201C "  U+201D "  U+201E „  U+2033 ″   → U+0022 "
     * U+2018 '  U+2019 '  U+201A ‚  U+2032 ′   → U+0027 '
     */
    private static final Pattern CURLY_DOUBLE_PATTERN =
            Pattern.compile("[\u201C\u201D\u201E\u2033]");

    private static final Pattern CURLY_SINGLE_PATTERN =
            Pattern.compile("[\u2018\u2019\u201A\u2032]");

    /**
     * Exactly two hyphens NOT preceded or followed by another hyphen.
     * Matches:  word--word   text -- text
     * Skips:    ------   ---   (legal placeholders)
     */
    private static final Pattern DOUBLE_HYPHEN_PATTERN =
            Pattern.compile("(?<!-)-{2}(?!-)");

    /** Two or more consecutive space characters (U+0020). */
    private static final Pattern MULTI_SPACE_PATTERN =
            Pattern.compile(" {2,}");

    // ── Configuration keys (resolved from cleanmetadata.cfg) ─────────────────

    /** Comma-separated list of fields to apply dash normalization to. */
    private static final String PROP_DASH_FIELDS    = "dash.fields";

    /** Set to "false" to disable quote normalization. Default: true. */
    private static final String PROP_FIX_QUOTES     = "fix.quotes";

    /** Set to "false" to disable dash normalization. Default: true. */
    private static final String PROP_FIX_DASHES     = "fix.dashes";

    /** Set to "false" to disable whitespace normalization. Default: true. */
    private static final String PROP_FIX_WHITESPACE = "fix.whitespace";

    // ── Runtime configuration (loaded in init()) ──────────────────────────────

    private Set<String> dashFields    = new HashSet<>();
    private boolean     fixQuotes     = true;
    private boolean     fixDashes     = true;
    private boolean     fixWhitespace = true;

    // ── Default dash target fields ────────────────────────────────────────────

    private static final List<String> DEFAULT_DASH_FIELDS = Arrays.asList(
            "dc.description.abstract",
            "dc.description.lawtext",
            "dc.description.summary",
            "dc.title"
    );

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    /**
     * Called once before the task begins processing objects.
     * Reads configuration from cleanmetadata.cfg via taskProperty().
     */
    @Override
    public void init(Curator curator, String taskId) throws IOException {
        super.init(curator, taskId);

        // Read boolean flags
        fixQuotes     = taskBooleanProperty(PROP_FIX_QUOTES,     true);
        fixDashes     = taskBooleanProperty(PROP_FIX_DASHES,     true);
        fixWhitespace = taskBooleanProperty(PROP_FIX_WHITESPACE, true);

        // Read dash target fields; fall back to defaults if not configured
        String dashFieldsCfg = taskProperty(PROP_DASH_FIELDS);
        if (dashFieldsCfg != null && !dashFieldsCfg.isBlank()) {
            for (String f : dashFieldsCfg.split(",")) {
                dashFields.add(f.trim());
            }
        } else {
            dashFields.addAll(DEFAULT_DASH_FIELDS);
        }

        log.info("CleanMetadata init — fix.quotes={}, fix.dashes={}, " +
                "fix.whitespace={}, dash.fields={}",
                fixQuotes, fixDashes, fixWhitespace, dashFields);
    }

    // ── Main entry point ──────────────────────────────────────────────────────

    /**
     * Processes a single DSpaceObject.
     * Non-Item objects (Communities, Collections) are skipped; the
     * @Distributive annotation ensures child Items are still visited.
     *
     * @param dso the object to process
     * @return a Curator status code
     */
    @Override
    public int perform(DSpaceObject dso) throws IOException {

        Context context = Curator.curationContext();

        // Skip anything that is not an Item
        if (dso.getType() != Constants.ITEM) {
            setResult("Skipped — not an Item: " + getHandle(context, dso));
            return Curator.CURATE_SKIP;
        }

        Item item = (Item) dso;
        String handle = getHandle(context, item);

        List<String> changeLog  = new ArrayList<>();
        int          totalFixed = 0;

        try {
            // Iterate over a snapshot of the item's metadata values
            List<MetadataValue> values =
                    itemService.getMetadata(item, Item.ANY, Item.ANY, Item.ANY, Item.ANY);

            for (MetadataValue mv : values) {

                String original = mv.getValue();
                if (original == null || original.isEmpty()) {
                    continue;
                }

                // Build the fully qualified field name for logging and config lookup
                String fieldName = buildFieldName(mv);

                String cleaned = original;

                // Step 1 — Quotes (all fields)
                if (fixQuotes) {
                    cleaned = normalizeQuotes(cleaned);
                }

                // Step 2 — Dashes (configured fields only)
                if (fixDashes && dashFields.contains(fieldName)) {
                    cleaned = normalizeDashes(cleaned);
                }

                // Step 3 — Whitespace (all fields)
                if (fixWhitespace) {
                    cleaned = normalizeWhitespace(cleaned);
                }

                // Persist the change if the value actually changed
                if (!cleaned.equals(original)) {
                    mv.setValue(cleaned);
                    totalFixed++;
                    changeLog.add(String.format(
                            "  [%s] row=%d  \"%s\"  →  \"%s\"",
                            fieldName,
                            mv.getPlace(),
                            truncate(original, 60),
                            truncate(cleaned, 60)
                    ));
                }
            }

            // Persist all changes on the item in one database write
            if (totalFixed > 0) {
                itemService.update(context, item);
                context.commit();
                log.info("Item {} updated with {} cleaned value(s).", handle, totalFixed);
            }

        } catch (SQLException e) {
            log.error("SQL error processing item {}: {}", handle, e.getMessage(), e);
            setResult("Error: " + e.getMessage());
            return Curator.CURATE_ERROR;
        }

        // Build and store the result string for reporting
        String result;
        if (totalFixed == 0) {
            result = "Item " + handle + " — no changes needed.";
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append("Item ").append(handle)
              .append(" — ").append(totalFixed).append(" value(s) cleaned:\n");
            changeLog.forEach(sb::append);
            result = sb.toString();
        }

        log.info(result);
        setResult(result);
        report(result);
        return Curator.CURATE_SUCCESS;
    }

    // ── Normalization helpers ─────────────────────────────────────────────────

    /**
     * Converts all curly/smart quote variants to plain straight equivalents.
     * Double curly variants → U+0022 (")
     * Single curly variants → U+0027 (')
     */
    private String normalizeQuotes(String value) {
        String result = CURLY_DOUBLE_PATTERN.matcher(value).replaceAll("\"");
        result        = CURLY_SINGLE_PATTERN.matcher(result).replaceAll("'");
        return result;
    }

    /**
     * Converts exactly two consecutive hyphens (--) to an em dash (—).
     * Sequences of three or more hyphens are left untouched.
     */
    private String normalizeDashes(String value) {
        return DOUBLE_HYPHEN_PATTERN.matcher(value).replaceAll("\u2014");
    }

    /**
     * Strips leading and trailing spaces and collapses internal runs of
     * two or more spaces to a single space.
     */
    private String normalizeWhitespace(String value) {
        return MULTI_SPACE_PATTERN.matcher(value.strip()).replaceAll(" ");
    }

    // ── Utility helpers ───────────────────────────────────────────────────────

    /**
     * Builds the dot-separated Dublin Core field name from a MetadataValue,
     * e.g. "dc.description.abstract" or "dc.title".
     */
    private String getHandle(Context context, DSpaceObject dso) {
        if (context == null || dso == null) {
            return null;
        }
        try {
            return handleService.findHandle(context, dso);
        } catch (SQLException e) {
            log.warn("Unable to resolve handle for {}: {}", dso.getID(), e.getMessage());
            return null;
        }
    }

    private String buildFieldName(MetadataValue mv) {
        String schema    = mv.getMetadataField().getMetadataSchema().getName();
        String element   = mv.getMetadataField().getElement();
        String qualifier = mv.getMetadataField().getQualifier();

        if (qualifier == null || qualifier.isBlank()) {
            return schema + "." + element;
        }
        return schema + "." + element + "." + qualifier;
    }

    /**
     * Truncates a string to the given length, appending "..." if truncated.
     */
    private String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }
}
