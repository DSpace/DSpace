/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model.hateoas;

import org.dspace.app.rest.model.VocabularyEntryRest;
import org.dspace.app.rest.model.hateoas.annotations.RelNameDSpaceResource;

/**
 * Vocabulary Entry Rest HAL Resource. The HAL Resource wraps the REST Resource
 * adding support for the links and embedded resources
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
@RelNameDSpaceResource(VocabularyEntryRest.NAME)
public class VocabularyEntryResource extends HALResource<VocabularyEntryRest> {
    public VocabularyEntryResource(VocabularyEntryRest sd) {
        super(sd);
    }
}
