/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.migration;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.cli.ParseException;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.content.Relationship;
import org.dspace.content.RelationshipType;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.RelationshipService;
import org.dspace.content.service.RelationshipTypeService;
import org.dspace.core.Context;
import org.dspace.scripts.DSpaceRunnable;
import org.dspace.utils.DSpace;

/**
 * DSpace script that migrates relationships to the CRIS authority model.
 *
 * <p>Migration is driven by {@link RelationshipTypeMigrationDefinition} beans
 * registered in Spring XML config. Each definition binds a relationship type
 * (by its leftward/rightward labels) to up to two optional
 * {@link DirectionalMigrationDefinition} beans (leftward and/or rightward).
 * No per-relationship-type Java classes are needed — everything is configured
 * via Spring.</p>
 *
 * <p>For each relationship of a matched type, this script executes all
 * configured directional definitions:</p>
 * <ol>
 *   <li>Reads the relationship data (items, place, value)</li>
 *   <li>Adds authority-based metadata on the owner item (UUID as authority,
 *       CF_ACCEPTED)</li>
 *   <li>Preserves the place/ordering from the relationship</li>
 *   <li>Optionally deletes the relationship after successful migration</li>
 * </ol>
 *
 * <p>Usage:</p>
 * <pre>
 *   dspace relationship-to-authority-migrate [-t &lt;relationship-type-id&gt;] [-d] [-n] [-b &lt;batch-size&gt;]
 * </pre>
 *
 * <p>Options:</p>
 * <ul>
 *   <li>{@code -t}: (optional) the relationship type ID to migrate. When omitted,
 *       every relationship type that has a configured migration definition is
 *       migrated.</li>
 *   <li>{@code -d}: (optional) delete relationships after migration</li>
 *   <li>{@code -n}: (optional) dry-run mode, do not commit changes</li>
 * <li>{@code -b}: (optional) number of relationships to process per batch
 *       (default {@value #DEFAULT_BATCH_SIZE})</li>
 * </ul>
 *
 * <p>The {@code -d} (delete) option is <strong>destructive</strong>: relationships
 * are permanently removed with {@code forceDelete}. This bypasses standard
 * authorization checks because the script operates with the authorization
 * system turned off. Use with caution and only after verifying the migration
 * output in a dry-run ({@code -n}).</p>
 *
 * @author Adamo Fapohunda (adamo.fapohunda at 4science.com)
 * */
