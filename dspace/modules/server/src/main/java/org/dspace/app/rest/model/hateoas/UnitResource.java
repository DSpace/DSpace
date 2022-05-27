package org.dspace.app.rest.model.hateoas;

import org.dspace.app.rest.model.UnitRest;
import org.dspace.app.rest.model.hateoas.annotations.RelNameDSpaceResource;
import org.dspace.app.rest.utils.Utils;

/**
 * Unit Rest HAL Resource. The HAL Resource wraps the REST Resource
 * adding support for the links and embedded resources
 */
@RelNameDSpaceResource(UnitRest.NAME)
public class UnitResource extends DSpaceResource<UnitRest> {
    public UnitResource(UnitRest unit, Utils utils) {
        super(unit, utils);
    }
}
