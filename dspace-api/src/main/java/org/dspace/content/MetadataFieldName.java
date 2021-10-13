/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import java.util.Arrays;
import javax.annotation.Nonnull;

/**
 * Simple immutable holder for the name of a metadata field.
 *
 * @author mwood
 */
public class MetadataFieldName {
    /** Name of the metadata schema which defines this field.  Never null. */
    public final String schema;

    /** Element name of this field.  Never null. */
    public final String element;

    /** Qualifier name of this field.  May be {@code null}. */
    public final String qualifier;

    /**
     * Initialize a tuple of (schema, element, qualifier) to name a metadata field.
     * @param schema name (not URI) of the schema.  Cannot be null.
     * @param element element name of the field.  Cannot be null.
     * @param qualifier qualifier name of the field.
     */
    public MetadataFieldName(@Nonnull String schema, @Nonnull String element, String qualifier) {
        if (null == schema) {
            throw new NullPointerException("Schema must not be null.");
        }

        if (null == element) {
            throw new NullPointerException("Element must not be null.");
        }

        this.schema = schema;
        this.element = element;
        this.qualifier = qualifier;
    }

    /**
     * Initialize a tuple of (schema, element, qualifier=null) to name a metadata field.
     * @param schema name (not URI) of the schema.  Cannot be null.
     * @param element element name of the field.  Cannot be null.
     */
    public MetadataFieldName(@Nonnull String schema, @Nonnull String element) {
        if (null == schema) {
            throw new NullPointerException("Schema must not be null.");
        }

        if (null == element) {
            throw new NullPointerException("Element must not be null.");
        }

        this.schema = schema;
        this.element = element;
        qualifier = null;
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

        this.schema = schema.getName();
        this.element = element;
        this.qualifier = qualifier;
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

        this.schema = schema.getName();
        this.element = element;
        qualifier = null;
    }

    /**
     * Initialize a tuple of (schema, element, qualifier) to name a metadata field.
     * @param name a dotted-triple {@code schema.element[.qualifier]}.  If the
     *             optional qualifier is omitted, it will be stored as {@code null}.
     */
    public MetadataFieldName(@Nonnull String name) {
        String[] elements = parse(name);
        schema = elements[0];
        element = elements[1];
        qualifier = elements[2];
    }

    /**
     * Split a dotted-triple field name {@code schema.element[.qualifier]} into
     * its components.
     * @param name the dotted-triple field name.
     * @return the components.  Always of size 3.  If the qualifier is omitted,
     *         the third element is {@code null}.
     * @throws IllegalArgumentException if there are not at least two components.
     * @throws NullPointerException if {@code name} is null.
     */
    public static String[] parse(@Nonnull String name) {
        if (null == name) {
            throw new NullPointerException("Name is null");
        }

        String[] elements = name.split("\\.", 3);
        if (elements.length < 2) {
            throw new IllegalArgumentException("Not enough elements:  " + name);
        }
        return Arrays.copyOf(elements, 3);
    }

    /**
     * Format a dotted-atoms representation of this field name.
     * @return schema.element.qualifier
     */
    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder(32);
        buffer.append(schema)
                .append('.')
                .append(element);
        if (null != qualifier) {
            buffer.append('.')
                    .append(qualifier);
        }
        return buffer.toString();
    }
}
