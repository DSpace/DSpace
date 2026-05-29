/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authority.filler;

import org.dspace.content.MetadataValue;

/**
 * Service interface for obtaining the appropriate AuthorityImportFiller
 * based on metadata values. AuthorityImportFillers are used during item
 * submission to enrich items with additional data from external sources
 * or related entities.
 *
 * The service uses authority types to determine which filler to use.
 * Each filler is responsible for populating metadata on the target item
 * based on the authority value in the source metadata.
 */
public interface AuthorityImportFillerService {

    /**
     * The default AuthorityImportFiller used when no specific filler is configured
     * for an authority type. This filler handles internal submission metadata
     * and provides basic item enrichment functionality.
     */
    String SOURCE_INTERNAL = "INTERNAL-SUBMISSION";

    /**
     * Returns the AuthorityImportFiller appropriate for the given metadata value.
     * The filler is selected based on the authority type extracted from the
     * metadata's authority field (e.g., "will be generated::ORCID::...").
     *
     * @param metadata the metadata value containing the authority reference
     * @return the appropriate AuthorityImportFiller, or null if none found
     */
    AuthorityImportFiller getAuthorityImportFillerByMetadata(MetadataValue metadata);

}
