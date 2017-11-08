package org.dspace.app.rest.model.hateoas;

import org.dspace.app.rest.model.StatusRest;
import org.dspace.app.rest.utils.Utils;

@RelNameDSpaceResource(StatusRest.NAME)
public class StatusResource extends DSpaceResource<StatusRest> {
    public StatusResource(StatusRest data, Utils utils, String... rels) {
        super(data, utils, rels);
    }
}
