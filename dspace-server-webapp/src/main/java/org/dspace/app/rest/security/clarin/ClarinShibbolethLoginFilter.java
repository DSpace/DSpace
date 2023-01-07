/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.security.clarin;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Objects;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.security.DSpaceAuthentication;
import org.dspace.app.rest.security.RestAuthenticationService;
import org.dspace.app.rest.security.StatelessLoginFilter;
import org.dspace.authenticate.clarin.ClarinShibAuthentication;
import org.dspace.authenticate.clarin.ShibHeaders;
import org.dspace.content.clarin.ClarinVerificationToken;
import org.dspace.content.factory.ClarinServiceFactory;
import org.dspace.content.service.clarin.ClarinVerificationTokenService;
import org.dspace.core.Context;
import org.dspace.core.Utils;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.web.ContextUtil;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

/**
 * This class is copied from `ShibbolethLoginFilter` and modified by the
 * @author Milan Majchrak (milan.majchrak at dataquest.sk).
 *
 * This class will filter Shibboleth requests to see if the user has been authenticated via Shibboleth.
 * <P>
 * The overall Shibboleth login process is as follows:
 *   1. When Shibboleth plugin is enabled, client/UI receives Shibboleth's absolute URL in WWW-Authenticate header.
 *      See {@link ClarinShibAuthentication} loginPageURL() method.
 *   2. Client sends the user to that URL when they select Shibboleth authentication.
 *   3. User logs in using Shibboleth
 *   4. If successful, they are redirected by Shibboleth to the path where this Filter is "listening" (that path
 *      is passed to Shibboleth as a URL param in step 1)
 *   4.1. This filter then intercepts the request in order to check for a valid Shibboleth login (see
 *      ShibAuthentication.authenticate()) and stores that user info in a JWT. It also saves that JWT in a *temporary*
 *      authentication cookie.
 *   4.2. At that point, the client reads the JWT from the Cookie, and sends it back in a request to /api/authn/login,
 *      which triggers the server-side to destroy the Cookie and move the JWT into a Header
 *   5. If not successful:
 *   5.1. The IdP hasn't sent the `Shib-Identity-Provider` or `SHIB-NETID` header. The user is redirected to the
 *      static error page.
 *   5.2. The IdP hasn't sent the `SHIB-EMAIL` header.
 *      The request headers passed by IdP are stored into the `verification_token` table the `shib_headers` column.
 *      The user is redirected to the page when he must fill his email.
 * <P>
 * This Shibboleth Authentication process is tested in ClarinShibbolethLoginFilterIT.
 *
 * @author Giuseppe Digilio (giuseppe dot digilio at 4science dot it)
 * @author Tim Donohue
 * @see ClarinShibAuthentication
 */
public class ClarinShibbolethLoginFilter extends StatelessLoginFilter {
    public static final String USER_WITHOUT_EMAIL_EXCEPTION = "UserWithoutEmailException";
    public static final String MISSING_HEADERS_FROM_IDP = "MissingHeadersFromIpd";
    private static final String AUTHORIZATION_HEADER = "Authorization";

    private static final Logger log = LogManager.getLogger(org.dspace.app.rest.security.ShibbolethLoginFilter.class);

    private ConfigurationService configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
    private ClarinVerificationTokenService clarinVerificationTokenService = ClarinServiceFactory.getInstance()
            .getClarinVerificationTokenService();
    private EPersonService ePersonService = EPersonServiceFactory.getInstance().getEPersonService();

