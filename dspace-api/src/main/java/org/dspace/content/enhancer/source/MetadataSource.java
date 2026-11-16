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
 * Reads metadata values from a source, such as the database or the search index.
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
