/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model.hateoas;

import org.dspace.app.rest.model.CorrectionTypeRest;
import org.dspace.app.rest.model.hateoas.annotations.RelNameDSpaceResource;
import org.dspace.app.rest.utils.Utils;

/**
 * CorrectionType Rest HAL Resource. The HAL Resource wraps the REST Resource
 * adding support for the links and embedded resources
 *
 * @author Mohamed Eskander (mohamed.eskander at 4science.com)
 */
@RelNameDSpaceResource(CorrectionTypeRest.NAME)
public class CorrectionTypeResource extends DSpaceResource<CorrectionTypeRest> {

    public CorrectionTypeResource(CorrectionTypeRest target, Utils utils) {
        super(target, utils);
    }

}
