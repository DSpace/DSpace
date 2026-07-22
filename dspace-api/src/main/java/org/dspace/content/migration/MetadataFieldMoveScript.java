/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.migration;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.commons.cli.ParseException;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataValue;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.MetadataFieldService;
import org.dspace.content.service.MetadataValueService;
import org.dspace.core.Context;
import org.dspace.scripts.DSpaceRunnable;
import org.dspace.utils.DSpace;

/**
 * DSpace script that MOVES (renames) metadata values from one metadata field to another, directly
 * at the database level.
 *
 * <p>The source is selected with a full Java regular expression matched against each registered
 * field's canonical name ({@code schema.element[.qualifier]}). The target is a template that may
 * reference capture groups, e.g. {@code $1}. Every registered field whose canonical name matches the
 * regex is moved to the field produced by applying the template.</p>
 *
 * <p>Examples:</p>
 * <pre>
 *   dspace metadata-field-move -s '^dc\.relation$'   -t 'dc.relation.project'
 *   dspace metadata-field-move -s '^cris\.(.+)$'     -t 'dspace.$1'
 * </pre>
 *
 * <p>Behaviour:</p>
 * <ul>
 *   <li><b>Move</b>: each matching value is reassigned to the target field and removed from the
 *       source field.</li>
 *   <li><b>Missing target fails</b>: if any resolved target field is not registered, the run aborts
 *       with no changes (register the field first).</li>
 *   <li><b>Collision fails</b>: if two distinct source fields resolve to the same target field, the
 *       run aborts with no changes.</li>
 *   <li><b>Skip exact duplicates</b>: a source value is skipped (deleted) when an identical value
 *       (text value + authority + language) already exists on the target for the same object, or has
 *       already been moved there during this run.</li>
 *   <li><b>Place</b>: when the target already has values on an object, moved values are appended
 *       after the highest existing place; otherwise the original place is preserved. Value, language,
 *       authority and confidence are always preserved.</li>
 *   <li><b>Scope</b>: global &mdash; every stored metadata value of the matched fields, regardless of
 *       object type or workflow state. Virtual (relationship) metadata is not affected.</li>
 * </ul>
 *
 * <p>This script bypasses the DSpace event/indexing pipeline, so after a real run the search index
 * must be refreshed with {@code dspace index-discovery -b}.</p>
 *
 * <p>Commits are performed per batch; the script is not atomic and a failure after a batch has been
 * committed will leave prior batches applied.</p>
 *
 * <p>Usage:</p>
 * <pre>
 *   dspace metadata-field-move -s &lt;source-regex&gt; -t &lt;target-template&gt; [-n] [-b &lt;batch-size&gt;]
 * </pre>
 *
 * @author Adamo Fapohunda (adamo.fapohunda at 4science.com)
 */
public class MetadataFieldMoveScript extends DSpaceRunnable<MetadataFieldMoveScriptConfiguration> {

    /** Default number of objects processed per batch when {@code -b} is not provided. */
    protected static final int DEFAULT_BATCH_SIZE = 1000;

    /** Token used to represent {@code null} components in the deduplication key. */
    private static final String NULL_TOKEN = "\u0001";

    /** Delimiter used between the components of the deduplication key. */
    private static final char KEY_DELIMITER = '\u0000';

    private String sourceRegex;
    private String targetTemplate;
    private boolean dryRun;
    private int batchSize = DEFAULT_BATCH_SIZE;

    private MetadataFieldService metadataFieldService;
    private MetadataValueService metadataValueService;

