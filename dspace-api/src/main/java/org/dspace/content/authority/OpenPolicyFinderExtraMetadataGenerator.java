/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.authority;

import java.util.Map;

import org.dspace.app.openpolicyfinder.v2.OpenPolicyFinderJournal;

/**
 * Strategy interface for generating extra metadata entries from an
 * {@link OpenPolicyFinderJournal}. Implementations are used by
 * {@link OpenPolicyFinderAuthority} to populate additional metadata
 * fields in authority choices (e.g. ISSN, publisher name).
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.com)
 */
public interface OpenPolicyFinderExtraMetadataGenerator {

    /**
     * Build extra metadata key-value pairs from the given journal.
     *
     * @param journal the Open Policy Finder journal entry
     * @return a map of extra metadata entries to include in the authority choice
     */
    Map<String, String> build(OpenPolicyFinderJournal journal);

}