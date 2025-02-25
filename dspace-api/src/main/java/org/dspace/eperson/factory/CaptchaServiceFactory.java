package org.dspace.eperson.factory;

import org.dspace.eperson.service.CaptchaService;
import org.dspace.services.factory.DSpaceServicesFactory;

public abstract class CaptchaServiceFactory {

    public abstract CaptchaService getCaptchaService();

    public static CaptchaServiceFactory getInstance() {
        return DSpaceServicesFactory.getInstance().getServiceManager()
                                    .getServiceByName("captchaServiceFactory", CaptchaServiceFactory.class);
    }
}