public class RelationshipToAuthorityMigrationScript
    extends DSpaceRunnable<RelationshipToAuthorityMigrationScriptConfiguration> {

    /**
     * Default batch size (1000 relationships per database page).
     * Chosen to balance memory usage against database round-trips:
     * each batch loads related items into the Hibernate session and
     * uncaches them afterwards to prevent OutOfMemoryError on large
     * datasets. 1000 is a safe default that works well with H2
     * (tests) and PostgreSQL (production).
     */
    protected static final int DEFAULT_BATCH_SIZE = 1000;

    /** Separator used to build composite strategy lookup keys. */
    private static final String STRATEGY_KEY_SEPARATOR = "::";

    private boolean migrateAll;
    private int relationshipTypeId;
    private boolean deleteAfterMigration;
    private boolean dryRun;
    private int batchSize = DEFAULT_BATCH_SIZE;

    private RelationshipService relationshipService;
    private RelationshipTypeService relationshipTypeService;
    private Map<String, RelationshipMigrationStrategy> strategies;

    private volatile RelationshipToAuthorityMigrationScriptConfiguration scriptConfiguration;

    @Override
    public void setup() throws ParseException {
        if (commandLine.hasOption('h')) {
            printHelp();
            handler.logInfo("Help displayed");
            return;
        }

        // -t is optional: when omitted, all configured relationship types are migrated.
        if (commandLine.hasOption('t')) {
            try {
                relationshipTypeId = Integer.parseInt(commandLine.getOptionValue('t'));
                migrateAll = false;
            } catch (NumberFormatException e) {
                throw new ParseException("Option -t must be a valid integer: " + commandLine.getOptionValue('t'));
            }
        } else {
            migrateAll = true;
        }

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

        deleteAfterMigration = commandLine.hasOption('d');
        dryRun = commandLine.hasOption('n');
    }

    @Override
    public void internalRun() throws Exception {
        if (commandLine.hasOption('h')) {
            return;
        }

        // Initialize services
        DSpace dspace = new DSpace();
        relationshipService = ContentServiceFactory.getInstance().getRelationshipService();
        relationshipTypeService = ContentServiceFactory.getInstance().getRelationshipTypeService();

        // Load configured type definitions (each implements RelationshipMigrationStrategy),
        // indexed by a composite key built from the leftward/rightward type labels.
        List<RelationshipMigrationStrategy> strategyBeans = dspace.getServiceManager()
            .getServicesByType(RelationshipMigrationStrategy.class);
        strategies = strategyBeans.stream()
            .collect(Collectors.toMap(
                s -> strategyKey(s.getLeftwardType(), s.getRightwardType()), Function.identity(),
                (existing, duplicate) -> {
                    String message = "Multiple migration strategies registered for relationship type "
                        + existing.getLeftwardType() + "/" + existing.getRightwardType()
                        + ". Each relationship type must map to exactly one strategy;"
                        + " check the Spring configuration for duplicates.";
                    handler.logError(message);
                    throw new IllegalStateException(message);
                }));

        handler.logInfo("=== Relationship to Authority Migration ===");
        handler.logInfo(migrateAll
            ? "Mode: ALL configured relationship types"
            : "Mode: single relationship type (ID " + relationshipTypeId + ")");
        handler.logInfo("Delete after migration: " + deleteAfterMigration);
        handler.logInfo("Dry run: " + dryRun);
        handler.logInfo("Batch size: " + batchSize);

        Context context = new Context(Context.Mode.READ_WRITE);
        boolean succeeded = false;
        try {
            context.turnOffAuthorisationSystem();

            MigrationResult totals = new MigrationResult();
            int typesProcessed;

            if (migrateAll) {
                typesProcessed = migrateAllTypes(context, batchSize, totals);
                if (typesProcessed == 0) {
                    handler.logWarning("No relationship type in the database matches any configured migration "
                        + "definition. Configured definitions: " + describeConfiguredStrategies());
                }
            } else {
                migrateSingleType(context, batchSize, totals);
                typesProcessed = 1;
            }

            if (dryRun) {
                context.abort();
                handler.logInfo("DRY RUN: no changes committed.");
            } else {
                context.complete();
                succeeded = true;
            }

            handler.logInfo("=== Migration Complete ===");
            handler.logInfo("Relationship types processed: " + typesProcessed);
            handler.logInfo("Total migrated: " + totals.migrated.get());
            handler.logInfo("Total errors: " + totals.errors.get());

        } catch (SQLException e) {
            handler.logError("Database error during migration: " + e.getMessage());
            throw e;
        } finally {
            if (!succeeded && !dryRun) {
                // Ensure abort is called if complete() was never reached
                // (e.g. exception from migrateAllTypes/migrateSingleType before dryRun check)
                context.abort();
            }
            context.restoreAuthSystemState();
        }
    }

    /**
     * Migrate every relationship type in the database that has a configured
     * migration definition (matched by leftward/rightward labels).
     *
     * @param context the DSpace context
     * @param batchSize the relationship paging batch size
     * @param totals accumulator updated with migrated/error counts
     * @return the number of relationship types processed
     * @throws SQLException if a database error occurs
     * @throws AuthorizeException if an authorization error occurs
     */
    private int migrateAllTypes(Context context, int batchSize, MigrationResult totals)
        throws SQLException, AuthorizeException {
        int typesProcessed = 0;
        for (RelationshipType relationshipType : relationshipTypeService.findAll(context)) {
            RelationshipMigrationStrategy strategy = strategies.get(
                strategyKey(relationshipType.getLeftwardType(), relationshipType.getRightwardType()));
            if (strategy == null) {
                continue;
            }
            migrateRelationshipType(context, relationshipType, strategy, batchSize, totals);
            typesProcessed++;
        }
        return typesProcessed;
    }

    /**
     * Resolve the relationship type given by the {@code -t} option and migrate it.
     *
     * @param context the DSpace context
     * @param batchSize the relationship paging batch size
     * @param totals accumulator updated with migrated/error counts
     * @throws SQLException if a database error occurs
     * @throws AuthorizeException if an authorization error occurs
     */
    private void migrateSingleType(Context context, int batchSize, MigrationResult totals)
        throws SQLException, AuthorizeException {
        RelationshipType relationshipType = relationshipTypeService.find(context, relationshipTypeId);
        if (relationshipType == null) {
            context.abort();
            handler.logError("RelationshipType with ID " + relationshipTypeId + " not found in database");
            throw new IllegalArgumentException("RelationshipType not found: " + relationshipTypeId);
        }

        // Match a migration definition by the relationship type's leftward/rightward labels.
        // Matching by label (rather than numeric ID) keeps configuration stable across installs.
        RelationshipMigrationStrategy strategy = strategies.get(
            strategyKey(relationshipType.getLeftwardType(), relationshipType.getRightwardType()));
        if (strategy == null) {
            context.abort();
            handler.logError("No migration definition found for relationship type "
                + relationshipType.getLeftwardType() + "/" + relationshipType.getRightwardType()
                + " (ID " + relationshipTypeId + ")");
            handler.logError("Available relationship types: " + describeConfiguredStrategies());
            throw new IllegalArgumentException("No migration definition registered for relationship type "
                + relationshipType.getLeftwardType() + "/" + relationshipType.getRightwardType());
        }

        migrateRelationshipType(context, relationshipType, strategy, batchSize, totals);
    }

    /**
     * Migrate all relationships of a single relationship type, paging in batches
     * and committing per batch (unless in dry-run mode).
     *
     * @param context the DSpace context
     * @param relationshipType the relationship type to migrate
     * @param strategy the migration strategy matched to the type
     * @param batchSize the relationship paging batch size
     * @param totals accumulator updated with migrated/error counts
     * @throws SQLException if a database error occurs
     * @throws AuthorizeException if an authorization error occurs
     */
    private void migrateRelationshipType(Context context, RelationshipType relationshipType,
                                         RelationshipMigrationStrategy strategy, int batchSize,
                                         MigrationResult totals) throws SQLException, AuthorizeException {

        int total = relationshipService.countByRelationshipType(context, relationshipType);
        handler.logInfo("--- " + strategy.getDescription() + " ["
            + relationshipType.getLeftwardType() + "/" + relationshipType.getRightwardType()
            + ", ID " + relationshipType.getID() + "]: " + total + " relationship(s) ---");

        if (total == 0) {
            return;
        }

        // Keyset (seek) pagination by ascending relationship id. Unlike LIMIT/OFFSET, this is
        // stable and gap/duplicate-free across batches even though we commit (and optionally
        // delete) between pages — so no relationship is processed twice or skipped.
        int lastId = 0;

        while (true) {
            List<Relationship> batch = relationshipService.findByRelationshipTypeAfterId(
                context, relationshipType, batchSize, lastId);

            if (batch.isEmpty()) {
                break;
            }

            // Advance the cursor to the highest id in this page (the list is ordered by
            // ascending id) before the entities are committed/uncached.
            lastId = batch.getLast().getID();

            for (Relationship relationship : batch) {
                try {
                    strategy.migrate(context, relationship, handler);

                    if (deleteAfterMigration && !dryRun) {
                        // forceDelete bypasses authorization checks. This is intentional:
                        // the script runs with auth disabled (turnOffAuthorisationSystem)
                        // and the -d option is a deliberate, documented destructive action.
                        relationshipService.forceDelete(context, relationship, false, false);
                    }

                    totals.migrated.incrementAndGet();
                } catch (RuntimeException e) {
                    totals.errors.incrementAndGet();
                    handler.logError("Error migrating relationship id=" + relationship.getID()
                        + ": " + e.getMessage(), e);
                }
            }

            if (!dryRun) {
                context.commit();
            }

            // Uncache the entities touched in this batch. context.commit() flushes the
            // session but does not clear the Hibernate cache, so without this the cache
            // grows unbounded and large repositories can hit OutOfMemoryError. Uncaching
            // is best-effort: a relationship deleted by deleteAfterMigration may already
            // be detached, so any failure here is logged and must not abort the run.
            for (Relationship relationship : batch) {
                try {
                    Item leftItem = relationship.getLeftItem();
                    if (leftItem != null) {
                        context.uncacheEntity(leftItem);
                    }
                    Item rightItem = relationship.getRightItem();
                    if (rightItem != null) {
                        context.uncacheEntity(rightItem);
                    }
                    context.uncacheEntity(relationship);
                } catch (RuntimeException e) {
                    handler.logError("Error uncaching relationship id=" + relationship.getID()
                        + ": " + e.getMessage(), e);
                }
            }

            handler.logInfo("Progress: " + totals.migrated.get() + " migrated, " + totals.errors.get() + " errors");
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public RelationshipToAuthorityMigrationScriptConfiguration getScriptConfiguration() {
        if (scriptConfiguration == null) {
            scriptConfiguration = new DSpace().getServiceManager().getServiceByName(
                "relationship-to-authority-migrate",
                RelationshipToAuthorityMigrationScriptConfiguration.class);
        }
        return scriptConfiguration;
    }

    /**
     * Build the composite lookup key that uniquely identifies a relationship
     * type by its leftward and rightward type labels, separated by {@code ::}.
     *
     * @param leftwardType the leftward type label
     * @param rightwardType the rightward type label
     * @return the composite key
     */
    private static String strategyKey(String leftwardType, String rightwardType) {
        return leftwardType + STRATEGY_KEY_SEPARATOR + rightwardType;
    }

    /**
     * @return a human-readable, comma-separated list of the configured
     *         relationship types (leftward/rightward labels), for logging.
     */
    private String describeConfiguredStrategies() {
        return strategies.values().stream()
            .map(s -> s.getLeftwardType() + "/" + s.getRightwardType())
            .collect(Collectors.joining(", "));
    }

    /**
     * Mutable accumulator for migrated/error counts across one or more
     * relationship types.
     */
    private static final class MigrationResult {
        private final AtomicInteger migrated = new AtomicInteger(0);
        private final AtomicInteger errors = new AtomicInteger(0);
    }
}
