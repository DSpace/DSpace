/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
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
