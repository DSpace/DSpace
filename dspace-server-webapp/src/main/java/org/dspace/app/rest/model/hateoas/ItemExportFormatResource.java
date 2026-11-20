/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model.hateoas;

import org.dspace.app.rest.model.ItemExportFormatRest;
import org.dspace.app.rest.model.hateoas.annotations.RelNameDSpaceResource;
import org.dspace.app.rest.utils.Utils;

/**
 * This class serves as a wrapper class to wrap the ItemExportFormatRest into
 * a HAL resource.
 *
 * @author Alessandro Martelli (alessandro.martelli at 4science.it)
 *
 */
@RelNameDSpaceResource(ItemExportFormatRest.NAME)
public class ItemExportFormatResource extends DSpaceResource<ItemExportFormatRest> {

    public ItemExportFormatResource(ItemExportFormatRest data, Utils utils) {
        super(data, utils);
    }


}