    public ClarinShibbolethLoginFilter(String url, AuthenticationManager authenticationManager,
                                 RestAuthenticationService restAuthenticationService) {
        super(url, authenticationManager, restAuthenticationService);
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest req,
                                                HttpServletResponse res) throws AuthenticationException {
        // First, if Shibboleth is not enabled, throw an immediate ProviderNotFoundException
        // This tells Spring Security that authentication failed
        if (!ClarinShibAuthentication.isEnabled()) {
            throw new ProviderNotFoundException("Shibboleth is disabled.");
        }

        // If the Idp doesn't send the email in the request header, send the redirect order to the FE for the user
        // to fill in the email.
        String netidHeader = configurationService.getProperty("authentication-shibboleth.netid-header");
        String emailHeader = configurationService.getProperty("authentication-shibboleth.email-header");

        Context context = ContextUtil.obtainContext(req);
        if (Objects.isNull(context)) {
            throw new RuntimeException("Cannot load the context");
        }

        // If the verification token is not null the user wants to login.
        String verificationToken = req.getHeader("verification-token");
        ClarinVerificationToken clarinVerificationToken;
        try {
            clarinVerificationToken = clarinVerificationTokenService.findByToken(context, verificationToken);
        } catch (SQLException e) {
            throw new RuntimeException("Cannot find clarin verification token by token: " + verificationToken + "" +
                    " because: " + e.getSQLState());
        }

        // Load ShibHeader from request or from clarin verification token object.
        ShibHeaders shib_headers;
        if (Objects.nonNull(clarinVerificationToken)) {
            // Set request attribute for authentication method.
            req.setAttribute("shib.headers", clarinVerificationToken.getShibHeaders());
            shib_headers = new ShibHeaders(clarinVerificationToken.getShibHeaders());
        } else {
            shib_headers = new ShibHeaders(req);
        }

        // Retrieve the netid and email values from the header.
        String netid = shib_headers.get_single(netidHeader);
        String idp = shib_headers.get_idp();
        // If the clarin verification object is not null load the email from there otherwise from header.
        String email = Objects.isNull(clarinVerificationToken) ?
                shib_headers.get_single(emailHeader) : clarinVerificationToken.getEmail();

        // If email is null and netid exist try to find the eperson by netid and load its email
        if (StringUtils.isEmpty(email) && StringUtils.isNotEmpty(netid)) {
            try {
                EPerson ePerson = ePersonService.findByNetid(context, netid);
                email = Objects.isNull(email) ? this.getEpersonEmail(ePerson) : null;
            } catch (SQLException ignored) {
                //
            }
        }

        try {
            if (StringUtils.isEmpty(netid) || StringUtils.isEmpty(idp)) {
                log.error("Cannot load the netid or idp from the request headers.");
                this.redirectToMissingHeadersPage(res);
                return null;
            }

            // The Idp hasn't sent the email - the user will be redirected to the page where he must fill in that
            // missing email
            if (StringUtils.isBlank(email)) {
                log.error("Cannot load the shib email header from the request headers.");
                this.redirectToWriteEmailPage(req, res);
                return null;
            }
        } catch (IOException e) {
            throw new RuntimeException("Cannot redirect the user to the Shibboleth authentication error page" +
                    " because: " + e.getMessage());
        }

        // In the case of Shibboleth, this method does NOT actually authenticate us. The authentication
        // has already happened in Shibboleth. So, this call to "authenticate()" is just triggering
        // ShibAuthentication.authenticate() to check for a valid Shibboleth login, and if found, the current user
        // is considered authenticated via Shibboleth.
        // NOTE: because this authentication is implicit, we pass in an empty DSpaceAuthentication
        return authenticationManager.authenticate(new DSpaceAuthentication());
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest req,
                                            HttpServletResponse res,
                                            FilterChain chain,
                                            Authentication auth) throws IOException, ServletException {
        // Once we've gotten here, we know we have a successful login (i.e. attemptAuthentication() succeeded)

        DSpaceAuthentication dSpaceAuthentication = (DSpaceAuthentication) auth;
        log.debug("Shib authentication successful for EPerson {}. Sending back temporary auth cookie",
                dSpaceAuthentication.getName());
        // OVERRIDE DEFAULT behavior of StatelessLoginFilter to return a temporary authentication cookie containing
        // the Auth Token (JWT). This Cookie is required because we *redirect* the user back to the client/UI after
        // a successful Shibboleth login. Headers cannot be sent via a redirect, so a Cookie must be sent to provide
        // the auth token to the client. On the next request from the client, the cookie is read and destroyed & the
        // Auth token is only used in the Header from that point forward.
        restAuthenticationService.addAuthenticationDataForUser(req, res, dSpaceAuthentication, true);

        String verificationToken = req.getHeader("verification-token");
        if (StringUtils.isEmpty(verificationToken)) {
            // redirect user after completing Shibboleth authentication, sending along the temporary auth cookie
            redirectAfterSuccess(req, res);
        } else {
            res.getWriter().write(res.getHeader(AUTHORIZATION_HEADER));
        }
    }

