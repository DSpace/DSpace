/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.external.provider.metadata.impl;

import org.dspace.content.Bitstream;
import org.dspace.content.InProgressSubmission;
import org.dspace.external.provider.impl.MockDataProvider;
import org.dspace.external.provider.metadata.MetadataSuggestionProvider;

public class MockMetadataSuggestionProvider extends MetadataSuggestionProvider<MockDataProvider> {
    public boolean supports(InProgressSubmission inProgressSubmission, String query,
                            Bitstream bitstream, boolean useMetadata) {
        return false;
    }
}
