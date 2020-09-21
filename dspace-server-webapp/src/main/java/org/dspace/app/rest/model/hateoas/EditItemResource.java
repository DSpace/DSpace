/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model.hateoas;

import org.dspace.app.rest.model.EditItemRest;
import org.dspace.app.rest.utils.Utils;

/**
 * EditItem Rest HAL Resource. The HAL Resource wraps the REST Resource
 * adding support for the links and embedded resources
 * 
 * @author Danilo Di Nuzzo (danilo.dinuzzo at 4science.it)
 */
public class EditItemResource extends DSpaceResource<EditItemRest> {
    public EditItemResource(EditItemRest witem, Utils utils) {
        super(witem, utils);
    }
}
