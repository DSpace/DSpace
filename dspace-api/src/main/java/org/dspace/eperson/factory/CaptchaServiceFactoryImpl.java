/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.eperson.factory;

import org.dspace.eperson.service.CaptchaService;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * Factory to get services for Captcha protection of DSpace forms / endpoints
 *
 * @author Kim Shepherd
 */
public class CaptchaServiceFactoryImpl extends CaptchaServiceFactory {

    @Autowired
    @Qualifier("googleCaptchaService")
    private CaptchaService googleCaptchaService;

    @Autowired
    @Qualifier("altchaCaptchaService")
    private CaptchaService altchaCaptchaService;

    @Autowired
    private ConfigurationService configurationService;

    /**
     * Get the configured CaptchService
     * TODO: This will be fully "operational" once we have full coverage of all
     *       forms by all supported captcha providers. Until then, REST repositories
     *       should request the specific captcha service required.
     * @return the configured CaptchaService
     */
    @Override
    public CaptchaService getCaptchaService() {
        String provider = configurationService.getProperty("captcha.provider", "google");

        if ("altcha".equalsIgnoreCase(provider)) {
            return altchaCaptchaService;
        }

        return googleCaptchaService; // default to Google ReCaptcha
    }

    /**
     * Get the configured Altcha CaptchaService. This is needed by REST repositories
     * processing captcha payloads for forms that are *only* protected by Altcha.
     * TODO: We are working towards full coverage of all forms by all providers
     * @return the configured Altcha CaptchaService
     */
    @Override
    public CaptchaService getAltchaCaptchaService() {
        return altchaCaptchaService;
    }

    /**
     * Get the configured Google CaptchaService. This is needed by REST repositories
     * processing captcha payloads for forms that are *only* protected by Google.
     * TODO: We are working towards full coverage of all forms by all providers
     * @return the configured Google CaptchaService
     */
    @Override
    public CaptchaService getGoogleCaptchaService() {
        return googleCaptchaService;
    }
}
