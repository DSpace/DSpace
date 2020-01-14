/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.external.provider.metadata.filter;

import org.dspace.content.InProgressSubmission;

public interface MetadataSuggestionProviderFilter {

    boolean supports(InProgressSubmission inProgressSubmission);
}
