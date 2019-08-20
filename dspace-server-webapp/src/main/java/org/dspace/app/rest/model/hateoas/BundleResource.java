package org.dspace.app.rest.model.hateoas;

import org.dspace.app.rest.model.BundleRest;
import org.dspace.app.rest.model.hateoas.annotations.RelNameDSpaceResource;
import org.dspace.app.rest.utils.Utils;

/**
 * Bunde Rest HAL Resource. The HAL Resource wraps the REST Resource
 * adding support for the links and embedded resources
 *
 * @author Jelle Pelgrims (jelle.pelgrims at atmire.com)
 */
@RelNameDSpaceResource(BundleRest.NAME)
public class BundleResource extends DSpaceResource<BundleRest>  {

    public BundleResource(BundleRest data, Utils utils, String... rels) {
        super(data, utils, rels);
    }
}