    @Override
    public void setup() throws ParseException {
        if (commandLine.hasOption('h')) {
            printHelp();
            handler.logInfo("Help displayed");
            return;
        }

        if (!commandLine.hasOption('s')) {
            throw new ParseException("Option -s (source field regex) is required");
        }
        if (!commandLine.hasOption('t')) {
            throw new ParseException("Option -t (target field template) is required");
        }
        sourceRegex = commandLine.getOptionValue('s');
        targetTemplate = commandLine.getOptionValue('t');

        if (commandLine.hasOption('b')) {
            try {
                batchSize = Integer.parseInt(commandLine.getOptionValue('b'));
            } catch (NumberFormatException e) {
                throw new ParseException("Option -b must be a valid integer: " + commandLine.getOptionValue('b'));
            }
            if (batchSize <= 0) {
                throw new ParseException("Option -b (batch size) must be a positive integer, got: " + batchSize);
            }
        }

        dryRun = commandLine.hasOption('n');
    }

    @Override
    public void internalRun() throws Exception {
        if (commandLine.hasOption('h')) {
            return;
        }

        metadataFieldService = ContentServiceFactory.getInstance().getMetadataFieldService();
        metadataValueService = ContentServiceFactory.getInstance().getMetadataValueService();

        try (Context context = new Context(Context.Mode.READ_WRITE)) {
            context.turnOffAuthorisationSystem();

            // Resolves the field pairs and validates them; aborts the context and throws on any
            // missing target field or source-to-target collision (so no changes are made).
            List<FieldMapping> mappings = resolveMappings(context);

            handler.logInfo("=== Metadata Field Move ===");
            handler.logInfo("Mode: " + (dryRun ? "DRY RUN (no changes)" : "APPLY"));
            handler.logInfo("Source field regex: " + sourceRegex);
            handler.logInfo("Note: the script commits each batch independently and is not atomic.");
            handler.logInfo("Target field template: " + targetTemplate);
            handler.logInfo("Batch size (objects): " + batchSize);

            if (mappings.isEmpty()) {
                handler.logWarning("No registered metadata field matches the source regex: " + sourceRegex);
                context.complete();
                return;
            }

            for (FieldMapping mapping : mappings) {
                long count = metadataValueService.countByField(context, mapping.source);
                handler.logInfo("  " + mapping.sourceCanonical + "  ->  " + mapping.targetCanonical
                    + "  (" + count + " value(s))");
            }

            if (dryRun) {
                handler.logInfo("DRY RUN: no changes committed.");
                return;
            }

            long totalMoved = 0;
            long totalSkipped = 0;
            for (FieldMapping mapping : mappings) {
                long[] result = moveField(context, mapping);
                totalMoved += result[0];
                totalSkipped += result[1];
                handler.logInfo("Field " + mapping.sourceCanonical + " -> " + mapping.targetCanonical
                    + ": moved=" + result[0] + ", skipped(duplicates)=" + result[1]);
            }

            context.complete();

            handler.logInfo("=== Move Complete ===");
            handler.logInfo("Total moved: " + totalMoved);
            handler.logInfo("Total skipped (exact duplicates): " + totalSkipped);
            handler.logInfo("Reindex required: run 'dspace index-discovery -b' to refresh the search index.");
        } catch (SQLException e) {
            handler.logError("Database error during metadata field move: " + e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Resolve the configured source regex into concrete source/target field pairs.
     *
     * <p>Aborts the context and throws {@link IllegalArgumentException} if any resolved target field
     * is not registered, or if two distinct source fields resolve to the same target field.</p>
     *
     * @param context the DSpace context
     * @return the resolved field mappings (possibly empty if nothing matches)
     * @throws SQLException if a database error occurs
     */
    private List<FieldMapping> resolveMappings(Context context) throws SQLException {
        Pattern pattern;
        try {
            pattern = Pattern.compile(sourceRegex);
        } catch (PatternSyntaxException e) {
            context.abort();
            handler.logError("Invalid source regex: " + e.getMessage());
            throw new IllegalArgumentException("Invalid source regex: " + sourceRegex, e);
        }

        List<FieldMapping> mappings = new ArrayList<>();
        List<String> missingTargets = new ArrayList<>();
        Map<Integer, List<String>> sourcesByTarget = new HashMap<>();

        for (MetadataField field : metadataFieldService.findAll(context)) {
            String canonical = toCanonical(field);
            if (!pattern.matcher(canonical).matches()) {
                continue;
            }
            String targetString = canonical.replaceFirst(sourceRegex, targetTemplate);
            if (targetString.equals(canonical)) {
                // Source resolves to itself: nothing to move.
                continue;
            }
            if (!isValidFieldName(targetString)) {
                missingTargets.add(canonical + " -> " + targetString + " (invalid field name)");
                continue;
            }
            MetadataField target = metadataFieldService.findByString(context, targetString, '.');
            if (target == null) {
                missingTargets.add(canonical + " -> " + targetString + " (NOT REGISTERED)");
                continue;
            }
            mappings.add(new FieldMapping(field, target, canonical, toCanonical(target)));
            sourcesByTarget.computeIfAbsent(target.getID(), k -> new ArrayList<>()).add(canonical);
        }

        if (!missingTargets.isEmpty()) {
            handler.logError("Target metadata field(s) not registered; register them first, then re-run:");
            for (String missing : missingTargets) {
                handler.logError("  " + missing);
            }
            context.abort();
            throw new IllegalArgumentException(
                "Target metadata field(s) not registered: " + String.join("; ", missingTargets));
        }

        List<String> collisions = new ArrayList<>();
        for (Map.Entry<Integer, List<String>> entry : sourcesByTarget.entrySet()) {
            if (entry.getValue().size() > 1) {
                collisions.add("target field id " + entry.getKey() + " <- " + String.join(", ", entry.getValue()));
            }
        }
        if (!collisions.isEmpty()) {
            handler.logError("Multiple source fields resolve to the same target field:");
            for (String collision : collisions) {
                handler.logError("  " + collision);
            }
            context.abort();
            throw new IllegalArgumentException(
                "Multiple source fields map to the same target: " + String.join("; ", collisions));
        }

        return mappings;
    }

    /**
     * Move every value of a single source field to its target field, paging by object with stable
     * keyset (seek) pagination and committing per batch.
     *
     * @param context the DSpace context
     * @param mapping the source/target field pair
     * @return a two-element array: {@code [movedCount, skippedDuplicateCount]}
     * @throws SQLException if a database error occurs
     */
    private long[] moveField(Context context, FieldMapping mapping) throws SQLException {
        long moved = 0;
        long skipped = 0;
        UUID after = null;
        int batchNumber = 0;

        while (true) {
            List<UUID> objectIds = metadataValueService.findObjectIdsByField(context, mapping.source, after, batchSize);
            if (objectIds.isEmpty()) {
                break;
            }
            batchNumber++;
            // Advance the cursor past the highest object id in this page. Processed objects lose the
            // source field, so this seek guarantees forward progress with no skips or repeats.
            after = objectIds.get(objectIds.size() - 1);

            List<MetadataValue> sources = metadataValueService.findByFieldAndObjects(context, mapping.source,
                objectIds);
            List<MetadataValue> targets = metadataValueService.findByFieldAndObjects(context, mapping.target,
                objectIds);

            Map<UUID, Set<String>> keysByObject = new HashMap<>();
            Map<UUID, Integer> maxPlaceByObject = new HashMap<>();
            Set<UUID> populatedObjects = new HashSet<>();
            for (MetadataValue target : targets) {
                UUID oid = target.getDSpaceObject().getID();
                populatedObjects.add(oid);
                keysByObject.computeIfAbsent(oid, k -> new HashSet<>()).add(dedupeKey(target));
                maxPlaceByObject.merge(oid, target.getPlace(), Math::max);
            }

            List<Integer> duplicateIds = new ArrayList<>();
            int batchMoved = 0;
            int batchSkipped = 0;
            for (MetadataValue source : sources) {
                UUID oid = source.getDSpaceObject().getID();
                String key = dedupeKey(source);
                Set<String> keys = keysByObject.computeIfAbsent(oid, k -> new HashSet<>());

                if (keys.contains(key)) {
                    // Exact duplicate of an existing/already-moved target value: drop it.
                    duplicateIds.add(source.getID());
                    skipped++;
                    batchSkipped++;
                    continue;
                }

                if (populatedObjects.contains(oid)) {
                    // Append after the current highest target place for this object.
                    source.setPlace(maxPlaceByObject.merge(oid, 1, Integer::sum));
                }
                source.setMetadataField(mapping.target);
                metadataValueService.update(context, source);
                keys.add(key);
                moved++;
                batchMoved++;
            }

            // Bulk-delete duplicates with a single statement. Deleting MetadataValue entities directly
            // through the session would force Hibernate to load their owning object (Item.metadata is an
            // orphan-removal collection) and then refuse the flush; a bulk delete bypasses that.
            if (!duplicateIds.isEmpty()) {
                metadataValueService.deleteByIds(context, duplicateIds);
            }

            context.commit();

            handler.logInfo("Batch " + batchNumber + " (" + mapping.sourceCanonical + " -> "
                + mapping.targetCanonical + "): processed " + objectIds.size() + " object(s), moved "
                + batchMoved + ", skipped duplicates " + batchSkipped + ", deleted duplicates "
                + duplicateIds.size());

            uncache(context, sources);
            uncache(context, targets);
        }

        return new long[] {moved, skipped};
    }

    /**
     * Best-effort uncache of the given values. {@code context.commit()} flushes the session but does
     * not clear the Hibernate cache, so without this the cache grows unbounded on large repositories.
     *
     * @param context the DSpace context
     * @param values  the values to evict from the session cache
     */
    private void uncache(Context context, List<MetadataValue> values) {
        for (MetadataValue value : values) {
            try {
                context.uncacheEntity(value);
            } catch (Exception e) {
                handler.logError("Error uncaching metadata value id=" + value.getID() + ": " + e.getMessage(), e);
            }
        }
    }

    /**
     * Build the canonical {@code schema.element[.qualifier]} name for a field.
     *
     * @param field the metadata field
     * @return the canonical field name
     */
    private static String toCanonical(MetadataField field) {
        String canonical = field.getMetadataSchema().getName() + "." + field.getElement();
        if (field.getQualifier() != null) {
            canonical += "." + field.getQualifier();
        }
        return canonical;
    }

    /**
     * @param name a candidate metadata field name
     * @return {@code true} if it parses into 2 or 3 non-empty dot-separated tokens
     */
    private static boolean isValidFieldName(String name) {
        String[] parts = name.split("\\.");
        if (parts.length < 2 || parts.length > 3) {
            return false;
        }
        for (String part : parts) {
            if (part.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Build the deduplication key for a value from its text value, authority and language.
     *
     * @param value the metadata value
     * @return the null-safe deduplication key
     */
    private static String dedupeKey(MetadataValue value) {
        return token(value.getValue()) + KEY_DELIMITER + token(value.getAuthority())
            + KEY_DELIMITER + token(value.getLanguage());
    }

    private static String token(String value) {
        return value == null ? NULL_TOKEN : value;
    }

    @Override
    @SuppressWarnings("unchecked")
    public MetadataFieldMoveScriptConfiguration getScriptConfiguration() {
        return new DSpace().getServiceManager().getServiceByName(
            "metadata-field-move", MetadataFieldMoveScriptConfiguration.class);
    }

    /**
     * Immutable resolved source-to-target field pair.
     */
    private static final class FieldMapping {
        private final MetadataField source;
        private final MetadataField target;
        private final String sourceCanonical;
        private final String targetCanonical;

        private FieldMapping(MetadataField source, MetadataField target, String sourceCanonical,
                             String targetCanonical) {
            this.source = source;
            this.target = target;
            this.sourceCanonical = sourceCanonical;
            this.targetCanonical = targetCanonical;
        }
    }
}
