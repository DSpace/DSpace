/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.external.provider.metadata.service;

import java.util.List;

import org.dspace.content.InProgressSubmission;
import org.dspace.external.provider.metadata.MetadataSuggestionProvider;

/**
 * This is the interface for the MetadataSuggestionService implementation
 */
public interface MetadataSuggestionProviderService {

    /**
     * This method will return a list of MetadataSuggestionProvider objects that support the given InProgressSubmission
     * @param inProgressSubmission  The given InProgressSubmission
     * @return                      The list of MetadataSuggestionProvider objects that support the given parameter
     */
    List<MetadataSuggestionProvider> getMetadataSuggestionProviders(InProgressSubmission inProgressSubmission);

    /**
     * This method will return the MetadataSuggestionProvider that has the given id
     * @param id    The id of the MetadataSuggestionProvider that is to be returned
     * @return      The MetadataSuggestionProvider that has the given id as its actual id
     */
    MetadataSuggestionProvider getMetadataSuggestionProvider(String id);

}
