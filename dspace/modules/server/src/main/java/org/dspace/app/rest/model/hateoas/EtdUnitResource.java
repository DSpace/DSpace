package org.dspace.app.rest.model.hateoas;

import org.dspace.app.rest.model.EtdUnitRest;
import org.dspace.app.rest.model.hateoas.annotations.RelNameDSpaceResource;
import org.dspace.app.rest.utils.Utils;

/**
 * Unit Rest HAL Resource. The HAL Resource wraps the REST Resource
 * adding support for the links and embedded resources
 */
@RelNameDSpaceResource(EtdUnitRest.NAME)
public class EtdUnitResource extends DSpaceResource<EtdUnitRest> {
    public EtdUnitResource(EtdUnitRest etdunit, Utils utils) {
        super(etdunit, utils);
    }
}
