package org.dspace.app.rest.model.hateoas;

import org.dspace.app.rest.model.ClarinLicenseRest;
import org.dspace.app.rest.model.hateoas.annotations.RelNameDSpaceResource;
import org.dspace.app.rest.utils.Utils;

@RelNameDSpaceResource(ClarinLicenseRest.NAME)
public class ClarinLicenseResource extends DSpaceResource<ClarinLicenseRest> {
    public ClarinLicenseResource(ClarinLicenseRest ms, Utils utils) {
        super(ms, utils);
    }
}

