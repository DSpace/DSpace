/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.external.provider.metadata.impl;

import org.dspace.content.InProgressSubmission;
import org.dspace.external.provider.impl.OrcidV2AuthorDataProvider;
import org.dspace.external.provider.metadata.MetadataSuggestionProvider;

/**
 * The implementation of the {@link MetadataSuggestionProvider} for the {@link OrcidV2AuthorDataProvider}
 */
public class OrcidV2AuthorMetadataSuggestionProvider extends MetadataSuggestionProvider<OrcidV2AuthorDataProvider> {

    @Override
    public boolean supports(InProgressSubmission inProgressSubmission) {
        return true;
    }
}
