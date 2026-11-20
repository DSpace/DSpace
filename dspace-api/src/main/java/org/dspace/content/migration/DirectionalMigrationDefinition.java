/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.migration;

import jakarta.annotation.PostConstruct;
import org.apache.commons.lang3.StringUtils;

/**
 * Configuration bean describing a single directional migration of a
 * relationship type.
 *
 * <p>A relationship type links two sides (LEFT and RIGHT). This definition
 * specifies which side is the <em>owner</em> (where metadata is written) and
 * which is the <em>related</em> side (providing authority and fallback value).
 * Each relationship type can have zero, one, or two directional definitions
 * (leftward and/or rightward), each independently configured and optional.</p>
 *
 * <p>Example (Author: Publication LEFT, Person RIGHT, metadata on Publication):</p>
 * <pre>
 *   ownerSide = LEFT
 *   schema = dc
 *   element = contributor
 *   qualifier = author
 * </pre>
 *
 * <p>No relationship-type-specific subclassing is needed. The behaviour is
 * fully driven by the ownerSide and metadata field properties.</p>
 *
 * @author Adamo Fapohunda (adamo.fapohunda at 4science.com)
 */
public class DirectionalMigrationDefinition {

    /**
     * The side that owns the metadata: "LEFT" or "RIGHT".
     * <ul>
     *   <li>LEFT  – write metadata to relationship.getLeftItem(),
     *       use leftPlace and leftwardValue</li>
     *   <li>RIGHT – write metadata to relationship.getRightItem(),
     *       use rightPlace and rightwardValue</li>
     * </ul>
     */
    private String ownerSide;

    /** Metadata schema (e.g. "dc"). */
    private String schema;

    /** Metadata element (e.g. "contributor"). */
    private String element;

    /** Metadata qualifier (e.g. "author"), or null. */
    private String qualifier;

    public String getOwnerSide() {
        return ownerSide;
    }

    public void setOwnerSide(String ownerSide) {
        this.ownerSide = ownerSide;
    }

    public String getSchema() {
        return schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }

    public String getElement() {
        return element;
    }

    public void setElement(String element) {
        this.element = element;
    }

    public String getQualifier() {
        return qualifier;
    }

    public void setQualifier(String qualifier) {
        this.qualifier = qualifier;
    }

    /**
     * Validates the bean configuration at Spring context startup.
     *
     * @throws IllegalStateException if a required property is missing or invalid
     */
    @PostConstruct
    public void validateConfiguration() {
        if (StringUtils.isBlank(ownerSide)) {
            throw new IllegalStateException("DirectionalMigrationDefinition: ownerSide is required"
                + " (value \"LEFT\" or \"RIGHT\")");
        }
        if (!"LEFT".equalsIgnoreCase(ownerSide) && !"RIGHT".equalsIgnoreCase(ownerSide)) {
            throw new IllegalStateException("DirectionalMigrationDefinition: ownerSide must be"
                + " \"LEFT\" or \"RIGHT\" (case-insensitive), got: '" + ownerSide + "'");
        }
        if (StringUtils.isBlank(schema)) {
            throw new IllegalStateException("DirectionalMigrationDefinition: schema is required"
                + " (e.g. \"dc\")");
        }
        if (StringUtils.isBlank(element)) {
            throw new IllegalStateException("DirectionalMigrationDefinition: element is required"
                + " (e.g. \"contributor\")");
        }
    }
}
