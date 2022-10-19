package org.dspace.app.rest.model.hateoas;

import org.dspace.app.rest.model.IdentifiersRest;
import org.dspace.app.rest.model.hateoas.annotations.RelNameDSpaceResource;
import org.dspace.app.rest.utils.Utils;

@RelNameDSpaceResource(IdentifiersRest.NAME)
public class IdentifiersResource extends HALResource<IdentifiersRest> {
    public IdentifiersResource(IdentifiersRest data, Utils utils) {
        super(data);
    }
}
