/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model.hateoas;
import org.dspace.app.rest.model.BulkAccessConditionRest;
import org.dspace.app.rest.model.hateoas.annotations.RelNameDSpaceResource;
import org.dspace.app.rest.utils.Utils;

/**
 * BulkAccessCondition HAL Resource.
 * This resource adds the data from the REST object together with embedded objects
 * and a set of links if applicable.
 *
 * @author Mohamed Eskander (mohamed.eskander at 4science.it)
 */
@RelNameDSpaceResource(BulkAccessConditionRest.NAME)
public class BulkAccessConditionResource extends DSpaceResource<BulkAccessConditionRest> {

    public BulkAccessConditionResource(BulkAccessConditionRest data, Utils utils) {
        super(data, utils);
    }

}