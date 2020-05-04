/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model.hateoas;

import org.dspace.app.rest.model.PropertyRest;
import org.dspace.app.rest.utils.Utils;

public class PropertyResource extends DSpaceResource<PropertyRest> {

    public PropertyResource(PropertyRest data, Utils utils) {
        super(data, utils);
    }
}
