/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model.hateoas;

import org.dspace.app.rest.model.MetadataSuggestionEntryRest;
import org.dspace.app.rest.model.hateoas.annotations.RelNameDSpaceResource;

/**
 * This class will act as a Resource class for {@link MetadataSuggestionEntryRest}
 */
@RelNameDSpaceResource(MetadataSuggestionEntryRest.NAME)
public class MetadataSuggestionEntryResource extends HALResource<MetadataSuggestionEntryRest> {

    /**
     * Default constructor for this Resource
     * It'll also embed a "changes" object constructed out of the MetadataChangeRest objects in the relevant content
     * @param content   The relevant REST object
     */
    public MetadataSuggestionEntryResource(MetadataSuggestionEntryRest content) {
        super(content);
    }
}
