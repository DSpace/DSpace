package org.dspace.authenticate;

import static org.dspace.core.LogHelper.getHeader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.naming.NamingException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import edu.umd.lib.dspace.authenticate.LdapService;
import edu.umd.lib.dspace.authenticate.impl.Ldap;
import edu.umd.lib.dspace.authenticate.impl.LdapServiceImpl;
import edu.yale.its.tp.cas.client.ProxyTicketValidator;
import edu.yale.its.tp.cas.client.ServiceTicketValidator;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.authenticate.factory.AuthenticateServiceFactory;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;
import org.dspace.eperson.service.GroupService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;

// we use the Java CAS client

/**
 * Authenticator for Central Authentication Service (CAS).
 *
 * @author Naveed Hashmi, University of Bristol based on code developed by
 *         Nordija A/S (www.nordija.com) for Center of Knowledge Technology
 *         (www.cvt.dk)
 * @version $Revision: 1.0 $
 */

public class CASAuthentication implements AuthenticationMethod {
    /** log4j category */
    private static final Logger log = LogManager.getLogger(CASAuthentication.class);

    protected GroupService groupService = EPersonServiceFactory.getInstance().getGroupService();

    protected EPersonService ePersonService = EPersonServiceFactory.getInstance().getEPersonService();

    public final static String CAS_AUTHENTICATED = "cas.authenticated";
    public final static String CAS_LDAP = "cas.ldap";

    private final static ConfigurationService configurationService =
        DSpaceServicesFactory.getInstance().getConfigurationService();

    /**
     * Predicate, can new user automatically create EPerson. Checks
     * configuration value. You'll probably want this to be true to take
     * advantage of a Web certificate infrastructure with many more users than
     * are already known by DSpace.
     */
    @Override
    public boolean canSelfRegister(Context context, HttpServletRequest request, String username) throws SQLException {
        return configurationService.getBooleanProperty("drum.webui.cas.autoregister");
    }

    /**
     * Nothing extra to initialize.
     */
    @Override
    public void initEPerson(Context context, HttpServletRequest request, EPerson eperson) throws SQLException {
    }

    /**
     * We don't use EPerson password so there is no reason to change it.
     */
    @Override
    public boolean allowSetPassword(Context context, HttpServletRequest request, String username) throws SQLException {
        return false;
    }

    /**
     * Returns true, CAS is an implicit method
     */
    @Override
    public boolean isImplicit() {
        return true;
    }

    /**
     * Returns an Ldap object from the given HttpServletRequest and Context,
     * or null if no Ldap object is found.
     *
     * This method returns an Ldap object when either:
     *
     * a) the HttpServlet request contains a CAS_LDAP attribute in the
     *     request's session (which occurs once as part of the CAS login
     *     process)
     *
     * or
     *
     * b) the "isContextSwitched()" method on the DSpace Contet object returns
     *    true, indicated that a user is being impersonated. Note that this
     *    method will be called for each request.
     *
     * @param context the DSpace context object
     * @param request the HttpServletRequest being processed
     * @return an Ldap object derived from the request, or null.
     */
    protected Ldap getLdap(Context context, HttpServletRequest request) {
        if (request.getSession().getAttribute(CAS_LDAP) != null) {
            return (Ldap) request.getSession().getAttribute(CAS_LDAP);
        }

        if (context.isContextUserSwitched() && (context.getCurrentUser() != null)) {
            String impersonatedNetId = context.getCurrentUser().getNetid();

            if (impersonatedNetId != null) {
                return queryLdap(context, impersonatedNetId);
            }
        }

        return null;
    }

    /**
     * Groups mapped from Ldap Units
     *
     * @param context the DSpace context
     * @param request the HTTP reqquest
     * @return a List of Groups representing the "special groups" the user has
     * access to.
     */
    @Override
    public List<Group> getSpecialGroups(Context context, HttpServletRequest request) {
        try {
            Ldap ldap = getLdap(context, request);

            if (ldap != null) {
                List<Group> groups = ldap.getGroups(context);

                Group CASGroup = groupService.findByName(context, "CAS Authenticated");
                if (CASGroup == null) {
                    throw new Exception(
                            "Unable to find 'CAS Authenticated' group");
                }

                groups.add(CASGroup);

                return groups;
            }
        } catch (Exception e) {
            log.error("Ldap exception", e);
        }

        return new ArrayList<Group>();
    }

    /**
     * Returns the "service" URL to send as part of the CAS ticket validation.
     * This URL must exactly match the "service" URL sent to CAS when
     * initiating authentication, including any query parametes.
     *
     * @param context the current Context
     * @param request the HttpServletRequest
     * @return a String representing the service URL to be sent as part of the
     *         CAS ticket validation.
     */
    protected String getServiceUrlFromRequest(Context context, HttpServletRequest request) {
        String serviceUrl = request.getRequestURL().toString();

        // Append "redirectUrl" query parameter (if present) onto "service"
        if (request.getParameter("redirectUrl") != null) {
            serviceUrl = serviceUrl + "?redirectUrl=" + request.getParameter("redirectUrl");
        }

        return serviceUrl;
    }

