package org.dspace.app.rest.model.hateoas;

import org.dspace.app.rest.model.ClarinLicenseLabelRest;
import org.dspace.app.rest.model.hateoas.annotations.RelNameDSpaceResource;
import org.dspace.app.rest.utils.Utils;

@RelNameDSpaceResource(ClarinLicenseLabelRest.NAME)
public class ClarinLicenseLabelResource extends DSpaceResource<ClarinLicenseLabelRest> {
    public ClarinLicenseLabelResource(ClarinLicenseLabelRest ms, Utils utils) {
        super(ms, utils);
    }
}
