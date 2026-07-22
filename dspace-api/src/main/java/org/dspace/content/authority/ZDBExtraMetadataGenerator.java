/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.authority;

import java.util.Map;

import org.dspace.authority.AuthorityValue;

/**
 * Strategy interface for generating extra metadata entries to attach to
 * ZDB authority choices.
 *
 * <p>Implementations extract specific metadata (e.g., ISSN, title) from a
 * {@link AuthorityValue} and return them as key-value pairs for use as
 * extras on {@link org.dspace.content.authority.Choice} objects.</p>
 *
 * @author Mykhaylo Boychuk (4science.it)
 */
public interface ZDBExtraMetadataGenerator {

    /**
     * Build extra metadata entries from the given authority value.
     *
     * @param val the {@link AuthorityValue} to extract metadata from
     * @return a map of extra metadata key-value pairs
     */
    public Map<String, String> build(AuthorityValue val);
}
