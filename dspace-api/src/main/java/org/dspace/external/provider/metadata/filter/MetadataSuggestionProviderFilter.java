/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.external.provider.metadata.filter;

import org.dspace.content.InProgressSubmission;

/**
 * This class is used by {@link org.dspace.external.provider.metadata.MetadataSuggestionProvider} implementation
 * to indicate whether or not a given InProgressSubmission is supported by the provider
 */
public interface MetadataSuggestionProviderFilter {

    /**
     * This method defines whether a MetadataSuggestionProviderFilter supports a given InProgressSubmission
     * @param inProgressSubmission  The given InProgressSubmission
     * @return                      A boolean indicating whether this MetadataSuggestionProviderFilter supports
     *                              the given InProgressSubmission or not
     */
    boolean supports(InProgressSubmission inProgressSubmission);
}