    /**
     * Returns the username of the authenticated user from the CAS ticket,
     * or null if the ticket is invalid, or authentication was not
     * successful.
     *
     * @param context the current Context
     * @param ticket the CAS ticket
     * @param serviceUrl the service URL to use in validating the ticket
     * @return the username of the authenticated user, or null.
     */
    protected String getNetIdFromCasTicket(Context context, String ticket, String serviceUrl)
            throws IOException, ServletException {
        // Determine CAS validation URL
        final String validateURL = configurationService.getProperty("drum.cas.validate.url");
        log.info(getHeader(context, "login", "CAS validate:  " + validateURL));
        if (validateURL == null) {
            log.error("No CAS validation URL specified. You need to set property 'drum.cas.validate.url'");
            return null;
        }

        // Validate ticket (it is assumed that CAS validator returns the
        // user network ID)
        final String netid = validate(serviceUrl, ticket, validateURL);
        return netid;
    }

    /**
     * This method is provided for testing, so that tests can override the
     * LdapService implementation.
     *
     * @return the LdapService for use in querying LDAP.
     */
    protected LdapService createLdapService(Context context) throws NamingException {
        return new LdapServiceImpl(context);
    }

    /**
     * Queries the LDAP service for the given user id, returning null if the
     * user id is not found.
     *
     * @param strUid the uid of the user to look up in LDAP
     * @return an Ldap object encapsulating the LDAP search results, or null
     * if the user id is not found.
     */
    protected Ldap queryLdap(Context context, String strUid) {
        try {
            LdapService ldapService = createLdapService(context);

            try (ldapService) {
                return ldapService.queryLdap(strUid);
            }
        } catch (Exception e) {
            log.error("Exception querying LDAP service", e);
        }

        return null;
    }

    /**
     * CAS authentication.
     *
     * @return One of: SUCCESS, BAD_CREDENTIALS, NO_SUCH_USER, BAD_ARGS
     */
    @Override
    public int authenticate(Context context, String username, String password, String realm, HttpServletRequest request)
            throws SQLException {
        final String ticket = request.getParameter("ticket");
        log.info(getHeader(context, "login", " ticket: " + ticket));

        if (ticket == null) {
            // No CAS ticket
            log.error("'ticket' from request is null.");
            return BAD_ARGS;
        }

        final String serviceUrl = getServiceUrlFromRequest(context, request);
        log.info(getHeader(context, "login", "service: " + serviceUrl));

        // CAS ticket
        try {
            String netid = getNetIdFromCasTicket(context, ticket, serviceUrl);
            if (netid == null) {
                log.error("Ticket '" + ticket + "' is not valid. netid is null.");
                return BAD_ARGS;
            }

            // Check directory
            Ldap ldap = queryLdap(context, netid);
            if (ldap == null) {
                log.error("Unknown directory id " + netid);
                return NO_SUCH_USER;
            }

            // Save the ldap object in the session
            request.getSession().setAttribute(CAS_LDAP, ldap);

            log.info("netid = " + netid);

            // Locate the eperson in DSpace
            EPerson eperson = null;
            try {
                eperson = ePersonService.findByNetid(context, netid.toLowerCase());
            } catch (SQLException ignored) {
                log.warn("ignored SQL exception");
            }

            // Register New User, if necessary
            if (eperson == null) {
                if (canSelfRegister(context, request, netid)) {
                    eperson = registerEPerson(netid, context, ldap, request);

                    context.setCurrentUser(eperson);
                } else {
                    // No auto-registration for valid netid
                    log.warn(getHeader(context, "authenticate", "type=netid_but_no_record, cannot auto-register"));
                    return NO_SUCH_USER;
                }
            }

            if (eperson == null) {
                return AuthenticationMethod.NO_SUCH_USER;
            }

            if (eperson.getRequireCertificate()) {
                // they must use a certificate
                return CERT_REQUIRED;
            }

            if (!eperson.canLogIn()) {
                return BAD_ARGS;
            }

            // Logged in OK.
            request.setAttribute(CAS_AUTHENTICATED, true);

            context.setCurrentUser(eperson);
            log.info(getHeader(context, "authenticate", "type=CAS"));
            return SUCCESS;
        } catch (Exception e) {
            log.error("Unexpected exception caught", e);
        }
        return BAD_ARGS;
    }

