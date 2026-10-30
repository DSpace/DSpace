/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.migration;

import java.sql.SQLException;

import jakarta.annotation.PostConstruct;
import org.apache.commons.lang3.StringUtils;
import org.dspace.content.Relationship;
import org.dspace.core.Context;
import org.dspace.scripts.handler.DSpaceRunnableHandler;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Config-driven implementation of {@link RelationshipMigrationStrategy} that
 * composes the leftward/rightward type labels of a relationship type with up to
 * two optional directional migration definitions (leftward and/or rightward).
 *
 * <p>The relationship type is identified by its {@code leftwardType} and
 * {@code rightwardType} labels rather than by a numeric ID, so the same
 * configuration works across installations regardless of the database-assigned
 * primary keys.</p>
 *
 * <p>Each direction is independently configured via a
 * {@link DirectionalMigrationDefinition} bean. When both directions are
 * configured for a type, the migration script executes them both for each
 * relationship — writing metadata to the left item for the leftward definition,
 * and to the right item for the rightward definition.</p>
 *
 * <p>This bean replaces the old class-per-relationship-type approach
 * ({@code AuthorRelationshipMigrationStrategy},
 * {@code ProjectRelationshipMigrationStrategy}) with a single reusable
 * implementation driven entirely by Spring bean configuration.</p>
 *
 * <p>Example XML config (single direction, leftward):</p>
 * <pre>
 * &lt;bean id="authorLeftwardMigration"
 *       class="o.d.c.m.DirectionalMigrationDefinition"&gt;
 *     &lt;property name="ownerSide" value="LEFT"/&gt;
 *     &lt;property name="schema" value="dc"/&gt;
 *     &lt;property name="element" value="contributor"/&gt;
 *     &lt;property name="qualifier" value="author"/&gt;
 * &lt;/bean&gt;
 *
 * &lt;bean class="o.d.c.m.RelationshipTypeMigrationDefinition"&gt;
 *     &lt;property name="leftwardType" value="isAuthorOfPublication"/&gt;
 *     &lt;property name="rightwardType" value="isPublicationOfAuthor"/&gt;
 *     &lt;property name="description"
 *               value="isAuthorOfPublication (Publication &lt;- Person)"/&gt;
 *     &lt;property name="leftwardMigration" ref="authorLeftwardMigration"/&gt;
 * &lt;/bean&gt;
 * </pre>
 *
 * @author Adamo Fapohunda (adamo.fapohunda at 4science.com)
 */
public class RelationshipTypeMigrationDefinition implements RelationshipMigrationStrategy {

    private String leftwardType;
    private String rightwardType;
    private String description;
    private DirectionalMigrationDefinition leftwardMigration;
    private DirectionalMigrationDefinition rightwardMigration;

    @Autowired
    private DirectionalMetadataMigrator migrator;

    /**
     * Validates the bean configuration at Spring context startup. A migration
     * definition must identify its relationship type (both labels) and must
     * configure at least one direction; otherwise it would either be
     * impossible to match or would silently no-op for every relationship of its
     * type, masking a configuration error. Failing fast here surfaces the
     * misconfiguration before the migration script runs.
     *
     * @throws IllegalStateException if {@code leftwardType} or
     *                               {@code rightwardType} is blank, or if both
     *                               {@code leftwardMigration} and
     *                               {@code rightwardMigration} are {@code null}
     */
    @PostConstruct
    public void validateConfiguration() {
        if (StringUtils.isBlank(leftwardType) || StringUtils.isBlank(rightwardType)) {
            throw new IllegalStateException("RelationshipTypeMigrationDefinition must declare both a leftwardType "
                + "and a rightwardType label (got leftwardType='" + leftwardType
                + "', rightwardType='" + rightwardType + "').");
        }
        if (leftwardMigration == null && rightwardMigration == null) {
            throw new IllegalStateException("RelationshipTypeMigrationDefinition for relationship type '"
                + leftwardType + "'/'" + rightwardType + "' has neither a leftwardMigration nor a "
                + "rightwardMigration configured; at least one direction must be set.");
        }
    }

    @Override
    public String getLeftwardType() {
        return leftwardType;
    }

    public void setLeftwardType(String leftwardType) {
        this.leftwardType = leftwardType;
    }

    @Override
    public String getRightwardType() {
        return rightwardType;
    }

    public void setRightwardType(String rightwardType) {
        this.rightwardType = rightwardType;
    }

    /**
     * Returns the human-readable description of this migration definition.
     * When no description has been configured (null or blank), a sensible
     * default derived from the relationship type labels is returned so callers
     * (e.g. logging) never receive {@code null}.
     *
     * @return the configured description, or a default derived from the labels if none is set
     */
    @Override
    public String getDescription() {
        if (StringUtils.isBlank(description)) {
            return leftwardType + "/" + rightwardType + " migration";
        }
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public DirectionalMigrationDefinition getLeftwardMigration() {
        return leftwardMigration;
    }

    public void setLeftwardMigration(DirectionalMigrationDefinition leftwardMigration) {
        this.leftwardMigration = leftwardMigration;
    }

    public DirectionalMigrationDefinition getRightwardMigration() {
        return rightwardMigration;
    }

    public void setRightwardMigration(DirectionalMigrationDefinition rightwardMigration) {
        this.rightwardMigration = rightwardMigration;
    }

    @Override
    public void migrate(Context context, Relationship relationship,
                        DSpaceRunnableHandler handler) throws SQLException {

        if (leftwardMigration != null) {
            migrator.migrate(context, relationship, leftwardMigration, handler);
        }

        if (rightwardMigration != null) {
            migrator.migrate(context, relationship, rightwardMigration, handler);
        }
    }
}
