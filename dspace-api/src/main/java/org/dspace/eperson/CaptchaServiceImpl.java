/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.eperson;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import javax.annotation.PostConstruct;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.eperson.service.CaptchaService;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;

/**
 * Basic services implementation for the Captcha.
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk@4science.com)
 */
public class CaptchaServiceImpl implements CaptchaService {

    private static final Logger log = LogManager.getLogger(CaptchaServiceImpl.class);

    private static Pattern RESPONSE_PATTERN = Pattern.compile("[A-Za-z0-9_-]+");

    private CaptchaSettings captchaSettings;

    @Autowired
    private ConfigurationService configurationService;

    @PostConstruct
    public void init() {
        captchaSettings = new CaptchaSettings();
        captchaSettings.setSite(configurationService.getProperty("google.recaptcha.key.site"));
        captchaSettings.setSecret(configurationService.getProperty("google.recaptcha.key.secret"));
        captchaSettings.setSiteVerify(configurationService.getProperty("google.recaptcha.site-verify"));
        captchaSettings.setCaptchaVersion(configurationService.getProperty("google.recaptcha.version", "v2"));
        captchaSettings.setThreshold(Float.parseFloat(
                                     configurationService.getProperty("google.recaptcha.key.threshold", "0.5")));
    }

    @Override
    public void processResponse(String response, String action) throws InvalidReCaptchaException {

        if (!responseSanityCheck(response)) {
            throw new InvalidReCaptchaException("Response contains invalid characters");
        }

        URI verifyUri = URI.create(captchaSettings.getSiteVerify());

        List<NameValuePair> params = new ArrayList<NameValuePair>(3);
        params.add(new BasicNameValuePair("secret", captchaSettings.getSecret()));
        params.add(new BasicNameValuePair("response", response));
        params.add(new BasicNameValuePair("remoteip", ""));

        HttpPost httpPost = new HttpPost(verifyUri);
        try {
            httpPost.addHeader("Accept", "application/json");
            httpPost.addHeader("Content-Type", "application/x-www-form-urlencoded");
            httpPost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e.getMessage(), e);
        }

        HttpClient httpClient = HttpClientBuilder.create().build();
        HttpResponse httpResponse;
        GoogleCaptchaResponse googleResponse;
        final ObjectMapper objectMapper = new ObjectMapper();
        try {
            httpResponse = httpClient.execute(httpPost);
            googleResponse = objectMapper.readValue(httpResponse.getEntity().getContent(), GoogleCaptchaResponse.class);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException("Error during verify google recaptcha site", e);
        }
        validateGoogleResponse(googleResponse, action);
    }

    private boolean responseSanityCheck(String response) {
        return StringUtils.hasLength(response) && RESPONSE_PATTERN.matcher(response).matches();
    }

    private void validateGoogleResponse(GoogleCaptchaResponse googleResponse, String action) {
        if (Objects.isNull(googleResponse)) {
            log.error("Google reCaptcha response was empty. ReCaptcha could not be validated.");
            throw new InvalidReCaptchaException("reCaptcha was not successfully validated");
        }

        if ("v2".equals(captchaSettings.getCaptchaVersion())) {
            if (!googleResponse.isSuccess()) {
                log.error("Google reCaptcha v2 returned an unsuccessful response. ReCaptcha was not validated.");
                throw new InvalidReCaptchaException("reCaptcha was not successfully validated");
            }
        } else {
            if (!googleResponse.isSuccess() || !googleResponse.getAction().equals(action)
                || googleResponse.getScore() < captchaSettings.getThreshold()) {
                log.error("Google reCaptcha v3 returned an unsuccessful response with"
                        + " action {" + googleResponse.getAction() + "} and score {" + googleResponse.getScore() + "}."
                        + " ReCaptcha was not validated.");
                throw new InvalidReCaptchaException("reCaptcha was not successfully validated");
            }
        }
    }

}