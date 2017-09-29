package org.dspace.app.rest.model.hateoas;

import org.dspace.app.rest.model.DiscoveryRest;
import org.dspace.app.rest.utils.Utils;

/**
 * Created by Luiz Claudio Santos on 4/6/17.
 */
@RelNameDSpaceResource(DiscoveryRest.NAME)
public class DiscoveryResource  extends DSpaceResource<DiscoveryRest>{

    public DiscoveryResource(DiscoveryRest data, Utils utils, String... rels) {
        super(data, utils, rels);
    }
}
