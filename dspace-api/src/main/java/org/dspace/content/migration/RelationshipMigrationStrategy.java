/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.migration;

import java.sql.SQLException;

import org.dspace.content.Relationship;
import org.dspace.core.Context;
import org.dspace.scripts.handler.DSpaceRunnableHandler;

/**
 * Strategy interface for migrating relationship types to the CRIS authority
 * model. Implementations are discovered by the migration script via Spring
 * bean lookup and matched to a relationship type by its leftward/rightward
 * type labels ({@link #getLeftwardType()} / {@link #getRightwardType()}).
 *
 * <p>Matching by label rather than by numeric ID keeps the configuration stable
 * across installations: relationship type primary keys are assigned by the
 * database and vary per environment, whereas the leftward/rightward labels are
 * defined by the entity model and are deterministic.</p>
 *
 * <p>The recommended implementation is
 * {@link RelationshipTypeMigrationDefinition}, which composes the leftward and
 * rightward type labels with up to two directional migration definitions
 * ({@link DirectionalMigrationDefinition}) — one leftward and one rightward,
 * each optional. This replaces the old class-per-relationship-type pattern
 * with a single config-driven class.</p>
 *
 * <p>To configure a new relationship type for migration, register a
 * {@code RelationshipTypeMigrationDefinition} bean in
 * {@code config/spring/api/scripts.xml} with the appropriate
 * {@code leftwardType}/{@code rightwardType} labels and at least one directional
 * definition.</p>
 *
 * @author Adamo Fapohunda (adamo.fapohunda at 4science.com)
 */
public interface RelationshipMigrationStrategy {

    /**
     * Returns the leftward type label of the relationship type this strategy handles.
     * This label matches {@link org.dspace.content.RelationshipType#getLeftwardType()}
     * and is used by the migration script to match strategies to relationship types.
     *
     * @return the leftward type label (never null)
     */
    String getLeftwardType();

    /**
     * Returns the rightward type label of the relationship type this strategy handles.
     * This label matches {@link org.dspace.content.RelationshipType#getRightwardType()}
     * and is used by the migration script to match strategies to relationship types.
     *
     * @return the rightward type label (never null)
     */
    String getRightwardType();

    /**
     * Returns a human-readable description of this migration strategy, used in
     * log messages during migration. Implementations should return a
     * non-null, non-blank string; if no description is configured, a default
     * derived from the type labels is acceptable.
     *
     * @return a human-readable description (never null)
     */
    String getDescription();

    /**
     * Migrate a single relationship to authority-based metadata.
     * The implementation should:
     * <ul>
     *   <li>Identify the owner item (where metadata will be written)</li>
     *   <li>Derive the display value from the related item</li>
     *   <li>Set the authority to the related item's UUID</li>
     *   <li>Use CF_ACCEPTED as confidence</li>
     *   <li>Preserve the place (ordering) from the relationship</li>
     * </ul>
     *
     * @param context the DSpace context
     * @param relationship the relationship to migrate
     * @param handler the script handler for logging
     * @throws SQLException if a database error occurs
     */
    void migrate(Context context, Relationship relationship, DSpaceRunnableHandler handler) throws SQLException;
}