    /**
     * If the above attemptAuthentication() call was unsuccessful, then ensure that the response is a 401 Unauthorized
     * AND it includes a WWW-Authentication header. We use this header in DSpace to return all the enabled
     * authentication options available to the UI (along with the path to the login URL for each option)
     * @param request current request
     * @param response current response
     * @param failed exception that was thrown by attemptAuthentication()
     * @throws IOException
     * @throws ServletException
     */
    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request,
                                              HttpServletResponse response, AuthenticationException failed)
            throws IOException, ServletException {

        String authenticateHeaderValue = restAuthenticationService.getWwwAuthenticateHeaderValue(request, response);

        response.setHeader("WWW-Authenticate", authenticateHeaderValue);
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication failed!");
        log.error("Authentication failed (status:{})",
                HttpServletResponse.SC_UNAUTHORIZED, failed);
    }


    /**
     * After successful login, redirect to the DSpace URL specified by this Shibboleth request (in the "redirectUrl"
     * request parameter). If that 'redirectUrl' is not valid or trusted for this DSpace site, then return a 400 error.
     * @param request
     * @param response
     * @throws IOException
     */
    private void redirectAfterSuccess(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // Get redirect URL from request parameter
        String redirectUrl = request.getParameter("redirectUrl");

        // If redirectUrl unspecified, default to the configured UI
        if (StringUtils.isEmpty(redirectUrl)) {
            redirectUrl = configurationService.getProperty("dspace.ui.url");
        }

        // Validate that the redirectURL matches either the server or UI hostname. It *cannot* be an arbitrary URL.
        String redirectHostName = Utils.getHostName(redirectUrl);
        String serverHostName = Utils.getHostName(configurationService.getProperty("dspace.server.url"));
        ArrayList<String> allowedHostNames = new ArrayList<>();
        allowedHostNames.add(serverHostName);
        String[] allowedUrls = configurationService.getArrayProperty("rest.cors.allowed-origins");
        for (String url : allowedUrls) {
            allowedHostNames.add(Utils.getHostName(url));
        }

        if (StringUtils.equalsAnyIgnoreCase(redirectHostName, allowedHostNames.toArray(new String[0]))) {
            log.debug("Shibboleth redirecting to " + redirectUrl);
            response.sendRedirect(redirectUrl);
        } else {
            log.error("Invalid Shibboleth redirectURL=" + redirectUrl +
                    ". URL doesn't match hostname of server or UI!");
            response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                    "Invalid redirectURL! Must match server or ui hostname.");
        }
    }

    /**
     * The IdP hasn't sent the `Shib-Identity-Provider` or `SHIB-NETID` header. The user is redirected to the
     * static error page (The UI process error message).
     */
    protected void redirectToMissingHeadersPage(HttpServletResponse res) throws IOException {
        res.sendError(HttpServletResponse.SC_UNAUTHORIZED, MISSING_HEADERS_FROM_IDP);
    }

    /**
     * The IdP hasn't sent the `SHIB-EMAIL` header. The user is redirected to the page where he must fill in his
     * email. (The UI process error message).
     * The request headers passed by IdP are stored into the `verification_token` table the `shib_headers` column
     * for later usage. After successful signing in the `verification_token` record is removed from the DB.
     */
    protected void redirectToWriteEmailPage(HttpServletRequest req,
                                            HttpServletResponse res) throws IOException {
        Context context = ContextUtil.obtainContext(req);
        String authenticateHeaderValue = restAuthenticationService.getWwwAuthenticateHeaderValue(req, res);

        // Load header keys from cfg
        String netidHeader = configurationService.getProperty("authentication-shibboleth.netid-header");

        // Store the header which the Idp has sent to the ShibHeaders object and save that header into the table
        // `verification_token` because after successful authentication the Idp headers will be showed for the user in
        // the another page.
        // Store header values in the ShibHeaders because of String issues.
        ShibHeaders shib_headers = new ShibHeaders(req);
        String netid = shib_headers.get_single(netidHeader);

        // Store the Idp headers associated with the current netid.
        try {
            ClarinVerificationToken clarinVerificationToken =
                    clarinVerificationTokenService.findByNetID(context, netid);
            if (Objects.isNull(clarinVerificationToken)) {
                clarinVerificationToken = clarinVerificationTokenService.create(context);
                clarinVerificationToken.setePersonNetID(netid);
            }
            clarinVerificationToken.setShibHeaders(shib_headers.toString());
            clarinVerificationTokenService.update(context, clarinVerificationToken);
            context.commit();
        } catch (SQLException e) {
            throw new RuntimeException("Cannot create or update the Clarin Verification Token because: "
                    + e.getMessage());
        }

        // Add header values to the error message to retrieve them in the FE. That headers are needed for the
        // next processing.
        String separator = ",";
        String[] headers = new String[] {USER_WITHOUT_EMAIL_EXCEPTION, netid};
        String errorMessage = StringUtils.join(headers, separator);

        res.setHeader("WWW-Authenticate", authenticateHeaderValue);
        res.sendError(HttpServletResponse.SC_UNAUTHORIZED, errorMessage);
    }

    private String getEpersonEmail(EPerson ePerson) {
        if (Objects.isNull(ePerson)) {
            return null;
        }
        return ePerson.getEmail();
    }
}

