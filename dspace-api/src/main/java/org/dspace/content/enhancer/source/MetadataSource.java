/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.enhancer.source;

import java.util.List;

/**
 * Read port over a source of relationship-related metadata.
 *
 * <p>This is the only abstraction over an IO world in the relationship metadata generator design.
 * Implementations may read from the database or from a search index, while consumers (the generators)
 * stay IO-free and depend on this port only.</p>
 *
 * @author Adamo Fapohunda (adamo.fapohunda at 4science.com)
 */
public interface MetadataSource {

    /**
     * Read all the values of the given metadata field from the underlying source.
     *
     * @param field the metadata field in the {@code schema.element[.qualifier]} form
     * @return the values found for the field, never {@code null}, possibly empty
     */
    List<RawMetadataValue> getValues(String field);

}
