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

public class CaptchaServiceFactoryImpl extends CaptchaServiceFactory {

    @Autowired
    @Qualifier("googleCaptchaService")
    private CaptchaService googleCaptchaService;

    @Autowired
    @Qualifier("altchaCaptchaService")
    private CaptchaService altchaCaptchaService;

    @Autowired
    private ConfigurationService configurationService;

    @Override
    public CaptchaService getCaptchaService() {
        String provider = configurationService.getProperty("captcha.provider");

        if ("altcha".equalsIgnoreCase(provider)) {
            return altchaCaptchaService;
        }

        return googleCaptchaService; // default to Google ReCaptcha
    }
}
