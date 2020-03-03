/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model.hateoas;

import org.dspace.app.rest.model.MetadataSuggestionsDifferencesRest;
import org.dspace.app.rest.model.hateoas.annotations.RelNameDSpaceResource;

/**
 * This is the HalResource for the {@link MetadataSuggestionsDifferencesRest} object
 */
@RelNameDSpaceResource(MetadataSuggestionsDifferencesRest.NAME)
public class MetadataSuggestionsDifferencesResource extends HALResource<MetadataSuggestionsDifferencesRest> {

    /**
     * This is the default constructor for a HalResource object
     * @param data  the {@link MetadataSuggestionsDifferencesRest} object to be used
     */
    public MetadataSuggestionsDifferencesResource(MetadataSuggestionsDifferencesRest data) {
        super(data);
    }
}
