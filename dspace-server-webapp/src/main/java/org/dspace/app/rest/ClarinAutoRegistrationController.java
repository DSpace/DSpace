/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Objects;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.authenticate.clarin.ClarinShibAuthentication;
import org.dspace.content.clarin.ClarinVerificationToken;
import org.dspace.content.service.clarin.ClarinVerificationTokenService;
import org.dspace.core.Context;
import org.dspace.core.Email;
import org.dspace.core.I18nUtil;
import org.dspace.core.Utils;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.service.EPersonService;
import org.dspace.services.ConfigurationService;
import org.dspace.web.ContextUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * If the Shibboleth authentication failed because the IdP hasn't sent the `SHIB-EMAIL` header.
 * Controller for sending the email with verification token to the user and confirming the token by it.
 *
 * This Shibboleth Authentication process is tested in ClarinShibbolethLoginFilterIT.
 *
 * @author Milan Majchrak (milan.majchrak at dataquest.sk)
 */
@RequestMapping(value = "/api/autoregistration")
@RestController
public class ClarinAutoRegistrationController {

    private static Logger log = Logger.getLogger(ClarinAutoRegistrationController.class);

    @Autowired
    ConfigurationService configurationService;
    @Autowired
    ClarinVerificationTokenService clarinVerificationTokenService;
    @Autowired
    EPersonService ePersonService;

    /**
     * Send the email with the verification token to the user.
     * @param netid of the user, this information is retrieved from the IdP.
     * @param email which the user has filled in.
     * @return Successful response or an error message.
     */
    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity sendEmail(HttpServletRequest request, HttpServletResponse response,
                                    @RequestParam("netid") String netid,
                                    @RequestParam("email") String email) throws IOException, SQLException {

        Context context = ContextUtil.obtainCurrentRequestContext();
        if (Objects.isNull(context)) {
            log.error("Cannot obtain the context from the request");
            throw new RuntimeException("Cannot obtain the context from the request");
        }

        // The verification token object is created when the Shibboleth authentication is failed in the
        // ClarinShibbolethLoginFilter. It is created with the user `netid` and `shib_headers` which are passed
        // from the IdP.
        ClarinVerificationToken clarinVerificationToken = clarinVerificationTokenService.findByNetID(context, netid);
        if (Objects.isNull(clarinVerificationToken)) {
            // The verification token doesn't exist.
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Cannot load the clarin verification " +
                    "token class by net id: " + netid);
            return null;
        }

        String uiUrl = configurationService.getProperty("dspace.ui.url");
        if (StringUtils.isEmpty(uiUrl)) {
            log.error("Cannot load the `dspace.ui.url` property from the cfg.");
            throw new RuntimeException("Cannot load the `dspace.ui.url` property from the cfg.");
        }

        // Generate token and create ClarinVerificationToken record with the token and user email.
        String verificationToken = Utils.generateHexKey();
        clarinVerificationToken.setePersonNetID(netid);
        clarinVerificationToken.setEmail(email);
        clarinVerificationToken.setToken(verificationToken);
        clarinVerificationTokenService.update(context, clarinVerificationToken);
        context.commit();

        // Compose the url with the verification token. The user will be redirected to the UI.
        String autoregistrationURL = uiUrl + "/login/autoregistration?verification-token=" + verificationToken;
        try {
            Locale locale = context.getCurrentLocale();
            Email bean = Email.getEmail(I18nUtil.getEmailFilename(locale, "clarin_autoregistration"));
            bean.addArgument(autoregistrationURL);
            bean.addRecipient(email);
            bean.send();
        } catch (Exception e) {
            log.error("Cannot send the email because: " + e.getMessage());
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Cannot send the email");
            return null;
        }

        return ResponseEntity.ok().build();
    }

    /**
     * Confirm the verification token which the user received from the email. If the verification token is
     * valid the user is registered and signed in or just signed in (if it exists). After successful
     * signing in the verification token object is removed from the database.
     * @param token to verify.
     * @return Successful response or an error message.
     */
    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity confirmEmail(HttpServletRequest request, HttpServletResponse response,
                                       @RequestParam("verification-token") String token) throws IOException,
            SQLException {
        Context context = ContextUtil.obtainCurrentRequestContext();
        if (Objects.isNull(context)) {
            log.error("Cannot obtain the context from the request");
            throw new RuntimeException("Cannot obtain the context from the request");
        }

        // Check if the token is valid (if it exists in the database)
        ClarinVerificationToken clarinVerificationToken = clarinVerificationTokenService.findByToken(context, token);
        if (Objects.isNull(clarinVerificationToken)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Verification token doesn't exists.");
            return null;
        }

        // Set request attribute `shib.headers` for authentication method.
        // These headers are retrieved in the ClarinShibAuthentication.authenticate method.
        request.setAttribute("shib.headers", clarinVerificationToken.getShibHeaders());
        try {
            new ClarinShibAuthentication().authenticate(context, "", "", "", request);
        } catch (SQLException e) {
            log.error("Cannot authenticate the user by an autoregistration URL because: " + e.getSQLState());
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Cannot authenticate the user by an autoregistration URL.");
            return null;
        }
        context.commit();

        // If the Authentication was successful the Eperson should be found.
        EPerson ePerson = ePersonService.findByNetid(context, clarinVerificationToken.getePersonNetID());
        if (Objects.isNull(ePerson)) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "The user wasn't successfully registered!");
            return null;
        }

        // Register the new user - call Shibboleth authenticate method
        // Send response
        return ResponseEntity.ok().build();
    }
}