    /**
     * Returns the NetID of the owner of the given ticket, or null if the ticket
     * isn't valid.
     *
     * @param service the service ID for the application validating the ticket
     * @param ticket  the opaque service ticket (ST) to validate
     */
    public static String validate(String service, String ticket, String validateURL)
            throws IOException, ServletException {
        ServiceTicketValidator stv;

        if (ticket.startsWith("ST")) {
            stv = new ServiceTicketValidator();
        } else {
            // uPortal uses this
            stv = new ProxyTicketValidator();
        }

        stv.setCasValidateUrl(validateURL);
        stv.setService(java.net.URLEncoder.encode(service, StandardCharsets.UTF_8));
        stv.setServiceTicket(ticket);

        try {
            stv.validate();
        } catch (Exception e) {
            log.error("Unexpected exception caught", e);
            throw new ServletException(e);
        }

        if (!stv.isAuthenticationSuccesful()) {
            return null;
        }
        return stv.getUser();
    }

    /*
     * Returns URL to which to redirect to obtain credentials (either password
     * prompt or e.g. HTTPS port for client cert.); null means no redirect.
     *
     * @param context DSpace context, will be modified (ePerson set) upon
     * success.
     *
     * @param request The HTTP request that started this operation, or null if
     * not applicable.
     *
     * @param response The HTTP response from the servlet method.
     *
     * @return fully-qualified URL
     */
    @Override
    public String loginPageURL(Context context, HttpServletRequest request, HttpServletResponse response) {
        // Determine the client redirect URL, where to redirect after authenticating.
        String redirectUrl = null;
        if (request.getHeader("Referer") != null && StringUtils.isNotBlank(request.getHeader("Referer"))) {
            redirectUrl = request.getHeader("Referer");
        } else if (request.getHeader("X-Requested-With") != null
                && StringUtils.isNotBlank(request.getHeader("X-Requested-With"))) {
            redirectUrl = request.getHeader("X-Requested-With");
        }

        // Determine the "service" URL, i.e., the DSpace authentication endpoint
        // where CAS will send the user after successfully authenticating.
        // The serviceUrl triggers the CASLoginFilter in order to extract
        // the user's information, locally authenticate them & then
        // redirect back to the UI.
        //
        // The path for the URL is configured in org.dspace.app.rest.security.WebSecurityConfiguration
        String serviceUrl = configurationService.getProperty("dspace.server.url") + "/api/authn/cas"
                + ((redirectUrl != null) ? "?redirectUrl=" + redirectUrl : "");

        if (serviceUrl.startsWith("https")) {
            // Ensure that the serviceUrl uses "http", instead of "https".
            // This is needed for Kubernetes, where HTTPS is handled
            // by Nginx, and the serviceUrl called after successful CAS
            // authentication is made to the DSpace endpoint as HTTP.
            // When validating the CAS ticket, the serviceUrl must exactly
            // match the serviceUrl returned by the "getServiceUrlFromRequest"
            // method (which calculates the serviceUrl from the parameters
            // of the incoming request), including protocol.
            serviceUrl = serviceUrl.replaceFirst("https", "http");
        }

        // Determine CAS server URL
        final String authServer = configurationService.getProperty("drum.cas.server.url");

        log.debug("CAS server:  " + authServer);
        log.debug("Service URL: " + serviceUrl);
        log.debug("redirectUrl: " + redirectUrl);

        // Redirect to CAS server
        String result = response.encodeRedirectURL(authServer + "?service=" + serviceUrl);
        return result;
    }

    @Override
    public String getName() {
        return "cas";
    }

    @Override
    public boolean isUsed(Context context, HttpServletRequest request) {
        if (request != null && context.getCurrentUser() != null && request.getAttribute(CAS_AUTHENTICATED) != null) {
            return true;
        }
        return false;
    }

    @Override
    public boolean canChangePassword(Context context, EPerson ePerson, String currentPassword) {
        return false;
    }

    /**
     * Registers an EPerson, using the information in the given Ldap object.
     */
    protected EPerson registerEPerson(String uid, Context context, Ldap ldap, HttpServletRequest request)
        throws Exception {
        // Turn off authorizations to create a new user
        context.turnOffAuthorisationSystem();

        try {
            // Create a new eperson
            EPerson eperson = ePersonService.create(context);

            String strFirstName = ldap.getFirstName();
            if (strFirstName == null) {
                strFirstName = "??";
            }

            String strLastName = ldap.getLastName();
            if (strLastName == null) {
                strLastName = "??";
            }

            String strPhone = ldap.getPhone();
            if (strPhone == null) {
                strPhone = "??";
            }

            eperson.setNetid(uid);
            eperson.setEmail(uid + "@umd.edu");
            eperson.setFirstName(context, strFirstName);
            eperson.setLastName(context, strLastName);
            ePersonService.setMetadataSingleValue(context, eperson, EPersonService.MD_PHONE, null, strPhone);
            eperson.setCanLogIn(true);
            eperson.setRequireCertificate(false);

            AuthenticateServiceFactory.getInstance().getAuthenticationService().initEPerson(context, request, eperson);
            ePersonService.update(context, eperson);
            context.dispatchEvents();

            log.info("CASAuthentication::registerEPerson: create_um_eperson eperson_id="
                     + eperson.getID() + ", uid=" + uid);

            return eperson;
        } finally {
            // Turn authorizations back on.
            context.restoreAuthSystemState();
        }
    }
}
