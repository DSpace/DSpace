/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import javax.annotation.Nonnull;

/**
 * Simple immutable holder for the name of a metadata field.
 *
 * @author mwood
 */
public class MetadataFieldName {
    /** Name of the metadata schema which defines this field.  Never null. */
    public final String SCHEMA;

    /** Element name of this field.  Never null. */
    public final String ELEMENT;

    /** Qualifier name of this field.  May be {@code null}. */
    public final String QUALIFIER;

    /**
     * Initialize a tuple of (schema, element, qualifier) to name a metadata field.
     * @param schema name (not URI) of the schema.  Cannot be null.
     * @param element element name of the field.  Cannot be null.
     * @param qualifier qualifier name of the field.
     */
    public MetadataFieldName(@Nonnull String schema, @Nonnull String element, String qualifier) {
        if (null == schema) {
            throw new IllegalArgumentException("Schema must not be null.");
        }

        if (null == element) {
            throw new IllegalArgumentException("Element must not be null.");
        }

        SCHEMA = schema;
        ELEMENT = element;
        QUALIFIER = qualifier;
    }

    /**
     * Initialize a tuple of (schema, element, qualifier=null) to name a metadata field.
     * @param schema name (not URI) of the schema.  Cannot be null.
     * @param element element name of the field.  Cannot be null.
     */
    public MetadataFieldName(@Nonnull String schema, @Nonnull String element) {
        if (null == schema) {
            throw new IllegalArgumentException("Schema must not be null.");
        }

        if (null == element) {
            throw new IllegalArgumentException("Element must not be null.");
        }

        SCHEMA = schema;
        ELEMENT = element;
        QUALIFIER = null;
    }

    /**
     * Initialize a tuple of (schema, element, qualifier) to name a metadata field.
     * @param schema name (not URI) of the schema.  Cannot be null.
     * @param element element name of the field.  Cannot be null.
     * @param qualifier qualifier name of the field.
     */
    public MetadataFieldName(@Nonnull MetadataSchemaEnum schema, @Nonnull String element, String qualifier) {
        if (null == schema) {
            throw new IllegalArgumentException("Schema must not be null.");
        }

        if (null == element) {
            throw new IllegalArgumentException("Element must not be null.");
        }

        SCHEMA = schema.getName();
        ELEMENT = element;
        QUALIFIER = qualifier;
    }

    /**
     * Initialize a tuple of (schema, element, qualifier=null) to name a metadata field.
     * @param schema name (not URI) of the schema.  Cannot be null.
     * @param element element name of the field.  Cannot be null.
     */
    public MetadataFieldName(@Nonnull MetadataSchemaEnum schema, @Nonnull String element) {
        if (null == schema) {
            throw new IllegalArgumentException("Schema must not be null.");
        }

        if (null == element) {
            throw new IllegalArgumentException("Element must not be null.");
        }

        SCHEMA = schema.getName();
        ELEMENT = element;
        QUALIFIER = null;
    }
}
