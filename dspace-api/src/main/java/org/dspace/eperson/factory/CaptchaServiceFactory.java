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

/**
 * Factory to get services for Captcha protection of DSpace forms / endpoints
 *
 * @author Kim Shepherd
 */
public abstract class CaptchaServiceFactory {

    /**
     * Get the singleton instance of this class
     * @return singleton instance of this class
     */
    public static CaptchaServiceFactory getInstance() {
        return DSpaceServicesFactory.getInstance().getServiceManager()
                .getServiceByName("captchaServiceFactory", CaptchaServiceFactory.class);
    }

    /**
     * Get the configured CaptchService
     * TODO: This will be fully "operational" once we have full coverage of all
     *       forms by all supported captcha providers. Until then, REST repositories
     *       should request the specific captcha service required.
     * @return the configured CaptchaService
     */
    public abstract CaptchaService getCaptchaService();


    /**
     * Get the configured Altcha CaptchaService. This is needed by REST repositories
     * processing captcha payloads for forms that are *only* protected by Altcha.
     * TODO: We are working towards full coverage of all forms by all providers
     * @return the configured Altcha CaptchaService
     */
    public abstract CaptchaService getAltchaCaptchaService();

    /**
     * Get the configured Google CaptchaService. This is needed by REST repositories
     * processing captcha payloads for forms that are *only* protected by Google.
     * TODO: We are working towards full coverage of all forms by all providers
     * @return the configured Google CaptchaService
     */
    public abstract CaptchaService getGoogleCaptchaService();
}
