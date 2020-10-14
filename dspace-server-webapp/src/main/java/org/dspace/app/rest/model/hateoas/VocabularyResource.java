/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model.hateoas;

import org.dspace.app.rest.model.VocabularyRest;
import org.dspace.app.rest.model.hateoas.annotations.RelNameDSpaceResource;
import org.dspace.app.rest.utils.Utils;

/**
 * Authority Rest HAL Resource. The HAL Resource wraps the REST Resource
 * adding support for the links and embedded resources
 *
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science.it)
 */
@RelNameDSpaceResource(VocabularyRest.NAME)
public class VocabularyResource extends DSpaceResource<VocabularyRest> {
    public VocabularyResource(VocabularyRest sd, Utils utils) {
        super(sd, utils);
        add(utils.linkToSubResource(sd, VocabularyRest.ENTRIES));
    }
}
