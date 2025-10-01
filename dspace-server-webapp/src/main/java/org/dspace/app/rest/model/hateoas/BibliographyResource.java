/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model.hateoas;

import org.dspace.app.rest.model.BibliographyRest;
import org.dspace.app.rest.model.hateoas.annotations.RelNameDSpaceResource;
import org.dspace.app.rest.utils.Utils;

/**
 * Boilerplate hateos resource for IdentifiersRest
 *
 * @author Jesiel Viana
 */
@RelNameDSpaceResource(BibliographyRest.NAME)
public class BibliographyResource extends HALResource<BibliographyRest> {
    public BibliographyResource(BibliographyRest data, Utils utils) {
        super(data);
    }
}
