/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model.hateoas;

import org.dspace.app.rest.model.PreviewContentRest;
import org.dspace.app.rest.model.hateoas.annotations.RelNameDSpaceResource;
import org.dspace.app.rest.utils.Utils;

@RelNameDSpaceResource(PreviewContentRest.NAME)
public class PreviewContentResource extends DSpaceResource<PreviewContentRest> {
    public PreviewContentResource(PreviewContentRest ms, Utils utils) {
        super(ms, utils);
    }
}
