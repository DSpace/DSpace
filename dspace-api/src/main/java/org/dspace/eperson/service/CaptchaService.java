/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.eperson.service;

import org.dspace.eperson.InvalidReCaptchaException;

/**
 * This service for validate the reCaptcha token
 *
 * @author Mohamed Eskander (mohamed.eskander at 4science dot it)
 */
public interface CaptchaService {

    public String REGISTER_ACTION = "register_email";

    /**
     * validate the entered reCaptcha token
     *
     * @param response reCaptcha token to be validated
     * @param action action of reCaptcha
     * @throws InvalidReCaptchaException if reCaptcha was not successfully validated
     */
    public void processResponse(String response, String action) throws InvalidReCaptchaException;

}