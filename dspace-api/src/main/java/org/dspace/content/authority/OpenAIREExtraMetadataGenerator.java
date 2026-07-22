/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.authority;

import java.util.Map;

/**
 * Strategy interface for generating extra metadata entries to attach to
 * OpenAIRE project authority choices.
 *
 * <p>Implementations produce key-value pairs that are added as extras on
 * each {@link Choice} returned by {@link OpenAIREProjectAuthority}.</p>
 *
 * @author Mykhaylo Boychuk (4science.it)
 */
public interface OpenAIREExtraMetadataGenerator {

    /**
     * Build extra metadata entries for the given OpenAIRE value.
     *
     * @param value the OpenAIRE identifier or code to process
     * @return a map of extra metadata key-value pairs
     */
    Map<String, String> build(String value);

}