/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.enhancer.generator;

import java.util.List;

import org.dspace.content.dto.MetadataValueDTO;
import org.dspace.content.enhancer.source.MetadataSource;

/**
 * Generates the additional metadata values of a relationship from the related entity.
 *
 * <p>Implementations declare the source field they read, and receive its values through a
 * {@link MetadataSource}.</p>
 *
 * @author Adamo Fapohunda (adamo.fapohunda at 4science.com)
 */
public interface RelationshipMetadataGenerator {

    /**
     * The metadata field read on the related entity.
     *
     * @return the field in the {@code schema.element[.qualifier]} form
     */
    String getSourceField();

    /**
     * Extract the additional metadata values for this generator from the given source.
     *
     * @param source the source to read the values from
     * @return the extracted values, never {@code null}, possibly empty
     */
    List<MetadataValueDTO> extract(MetadataSource source);

}
