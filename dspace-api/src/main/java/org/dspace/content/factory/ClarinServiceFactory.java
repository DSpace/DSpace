package org.dspace.content.factory;

import org.dspace.content.service.BundleService;
import org.dspace.content.service.clarin.ClarinLicenseLabelService;
import org.dspace.content.service.clarin.ClarinLicenseService;
import org.dspace.services.factory.DSpaceServicesFactory;

public abstract class ClarinServiceFactory {

    public abstract ClarinLicenseService getClarinLicenseService();

    public abstract ClarinLicenseLabelService getClarinLicenseLabelService();

    public static ClarinServiceFactory getInstance() {
        return DSpaceServicesFactory.getInstance().getServiceManager()
                .getServiceByName("clarinServiceFactory", ClarinServiceFactory.class);
    }
}
